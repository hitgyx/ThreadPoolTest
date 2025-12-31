package com.test;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyDelayQueue implements IMyQueue<DelayedTask<?>> {
    private final PriorityQueue<DelayedTask<?>> queue = new PriorityQueue<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    @Override
    public void put(DelayedTask<?> task) throws InterruptedException {
        lock.lock();
        try {
            queue.offer(task);
            condition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DelayedTask<?> take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (true) {
                DelayedTask<?> first = queue.peek();
                if (first == null) {
                    condition.await();
                } else {
                    long delay = first.getExecuteTime() - System.currentTimeMillis();
                    if (delay <= 0) {
                        return queue.poll();
                    }
                    condition.awaitNanos(TimeUnit.MILLISECONDS.toNanos(delay));
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DelayedTask<?> poll(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lockInterruptibly();
        try {
            while (true) {
                DelayedTask<?> first = queue.peek();
                if (first == null) {
                    if (nanos <= 0) return null;
                    nanos = condition.awaitNanos(nanos);
                } else {
                    long delay = first.getExecuteTime() - System.currentTimeMillis();
                    if (delay <= 0) {
                        return queue.poll();
                    }
                    if (nanos <= 0) return null;

                    long delayNanos = TimeUnit.MILLISECONDS.toNanos(delay);
                    long waitNanos = Math.min(nanos, delayNanos);

                    long remaining = condition.awaitNanos(waitNanos);
                    nanos -= (waitNanos - remaining);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<DelayedTask<?>> drainTo() {
        lock.lock();
        try {
            List<DelayedTask<?>> list = new ArrayList<>(queue);
            queue.clear();
            return list;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}