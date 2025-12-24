package com.test;

public class Main {
    public static void main(String[] args) {
        // 创建一个拥有 2 个线程、容量为 5 的线程池
        MyThreadPool pool = new MyThreadPool(2, 5);

        // 提交 10 个任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            try {
                pool.execute(() -> {
                    try {
                        // 模拟任务执行耗时
                        Thread.sleep(1000);
                        Logger.log("任务 " + taskId + " 正在被处理...");
                    } catch (InterruptedException e) {
                        Logger.log("任务被取消，不再执行后续逻辑");
                        return; // 关键：立即结束 run 方法
                    }
                    Logger.log("任务正常完成");
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        pool.shutdown();
    }
}