package com.test;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        // 创建一个拥有 2 个线程、容量为 5 的线程池
        MyThreadPool pool = new MyThreadPool(2, 20);

        // 提交 10 个任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Logger.log("准备提交任务 " + i);
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
            Logger.log("任务 " + i + " 提交成功");
        }

        // 稍微等 500ms，让前两个任务先跑起来
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // 突然决定立刻关闭
        List<Runnable> notStarted = pool.shutdownNow();

        Logger.log("未完成的任务列表如下：");
        for (Runnable r : notStarted) {
            // 因为是 lambda，打印出来可能不太好看，但能证明它们拿到了
            Logger.log("待补偿任务: " + r);
        }
    }
}