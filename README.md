## NeEventBus 手写EventBust框架 
思路：  
EventBus 作为Activity/Fragment 的中间桥梁，管理了其中被Subscribe注解的方法，
当post方法调用时遍历 并 通过反射执行器方法。

