package com.test.base;

import com.test.utils.Logger;
import com.test.concurrent.MainThreadExecutor;
import com.test.concurrent.MyThreadPool;
import com.test.annotation.TraceTask;

import java.lang.reflect.Proxy;

public class SmartTaskDispatcher {
    private final MyThreadPool pool;
    private final MainThreadExecutor uiExecutor = new MainThreadExecutor();

    public SmartTaskDispatcher(MyThreadPool pool) {
        this.pool = pool;
    }

    @SuppressWarnings("unchecked")
    public <T> T createService(Class<T> serviceInterface, Object realServiceImpl) {
        return (T) Proxy.newProxyInstance(
                serviceInterface.getClassLoader(),
                new Class<?>[]{serviceInterface},
                (proxy, method, args) -> {
                    // 1. 识别注解
                    if (method.isAnnotationPresent(TraceTask.class)) {
                        TraceTask annotation = method.getAnnotation(TraceTask.class);
                        Logger.log("[分发器] 捕获异步请求: " + annotation.value());

                        // 2. 寻找参数中的回调接口
                        TaskCallback<Object> callback = null;
                        if (args != null && args.length > 0 && args[args.length - 1] instanceof TaskCallback) {
                            callback = (TaskCallback<Object>) args[args.length - 1];
                        }

                        final TaskCallback<Object> finalCallback = callback;

                        // 3. 开启后台线程执行业务
                        pool.execute(() -> {
                            try {
                                Logger.log("[Worker] 正在后台执行业务逻辑...");
                                Object result = method.invoke(realServiceImpl, args);

                                // 4. 执行成功，切换回“主线程”回调
                                if (finalCallback != null) {
                                    uiExecutor.execute(() -> finalCallback.onSuccess(result));
                                }
                            } catch (Exception e) {
                                if (finalCallback != null) {
                                    uiExecutor.execute(() -> finalCallback.onError(e));
                                }
                            }
                        });
                        return null; // 异步方法立即返回
                    }
                    return method.invoke(realServiceImpl, args);
                }
        );
    }
}