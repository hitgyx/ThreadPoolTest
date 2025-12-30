package com.test;

@FunctionalInterface
public interface MyCallable<V> {
    V call() throws Exception;
}
