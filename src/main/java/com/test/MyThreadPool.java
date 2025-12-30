package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyThreadPool {
    // 任务队列：存放 Runnable 任务
    private final MyBlockingQueue<Runnable> taskQueue;
    // 工作线程列表
    private final List<Worker> workers = new ArrayList<>();
    private volatile boolean isShutdown = false; // 状态位

    public MyThreadPool(int numThreads, int capacity) {
        this.taskQueue = new MyBlockingQueue<>(capacity);

        // 初始化并启动指定数量的工作线程
        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker("Worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    public void execute(Runnable task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("线程池已关闭，拒绝提交新任务");
        }
        taskQueue.put(task);
    }

    public void shutdown() {
        Logger.log("正在关闭线程池...");
        isShutdown = true;
        // 中断所有工作线程
//        for (Worker worker : workers) {
//            worker.interrupt();
//        }
    }

    public List<Runnable> shutdownNow() {
        Logger.log("！！！立即关闭线程池！！！");
        isShutdown = true;

        // 1. 提取所有还没开始执行的任务
        List<Runnable> droppedTasks = taskQueue.drainTo();

        // 2. 暴力中断所有正在执行的 Worker
        for (Worker worker : workers) {
            worker.interrupt();
        }

        Logger.log("已拦截未执行任务数量: " + droppedTasks.size());
        return droppedTasks;
    }

    public <V> MyFuture<V> submit(MyCallable<V> callable) throws InterruptedException {
        MyFuture<V> future = new MyFuture<>();
        execute(() -> {
            try {
                V result = callable.call();
                future.set(result);
            } catch (Exception e) {
                Logger.log("捕获到任务异常，正在传回 Future");
                future.setException(e); // 将异常塞入凭证
            }
        });
        return future;
    }

    // 内部类：打工人
    private class Worker extends Thread {
        public Worker(String name) { super(name); }

        @Override
        public void run() {
            // 不断从队列里拿任务，拿不到就会在 MyBlockingQueue.take() 里阻塞等待
            while (!isShutdown || !taskQueue.isEmpty()) {
                try {
                    // 尝试拿任务，最多等 100 毫秒
                    Runnable task = taskQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        Logger.log(getName() + " 开始执行任务...");
                        task.run();
                        Logger.log(getName() + " 任务执行完毕。");
                    }
                    // 如果 task == null，说明这 100ms 没活干，回到 while 循环开头检查状态
                } catch (InterruptedException e) {
                    // 如果在 poll 过程中收到中断信号（shutdownNow 情况）
                    Logger.log(getName() + " 收到中断信号，立即停止");
                    break;
                }
            }
        }
    }
}