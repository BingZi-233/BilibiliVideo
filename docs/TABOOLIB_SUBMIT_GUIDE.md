# TabooLib Submit 使用指南

本文档总结了TabooLib中`submit`函数的各种用法和最佳实践。

## 基本导入

```kotlin
import taboolib.common.platform.function.submit
import taboolib.platform.util.PlatformTask
```

## 基本用法

### 1. 基础任务提交

```kotlin
submit {
    // 任务逻辑
    println("在主线程中执行")
}
```

### 2. 异步执行

```kotlin
submit(async = true) {
    // 在异步线程中执行，适用于网络请求、文件操作等耗时任务
    val result = performNetworkRequest()
    
    // 切换回主线程处理结果
    submit(async = false) {
        updateUI(result)
    }
}
```

### 3. 延迟执行

```kotlin
submit(delay = 20) {
    // 延迟20个tick（1秒）后执行
    println("延迟执行的任务")
}
```

### 4. 重复执行

```kotlin
// 基本重复执行
val task = submit(period = 20) {
    // 每20个tick（1秒）执行一次
    performPeriodicTask()
}

// 可以通过返回的task对象取消
task.cancel()

// 带延迟的重复执行
val delayedTask = submit(delay = 40, period = 20) {
    // 延迟2秒后开始，然后每1秒执行一次
    performPeriodicTask()
}

// 异步重复执行
val asyncTask = submit(async = true, period = 100) {
    // 每5秒在异步线程中执行一次
    performHeavyTask()
}
```

### 5. 立即执行

```kotlin
submit(now = true) {
    // 立即执行，不进入调度队列
    urgentTask()
}
```

## 组合用法

### 延迟异步执行

```kotlin
submit(async = true, delay = 40) {
    // 延迟2秒后在异步线程中执行
    val data = fetchDataFromAPI()
    
    submit(async = false) {
        processData(data)
    }
}
```

### 重复异步任务

```kotlin
submit(async = true, period = 100) {
    // 每5秒在异步线程中执行一次
    checkServerStatus()
}
```

## 实际应用场景

### 1. 网络请求模式

```kotlin
// 错误示例：直接在主线程进行网络请求
fun badNetworkCall() {
    val response = apiClient.get("/api/data") // 阻塞主线程
    updateUI(response)
}

// 正确示例：异步网络请求
fun goodNetworkCall() {
    submit(async = true) {
        val response = apiClient.get("/api/data")
        
        submit(async = false) {
            updateUI(response)
        }
    }
}
```

### 2. 轮询模式

```kotlin
class PollingService {
    private val isActive = AtomicBoolean(true)
    private var pollingTask: PlatformTask? = null
    
    fun startPolling() {
        if (!isActive.compareAndSet(false, true)) {
            return // 已经在运行
        }
        
        fun pollOnce() {
            if (!isActive.get()) return
            
            submit(async = true) {
                val result = checkStatus()
                
                submit(async = false) {
                    if (!isActive.get()) return@submit
                    
                    when (result.status) {
                        "success" -> {
                            stopPolling()
                            onSuccess()
                        }
                        "error" -> {
                            stopPolling()
                            onError(result.message)
                        }
                        else -> {
                            // 继续轮询 - 延迟1秒后再次执行
                            submit(delay = 20) { 
                                pollOnce() 
                            }
                        }
                    }
                }
            }
        }
        
        // 开始第一次轮询
        pollOnce()
    }
    
    fun stopPolling() {
        isActive.set(false)
        pollingTask?.cancel()
        pollingTask = null
    }
}

// 或者使用period参数实现固定间隔轮询（不推荐用于需要动态停止的场景）
class SimplePollingService {
    private var pollingTask: PlatformTask? = null
    
    fun startPolling() {
        pollingTask = submit(async = true, period = 20) {
            val result = checkStatus()
            
            submit(async = false) {
                when (result.status) {
                    "success" -> {
                        pollingTask?.cancel()
                        onSuccess()
                    }
                    "error" -> {
                        pollingTask?.cancel() 
                        onError(result.message)
                    }
                    else -> {
                        // 继续轮询（period会自动处理重复）
                        updateStatus(result.message)
                    }
                }
            }
        }
    }
    
    fun stopPolling() {
        pollingTask?.cancel()
        pollingTask = null
    }
}
```

### 3. 数据库操作模式

```kotlin
fun saveUserData(userData: UserData) {
    submit(async = true) {
        val success = database.save(userData)
        
        submit(async = false) {
            if (success) {
                showMessage("保存成功")
            } else {
                showError("保存失败")
            }
        }
    }
}
```

### 4. 延迟重试模式

```kotlin
fun retryOperation(maxRetries: Int = 3, currentRetry: Int = 0) {
    if (currentRetry >= maxRetries) {
        onFailed("超过最大重试次数")
        return
    }
    
    submit(async = true) {
        val result = performOperation()
        
        submit(async = false) {
            if (result.isSuccess) {
                onSuccess(result)
            } else {
                // 延迟重试
                val delay = (currentRetry + 1) * 20 // 递增延迟
                submit(delay = delay) {
                    retryOperation(maxRetries, currentRetry + 1)
                }
            }
        }
    }
}
```

## 最佳实践

### 1. 线程安全

```kotlin
// 使用AtomicBoolean确保线程安全的状态控制
private val isRunning = AtomicBoolean(false)

fun startTask() {
    if (!isRunning.compareAndSet(false, true)) {
        return // 已经在运行
    }
    
    submit(async = true) {
        try {
            performTask()
        } finally {
            isRunning.set(false)
        }
    }
}
```

### 2. 资源清理

```kotlin
class ResourceManager {
    private val tasks = mutableSetOf<String>()
    
    fun addTask(id: String) {
        tasks.add(id)
        
        submit(async = true) {
            performTask(id)
            
            submit(async = false) {
                tasks.remove(id)
                if (tasks.isEmpty()) {
                    onAllTasksComplete()
                }
            }
        }
    }
}
```

### 3. 错误处理

```kotlin
fun safeAsyncOperation() {
    submit(async = true) {
        try {
            val result = riskyOperation()
            
            submit(async = false) {
                handleSuccess(result)
            }
        } catch (e: Exception) {
            submit(async = false) {
                handleError(e)
            }
        }
    }
}
```

## 时间单位说明

- **1 tick = 50ms（1/20秒）**
- `delay = 20` = 1秒延迟
- `delay = 40` = 2秒延迟
- `period = 20` = 每秒执行一次
- `period = 100` = 每5秒执行一次

## 注意事项

1. **避免在异步线程中操作UI或游戏状态**
2. **长时间运行的任务应该使用异步执行**
3. **网络请求和数据库操作必须在异步线程中进行**
4. **使用AtomicBoolean等线程安全的数据结构**
5. **及时清理资源，避免内存泄漏**
6. **适当的错误处理和异常捕获**

## 常见错误

### 错误：在异步线程中操作游戏对象

```kotlin
// 错误示例
submit(async = true) {
    player.sendMessage("消息") // 可能导致线程安全问题
}

// 正确示例
submit(async = true) {
    val data = fetchData()
    
    submit(async = false) {
        player.sendMessage("数据: $data")
    }
}
```

### 错误：忘记清理重复任务

```kotlin
// 错误示例：没有保存任务引用，无法停止
submit(period = 20) {
    // 这个任务会永远运行下去，无法停止
    doSomething()
}

// 正确示例：保存任务引用以便后续取消
class TaskManager {
    private var periodicTask: PlatformTask? = null
    
    fun startTask() {
        if (periodicTask != null) {
            return // 已经在运行
        }
        
        periodicTask = submit(period = 20) {
            if (shouldStop()) {
                stopTask()
                return@submit
            }
            doSomething()
        }
    }
    
    fun stopTask() {
        periodicTask?.cancel()
        periodicTask = null
    }
}
```

## 参考资料

- [TabooLib官方文档](https://docs.tabooproject.org/)
- [TabooLib源码 - Executor.kt](https://github.com/TabooLib/taboolib/blob/master/common-platform-api/src/main/kotlin/taboolib/common/platform/function/Executor.kt)