package com.test.service;

import com.test.annotation.TraceTask;
import com.test.base.TaskCallback;

public interface UserService {
    @TraceTask(value = "获取用户名", permission = "USER")
    void getName();

    @TraceTask(value = "获取用户名", permission = "USER")
    String getName(TaskCallback<String> callback);

    @TraceTask(value = "删除数据库", permission = "ADMIN")
    void deleteDatabase(); // 这个方法应该会被拦截
}
