package com.test;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 1. 先创建一个具体的消息队列实现（这里使用延时队列，因为它功能最全）
        IMyQueue<DelayedTask<?>> queue = new MyDelayQueue();

        // 2. 创建线程池，将队列注入进去
        // 现在的构造函数不再需要 capacity 参数，因为 MyDelayQueue 是基于优先级堆的自动扩容队列
        MyThreadPool pool = new MyThreadPool(2, queue);

        try {
//            Logger.log("=== 开始基础执行测试 ===");
//            testBasicExecute(pool);
//
//            Thread.sleep(3000); // 等待基础测试完成
//
//            Logger.log("=== 开始 Future 获取测试 ===");
//            testFutureGet(pool);
//
//            Logger.log("=== 开始定时任务测试 ===");
//            testSchedule(pool);
//
//            Thread.sleep(5000); // 预留足够时间让定时任务跑完
//
//            Logger.log("=== 开始优雅关闭测试 ===");
//            testGracefulShutdown(pool);

//            testProxyTask(pool); // 执行代理测试

//            // 权限检查
//            testPermissionProxy(pool);
//            testReflectHack();

//            testGenericReflection();

            testTheWholeFramework(pool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return;
    }

    /**
     * 测试基础任务提交与执行
     */
    public static void testBasicExecute(MyThreadPool pool) throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            pool.execute(() -> {
                Logger.log("正在处理基础任务 " + taskId);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Logger.log("任务 " + taskId + " 被中断");
                }
            });
        }
    }

    /**
     * 测试带有返回值的任务 (Future/Callable)
     */
    public static void testFutureGet(MyThreadPool pool) throws Exception {
        MyFuture<Integer> future = pool.submit(() -> {
            Logger.log("异步计算任务开始 (100+200)...");
            Thread.sleep(2000);
            return 300;
        });

        Logger.log("主线程去做别的事了...");
        Integer result = future.get(); // 此时主线程会阻塞，直到结果返回
        Logger.log("拿到异步结果: " + result);
    }

    /**
     * 测试定时/延迟任务
     */
    public static void testSchedule(MyThreadPool pool) throws Exception {
        Logger.log("提交延迟 3s 的任务");
        MyFuture<String> f1 = pool.schedule(() -> "我是 3s 任务的结果", 3000);

        Logger.log("提交延迟 1s 的任务");
        MyFuture<String> f2 = pool.schedule(() -> "我是 1s 任务的结果", 1000);

        // 验证非阻塞性：主线程可以先拿到延迟短的结果
        Logger.log("等待获取 1s 任务结果: " + f2.get());
        Logger.log("等待获取 3s 任务结果: " + f1.get());
    }

    /**
     * 测试优雅关闭
     */
    public static void testGracefulShutdown(MyThreadPool pool) throws InterruptedException {
        pool.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
            Logger.log("关闭前的最后一个清理任务完成");
        });

        pool.shutdown();
        Logger.log("已发出 shutdown 指令，Worker 线程将在处理完剩余任务后退出。");
    }

    public static void testProxyTask(MyThreadPool pool) throws Exception {
        Logger.log("=== 开始动态代理任务测试 ===");

        // 1. 创建真实对象
        UserService realService = new UserServiceImpl();

        // 2. 创建代理对象 (利用你之前写的 TaskProxyHandler)
        UserService proxyService = TaskProxyHandler.createProxy(realService, UserService.class);

        // 3. 将代理对象的调用丢进线程池
        // 注意：当我们调用 proxyService.getName() 时，会自动触发 TaskProxyHandler 的 invoke 方法
        pool.execute(() -> {
            proxyService.getName();
        });
    }

    public static void testPermissionProxy(MyThreadPool pool) throws Exception {
        UserService realService = new UserServiceImpl();
        UserService proxyService = TaskProxyHandler.createProxy(realService, UserService.class);

        pool.execute(() -> {
            Logger.log("--- 准备执行普通任务 ---");
            proxyService.getName(); // 应该成功

            Logger.log("--- 准备执行高危任务 ---");
            proxyService.deleteDatabase(); // 应该被拦截，不会打印“执行高危操作”
        });
    }

    public static void testReflectHack() throws Exception {
        Logger.log("=== 开始暴力反射测试 ===");
        UserServiceImpl service = new UserServiceImpl();
        Class<?> clazz = service.getClass();

        // 1. 读取私有变量
        // getField 只能拿 public，getDeclaredField 可以拿所有声明过的变量
        java.lang.reflect.Field field = clazz.getDeclaredField("secretKey");
        // 关键：突破 private 限制
        field.setAccessible(true);
        String value = (String) field.get(service);
        Logger.log("[反射获取] 成功拿到私有变量 secretKey: " + value);

        // 2. 调用私有方法
        // 这里的参数需要匹配方法名和参数类型列表
        java.lang.reflect.Method privateMethod = clazz.getDeclaredMethod("internalDebug", String.class);
        privateMethod.setAccessible(true);
        // 强制执行方法
        privateMethod.invoke(service, "ADMIN-CONFIRM");
    }

    public static void testGenericReflection() {
        Logger.log("=== 开始泛型反射测试 ===");

        // 实例化子类
        UserRepository userRepo = new UserRepository();

        // 观察它是否能“记起”自己被擦除的泛型
        userRepo.printType();
    }

    public static void testTheWholeFramework(MyThreadPool pool) {
        SmartTaskDispatcher dispatcher = new SmartTaskDispatcher(pool);
        UserService service = dispatcher.createService(UserService.class, new UserServiceImpl());

        Logger.log("[Main] 发起异步请求...");

        service.getName(new TaskCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Logger.log("[Main] 收到最终结果并在 UI 线程展示: " + result);
            }

            @Override
            public void onError(Throwable t) {
                Logger.log("[Main] 发生错误: " + t.getMessage());
            }
        });

        Logger.log("[Main] 已经继续执行后续代码，不被阻塞。");
    }
}