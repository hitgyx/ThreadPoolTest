package com.test.concurrent;

import com.test.utils.Logger;

import java.util.concurrent.Executor;

/**
 * 模拟 Android 的 Handler(Looper.getMainLooper())
 */
public class MainThreadExecutor implements Executor {
    @Override
    public void execute(Runnable command) {
        // 在真实 Android 中，这里是 handler.post(command)
        // 在这里我们手动打印日志，模拟切换回了 UI 线程
        new Thread(() -> {
            Thread.currentThread().setName("UI-Thread");
            Logger.log("[UI 线程] 正在更新界面渲染...");
            command.run();
        }).start();
    }
}