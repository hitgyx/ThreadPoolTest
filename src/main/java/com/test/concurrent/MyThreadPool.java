package com.test.concurrent;

import com.test.model.DelayedTask;
import com.test.utils.Logger;
import com.test.base.IMyQueue;

import java.util.ArrayList;
import java.util.List;

public class MyThreadPool {
    // 1. 必须使用 volatile 修饰！防止指令重排序
    private static volatile MyThreadPool instance;

    // 核心解耦：持有接口，不关心具体是阻塞队列还是延时队列
    private final IMyQueue<DelayedTask<?>> taskQueue;
    private final List<Worker> workers = new ArrayList<>();
    private volatile boolean isShutdown = false;

    /**
     * @param numThreads 线程数量
     * @param queue 注入具体的队列实现（如 MyDelayQueue 或 MyBlockingQueue）
     */
    private MyThreadPool(int numThreads, IMyQueue<DelayedTask<?>> queue) {
        this.taskQueue = queue;

        // 初始化并启动工作线程
        for (int i = 0; i < numThreads; i++) {
            Worker worker = new Worker("Worker-" + i);
            workers.add(worker);
            worker.start();
        }
    }

    // 3. 双重检查锁定的核心获取方法
    public static MyThreadPool getInstance(int numThreads, IMyQueue<DelayedTask<?>> queue) {
        // 第一重检查：如果实例已经存在，直接返回，避免进入 synchronized 块带来的性能损耗
        if (instance == null) {
            synchronized (MyThreadPool.class) {
                // 第二重检查：在占有锁的情况下再次检查。
                // 防止线程 A 和线程 B 同时通过了第一重检查，A 先拿锁创建了对象，B 等 A 释放后如果不检查就会再创建一次
                if (instance == null) {
                    instance = new MyThreadPool(numThreads, queue);
                }
            }
        }
        return instance;
    }

    // 兼容旧的 execute 方法，将其转化为延迟 0 毫秒的任务
    public void execute(Runnable runnableTask) throws InterruptedException {
        submit(() -> {
            runnableTask.run();
            return null;
        });
    }

    public <V> MyFuture<V> submit(MyCallable<V> callable) throws InterruptedException {
        return schedule(callable, 0);
    }

    public <V> MyFuture<V> schedule(MyCallable<V> callable, long delayMs) throws InterruptedException {
        if (isShutdown) throw new IllegalStateException("ThreadPool is closed");

        MyFuture<V> future = new MyFuture<>();
        // 将任务包装成 DelayedTask 存入通用接口队列
        DelayedTask<V> task = new DelayedTask<>(callable, future, delayMs);
        taskQueue.put(task);
        return future;
    }

    public void shutdown() {
        Logger.log("正在关闭线程池...");
        isShutdown = true;
        // 注意：优雅关闭通常不立即 interrupt，而是让 Worker 检查 isEmpty 后自行退出
    }

    public List<DelayedTask<?>> shutdownNow() {
        Logger.log("！！！立即关闭线程池！！！");
        isShutdown = true;

        // 1. 提取所有还没开始执行的任务（由于接口化，这里返回的是 DelayedTask 列表）
        List<DelayedTask<?>> droppedTasks = taskQueue.drainTo();

        // 2. 强制中断
        for (Worker worker : workers) {
            worker.interrupt();
        }

        Logger.log("已拦截未执行任务数量: " + droppedTasks.size());
        return droppedTasks;
    }

    private class Worker extends Thread {
        public Worker(String name) { super(name); }

        @Override
        public void run() {
            // 只要没关闭，或者队列里还有活没干完，就继续
            while (!isShutdown || !taskQueue.isEmpty()) {
                try {
                    // 使用 poll 带有超时机制，方便在 shutdown 时快速响应状态位变化
                    DelayedTask<?> task = taskQueue.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (task != null) {
                        task.run();
                    }
                } catch (InterruptedException e) {
                    if (isShutdown) break;
                }
            }
            Logger.log(getName() + " 优雅退出");
        }
    }
}