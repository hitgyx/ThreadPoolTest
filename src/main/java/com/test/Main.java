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
            Logger.log("=== 开始基础执行测试 ===");
            testBasicExecute(pool);

            Thread.sleep(3000); // 等待基础测试完成

            Logger.log("=== 开始 Future 获取测试 ===");
            testFutureGet(pool);

            Logger.log("=== 开始定时任务测试 ===");
            testSchedule(pool);

            Thread.sleep(5000); // 预留足够时间让定时任务跑完

            Logger.log("=== 开始优雅关闭测试 ===");
            testGracefulShutdown(pool);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
}