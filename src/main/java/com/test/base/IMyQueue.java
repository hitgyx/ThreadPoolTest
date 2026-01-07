package com.test.base;

public interface IMyQueue<T> {
    /** 放入任务 */
    void put(T task) throws InterruptedException;

    /** 取出任务（阻塞） */
    T take() throws InterruptedException;

    /** 获取带超时的任务（用于线程池优雅退出） */
    T poll(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException;

    /** 队列是否为空 */
    boolean isEmpty();

    /** 清空并返回剩余任务（用于 shutdownNow） */
    java.util.List<T> drainTo();
}
