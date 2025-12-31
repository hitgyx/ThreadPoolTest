package com.test;

// 实现 Runnable 接口，方便 Worker 直接调用 run()
public class DelayedTask<V> implements Comparable<DelayedTask<V>>, Runnable{
    private final MyCallable<V> callable;
    private final MyFuture<V> future;
    private final long executeTime;

    public DelayedTask(MyCallable<V> callable, MyFuture<V> future, long delayMs) {
        this.callable = callable;
        this.future = future;
        this.executeTime = System.currentTimeMillis() + delayMs;
    }

    @Override
    public void run() {
        try {
            V result = callable.call();
            future.set(result);
        } catch (Exception e) {
            future.setException(e);
        }
    }

    public long getExecuteTime() { return executeTime; }

    @Override
    public int compareTo(DelayedTask<V> other) {
        return Long.compare(this.executeTime, other.executeTime);
    }
}
