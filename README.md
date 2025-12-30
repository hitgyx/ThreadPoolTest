# Custom Java Concurrency Toolkit (ThreadPool & Scheduler)

本项目是一个纯 Java 实现的高性能并发任务处理库。通过手写线程池核心组件，深入理解 Java 内存模型、JUC 工具包以及 Android Framework 的底层消息调度机制。

## 🛠 核心组件

### 1. MyDelayQueue (延时优先级队列)
* **原理**：基于 `ReentrantLock` 和 `Condition` 实现。
* **特性**：支持按时间戳排序，`take()` 方法会在任务未到期时通过 `awaitNanos()` 精准阻塞线程。
* **对标**：Android Framework 中的 `MessageQueue.next()`。

### 2. MyThreadPool (线程池)
* **原理**：维护一组 `Worker` 线程，通过死循环不断从队列中提取任务。
* **特性**：支持 `execute` (立即执行)、`schedule` (定时执行) 以及 `shutdown/shutdownNow` (优雅/暴力关闭)。
* **对标**：Java `ThreadPoolExecutor` 与 Android `HandlerThread`。

### 3. MyFuture & MyCallable (异步凭证)
* **原理**：利用状态位 `isDone` 和条件变量实现结果同步。
* **特性**：支持跨线程获取返回值，并能将 Worker 线程中的异常传递给调用者线程。
* **对标**：Java 标准库 `FutureTask`。

## 🧪 测试用例

项目 `Main.java` 中包含以下核心测试方法：

* **testScheduledExecution**: 验证不同延迟时间的任务是否能按时间顺序（而非提交顺序）执行。
* **testExceptionPropagation**: 验证任务执行崩溃时，主线程能否在 `future.get()` 时捕获异常。
* **testGracefulShutdown**: 验证线程池在关闭时，是否能将队列中剩余的任务处理完毕。

## 📝 开发者笔记：并发核心点
1. **虚假唤醒**：在 `await()` 时必须使用 `while` 循环检查条件。
2. **锁的释放**：`Condition.await()` 会自动释放当前持有的锁，否则会造成死锁。
3. **响应中断**：使用 `lockInterruptibly()` 确保线程在阻塞时可以被安全停止，防止僵尸线程。