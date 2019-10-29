package com.sty.ne.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by tian on 2019/10/29.
 */

public class EventBus {
    private Map<Object, List<SubscribeMethod>> cacheMap;
    private static volatile EventBus instance;
    private Handler mHandler;
    private ExecutorService threadPool;

    private EventBus(){
        cacheMap = new HashMap<>();
        mHandler = new Handler();
        threadPool = new ThreadPoolExecutor(5, 10, 60L,
                TimeUnit.SECONDS, new ArrayBlockingQueue(10));
    }

    public static EventBus getDefault() {
        if(instance == null) {
            synchronized (EventBus.class) {
                if(instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

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
}
