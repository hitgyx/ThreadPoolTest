package com.test.model;

import com.test.utils.Logger;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

// 定义一个泛型基类
public abstract class BaseRepository<T> {
    private final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    public BaseRepository() {
        Logger.log("[构造链路] 当前正在初始化的实例类名: " + this.getClass().getName());

        // 【核心代码】通过反射获取父类带泛型的类型
        Type superClass = getClass().getGenericSuperclass();

        // 强制转换为参数化类型 (ParameterizedType)
        ParameterizedType parameterizedType = (ParameterizedType) superClass;

        // 获取真实的泛型参数数组 (T)
        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

        // 拿到第一个泛型 T 的真实 Class 对象
        this.entityClass = (Class<T>) actualTypeArguments[0];
    }

    public void printType() {
        Logger.log("[泛型反射] 当前 Repository 处理的真实类型是: " + entityClass.getSimpleName());
    }
}