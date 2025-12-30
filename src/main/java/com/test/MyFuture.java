package com.test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyFuture<V> {
    private V result;
    private Exception exception; // 新增：保存异常
    private boolean isDone = false;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition doneCondition = lock.newCondition();

    // 当任务完成时，填入结果并唤醒等待的人
    public void set(V value) {
        lock.lock();
        try {
            this.result = value;
            this.isDone = true;
            doneCondition.signalAll(); // 唤醒所有在 get() 上等待的人
        } finally {
            lock.unlock();
        }
    }

    // 新增：设置异常的方法
    public void setException(Exception e) {
        lock.lock();
        try {
            this.exception = e;
            this.isDone = true; // 即使是异常，任务也算“结束”了
            doneCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    // 调用者通过 get() 拿结果，如果还没好就阻塞
    public V get() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (!isDone) {
                Logger.log("结果还没出来，我先等会儿...");
                doneCondition.await();
            }
            if (exception != null) {
                throw new RuntimeException("任务执行出错: " + exception.getMessage(), exception);
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
