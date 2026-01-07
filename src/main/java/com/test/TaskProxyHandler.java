package com.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class TaskProxyHandler implements InvocationHandler {
    private final Object target;
    // 模拟当前登录用户的权限等级 (实际开发中可能来自 Session 或 Context)
    private static final String CURRENT_USER_PERMISSION = "USER";

    public TaskProxyHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 检查方法上是否有 @TraceTask 注解
        if (method.isAnnotationPresent(TraceTask.class)) {
            TraceTask annotation = method.getAnnotation(TraceTask.class);
            String requiredPermission = annotation.permission();

            Logger.log("[权限检查] 尝试访问: " + annotation.value() + " | 需要权限: " + requiredPermission);

            // 2. 权限校验逻辑
            if ("ADMIN".equals(requiredPermission) && !"ADMIN".equals(CURRENT_USER_PERMISSION)) {
                Logger.log("[拒绝访问] 权限不足！当前权限: " + CURRENT_USER_PERMISSION + "，拒绝执行 " + method.getName());
                // 如果是带返回值的，这里可以返回 null 或抛出自定义异常
                return null;
            }

            // 3. 校验通过，记录耗时并执行
            Logger.log("[权限通过] 准备执行...");
            long start = System.currentTimeMillis();
            Object result = method.invoke(target, args);
            long end = System.currentTimeMillis();
            Logger.log("[统计] 执行耗时: " + (end - start) + "ms");
            return result;
        }

        return method.invoke(target, args);
    }

    // 辅助方法：创建代理对象
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, Class<T> interfaceType) {
        return (T) Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[]{interfaceType},
                new TaskProxyHandler(target)
        );
    }
}