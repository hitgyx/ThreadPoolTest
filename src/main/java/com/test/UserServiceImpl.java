package com.test;

public class UserServiceImpl implements UserService {
    // 模拟私有变量
    private String secretKey = "SS-999-XP";

    @Override
    public String getName(TaskCallback<String> callback) {
        // 注意：这里的逻辑依然是业务逻辑
        try {
            Logger.log("[业务逻辑] 正在查询数据库获取用户名...");
            Thread.sleep(800); // 模拟耗时操作
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "张三 (Secret: " + secretKey + ")"; // 返回结果，供分发器捕获
    }

    @Override
    public void getName() {
        // 模拟业务处理耗时
        try {
            Thread.sleep(800);
            Logger.log("[业务逻辑] 正在查询数据库获取用户名...");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteDatabase() {
        Logger.log("[业务逻辑] 正在执行高危操作：删除数据库...");
    }

    // 私有方法供反射测试使用
    private void internalDebug(String code) {
        Logger.log("[秘密方法] 内部调试启动，校验码: " + code);
    }
}