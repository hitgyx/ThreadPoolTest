package com.test;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 创建一个拥有 2 个线程、容量为 5 的线程池
        MyThreadPool pool = new MyThreadPool(2, 20);

        try {
//            testBasicExecute(pool);
//            testFutureGet(pool);
//            testGracefulShutdown(pool);
            testSchedule(pool);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testBasicExecute(MyThreadPool pool) throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            pool.execute(() -> {
                Logger.log("正在处理任务 " + taskId);
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            });
        }
    }

    public static void testFutureGet(MyThreadPool pool) throws Exception {
        MyFuture<Integer> future = pool.submit(() -> {
            Logger.log("计算任务开始...");
            Thread.sleep(2000);
            return 42;
        });

        Logger.log("主线程去做别的事了...");
        Integer result = future.get(); // 阻塞等待
        Logger.log("拿到异步结果: " + result);
    }

    public static void testGracefulShutdown(MyThreadPool pool) throws InterruptedException {
        pool.execute(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException e) {}
            Logger.log("最后的老任务完成");
        });
        pool.shutdown();
        Logger.log("已发出关闭指令，等待 Worker 退出...");
    }

    public static void testSchedule(MyThreadPool pool) throws Exception {
        Logger.log("提交 3s 延迟任务");
        MyFuture<String> f1 = pool.schedule(() -> "Task 3s", 3000);

        Logger.log("提交 1s 延迟任务");
        MyFuture<String> f2 = pool.schedule(() -> "Task 1s", 1000);

        Logger.log("1s 任务结果: " + f2.get());
        Logger.log("3s 任务结果: " + f1.get());
    }
}

