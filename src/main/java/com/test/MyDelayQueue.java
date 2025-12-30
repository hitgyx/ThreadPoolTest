package com.test;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyDelayQueue<T extends DelayedTask<?>> {
    private final PriorityQueue<T> queue = new PriorityQueue<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public void put(T task) {
        lock.lock();
        try {
            queue.offer(task);
            // 每次放入新任务，都要唤醒消费者去检查：万一这个新任务是最早要执行的呢？
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (true) {
                T first = queue.peek();
                if (first == null) {
                    condition.await(); // 没任务，死等
                } else {
                    long delay = first.getExecuteTime() - System.currentTimeMillis();
                    if (delay <= 0) {
                        // 时间到了，可以拿走执行
                        return queue.poll();
                    }
                    // 时间还没到，精准地睡 delay 毫秒
                    // 注意：在 await 期间，如果有新任务进入（put调用了signal），这里会被提前唤醒
                    condition.awaitNanos(TimeUnit.MILLISECONDS.toNanos(delay));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            // 直接调用底层 PriorityQueue 的 isEmpty
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}
