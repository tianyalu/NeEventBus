# 手写`EventBus`反射原理框架 
## 一、实现思路  
`EventBus` 作为`Activity/Fragment `的中间桥梁，管理了其中被`Subscribe`注解的方法，以`Object`(`Activity`或者`Fragment`)为键，放在`Map<Object, List<SubscribeMethod>> Map`中；当`post`方法调用时遍历`Map`,遍历`List`,找到所有以`bean`参数类型的方法，并通过反射执行这些方法。

## 二、核心代码

### 2.1 注册/订阅事件

注册时寻找`object`(本例子中对应的就是`MainActivity`)中所有带有`subscribe`注解的方法，然后将这些方法放到`EventBus`内部维护的以`Object`(`Activity`或者`Fragment`)为键，`List<SubscribeMethod>`为值的`Map`中进行管理。

```java
/**
 * 寻找object(本例子中对应的就是MainActivity)中所有带有subscribe注解的方法 放到map中进行管理
 * @param obj
 */
public void register(Object obj) {
  List<SubscribeMethod> list = cacheMap.get(obj);
  if(list == null) {
    list = findSubscribeMethods(obj);
    cacheMap.put(obj, list);
  }
}

/**
 * 不但找本类中有Subscribe注解的方法，还要找其父类的（系统类除外）
 * @param obj
 * @return
 */
private List<SubscribeMethod> findSubscribeMethods(Object obj) {
  List<SubscribeMethod> list = new ArrayList<>();
  Class<?> clazz = obj.getClass();
  while (clazz != null) {
    // 凡是系统级别的父类，直接忽略掉
    String name = clazz.getName();
    if("java.".equals(name) || "javax.".equals(name) || "android.".equals(name)) {
      break;
    }

    Method[] methods = clazz.getDeclaredMethods(); //仅寻找类声明了的方法（非类中全部方法）
    for (Method method : methods) {
      Subscribe subscribe = method.getAnnotation(Subscribe.class); //!=null
      //Override override = method.getAnnotation(Override.class); //==null RetentionPolicy.SOURCE 仅在编译时有效
      if(subscribe == null) {
        continue;
      }
      //判断方法中的参数是否唯一
      Class<?>[] types = method.getParameterTypes();
      if(types.length != 1) {
        Log.e("ERROR", "EventBus only accept on param");
      }
      ThreadMode threadMode = subscribe.threadMode();
      SubscribeMethod subscribeMethod = new SubscribeMethod(method, threadMode, types[0]);
      list.add(subscribeMethod);
    }
    clazz = clazz.getSuperclass();
  }

  return list;
}
```

### 2.2 发送事件

发送事件后，循环遍历`EventBus`维护的`Map`，找到所有和`post`方法中参数类型相同的`subscribeMethod`，根据注解参数中规定的线程，利用反射调用方法在指定的线程中执行。

```java
/**
 * 直接循环cacheMap中的方法，找到对应方法进行调用
 * @param type
 */
public void post(final Object type) {
  Set<Object> set = cacheMap.keySet();
  Iterator<Object> iterator = set.iterator();
  while (iterator.hasNext()) {
    final Object obj = iterator.next();
    List<SubscribeMethod> list = cacheMap.get(obj);
    for (final SubscribeMethod subscribeMethod : list) {
      if(subscribeMethod.getType().isAssignableFrom(type.getClass())) {
        switch (subscribeMethod.getThreadMode()) {
          case MAIN:
            //主 - 主
            if(Looper.myLooper() == Looper.getMainLooper()) {
              invoke(subscribeMethod, obj, type);
            }else { //子 - 主
              mHandler.post(new Runnable() {
                @Override
                public void run() {
                  invoke(subscribeMethod, obj, type);
                }
              });
            }

            break;
          case BACKGROUND:
            // 主 - 子
            if(Looper.myLooper() == Looper.getMainLooper()) {
              threadPool.execute(new Runnable() {
                @Override
                public void run() {
                  invoke(subscribeMethod, obj, type);
                }
              });
            }else { // 子 - 子
              invoke(subscribeMethod, obj, type);
            }
            // ExecutorService
            break;
          default:
            break;
        }
      }
    }
  }
}

private void invoke(SubscribeMethod subscribeMethod, Object obj, Object type) {
  Method method = subscribeMethod.getMethod();
  try {
    method.invoke(obj, type);
  } catch (IllegalAccessException e) {
    e.printStackTrace();
  } catch (InvocationTargetException e) {
    e.printStackTrace();
  }
}
```

### 2.3 反注册事件

将当前`Activity/Fragment`从`EventBus`维护的`Map`中移除。

```java
public void unregister(Object obj) {
  if(obj != null && cacheMap.get(obj) != null) {
    cacheMap.remove(obj);
  }
}
```

