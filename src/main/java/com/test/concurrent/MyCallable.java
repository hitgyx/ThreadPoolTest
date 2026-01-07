package com.test.concurrent;

@FunctionalInterface
public interface MyCallable<V> {
    V call() throws Exception;
}
