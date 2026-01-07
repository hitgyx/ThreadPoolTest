package com.test.base;

/**
 * 异步任务回调接口
 * @param <T> 结果的类型
 */
public interface TaskCallback<T> {
    void onSuccess(T result);
    void onError(Throwable t);
}