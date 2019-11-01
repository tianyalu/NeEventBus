## NeEventBus 手写EventBust框架 
### 思路：  
EventBus 作为Activity/Fragment 的中间桥梁，管理了其中被Subscribe注解的方法，以Object为键，
放在Map<Object, List<SubscribeMethod>> Map中；当post方法调用时遍历Map,遍历List,找到所有以bean参数类型的方法，
并通过反射执行这些方法。
### 核心代码
```android 
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

    public void unregister(Object obj) {
        if(obj != null && cacheMap.get(obj) != null) {
            cacheMap.remove(obj);
        }
    }
```

