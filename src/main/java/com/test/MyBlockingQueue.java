package com.test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyBlockingQueue<T> {
    private final LinkedList<T> queue = new LinkedList<>();
    private final int capacity;
    private final ReentrantLock lock = new ReentrantLock();

    // 核心：利用两个 Condition 实现精准唤醒
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    public MyBlockingQueue(int capacity) {
        this.capacity = capacity;
    }

    public void put(T item) throws InterruptedException {
        lock.lockInterruptibly(); // 建议使用可中断锁，这是 Framework 开发中的好习惯
        Logger.log(Thread.currentThread().getName() + " 获取到了锁");
        try {
            // TODO: 如果队列满了，应该挂起在哪一个 Condition 上？
            // 【关键点】这里必须使用 while，不能用 if
            while (queue.size() == capacity) {
                Logger.log("队列已满，生产者线程 " + Thread.currentThread().getName() + " 进入等待...");
                notFull.await();
            }

            queue.addLast(item);
            Logger.log(Thread.currentThread().getName() + " 准备释放锁");
            // 既然放了东西，就通知那些因为“没东西吃”而阻塞的消费者线程
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            // 1. 同样使用 while 循环防止虚假唤醒
            // 如果队列为空，则消费者挂起在 notEmpty 条件上
            while (queue.isEmpty()) {
                Logger.log("队列为空，消费者线程 " + Thread.currentThread().getName() + " 进入等待...");
                notEmpty.await();
            }

            // 2. 核心操作：取走队头元素
            T item = queue.removeFirst();

            // 3. 既然消费了一个空间，通知生产者：现在队列“不为满”了，可以继续生产
            notFull.signal();

            return item;
        } finally {
            lock.unlock();
        }
    }

    // 1. 判断是否为空
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    // 2. 带超时的获取任务（非阻塞拿，拿不到等一会儿，再拿不到就返回 null）
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (queue.isEmpty()) {
                if (nanos <= 0L) {
                    return null; // 超时了，不再等了，返回空
                }
                // awaitNanos 会释放锁并等待，返回剩余的等待时间
                nanos = notEmpty.awaitNanos(nanos);
            }
            T item = queue.removeFirst();
            notFull.signal(); // 拿走了一个，通知生产者
            return item;
        } finally {
            lock.unlock();
        }
    }

    public List<T> drainTo() {
        lock.lock();
        try {
            List<T> remaining = new ArrayList<>(queue);
            queue.clear();
            // 既然清空了，可以通知生产者（虽然在关闭时这不重要，但保持逻辑完整）
            notFull.signalAll();
            return remaining;
        } finally {
            lock.unlock();
        }
    }
}