package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MyThreadPool {
    // 任务队列：存放 Runnable 任务
    private final MyBlockingQueue<Runnable> taskQueue;
    private final MyDelayQueue<DelayedTask<?>> delayTaskQueue;
    // 工作线程列表
    private final List<Worker> workers = new ArrayList<>();
    private volatile boolean isShutdown = false; // 状态位

    public MyThreadPool(int numThreads, int capacity) {
        this.taskQueue = new MyBlockingQueue<>(capacity);
        this.delayTaskQueue = new MyDelayQueue<>();

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

//    public <V> MyFuture<V> submit(MyCallable<V> callable) throws InterruptedException {
//        MyFuture<V> future = new MyFuture<>();
//        execute(() -> {
//            try {
//                V result = callable.call();
//                future.set(result);
//            } catch (Exception e) {
//                Logger.log("捕获到任务异常，正在传回 Future");
//                future.setException(e); // 将异常塞入凭证
//            }
//        });
//        return future;
//    }

    // 立即执行：相当于延迟 0 毫秒
    public <V> MyFuture<V> submit(MyCallable<V> callable) throws InterruptedException {
        return schedule(callable, 0);
    }

    // 定时执行
    public <V> MyFuture<V> schedule(MyCallable<V> callable, long delayMs) throws InterruptedException {
        if (isShutdown) throw new IllegalStateException("ThreadPool is closed");

        MyFuture<V> future = new MyFuture<>();
        DelayedTask<V> task = new DelayedTask<>(callable, future, delayMs);
        delayTaskQueue.put(task); // 放入延时队列
        return future;
    }

    // 内部类：打工人
    private class Worker extends Thread {
        public Worker(String name) { super(name); }

        @Override
        public void run() {
            // 优雅退出逻辑：只要没关闭，或者队列里还有定时任务没到期
            while (!isShutdown || !delayTaskQueue.isEmpty()) {
                try {
                    // take() 会阻塞，直到有任务且时间到达
                    DelayedTask<?> task = delayTaskQueue.take();
                    if (task != null) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    if (isShutdown) break; // 收到中断且已关闭，则退出
                }
            }
            Logger.log(getName() + " 优雅退出");
        }
    }
}