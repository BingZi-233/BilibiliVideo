# QRCode API 包

## 主要用途和功能概述

本包是BilibiliVideo插件的二维码发送模块核心API，提供了一套完整的、可扩展的二维码发送框架。该框架支持多种二维码发送方式，包括发送器的注册管理、依赖检查、生命周期管理等功能。

### 核心特性

- **可插拔的发送器架构**：支持多种二维码发送实现（如本地图片、URL、邮件等）
- **依赖管理系统**：自动检查和管理发送器所需的依赖项
- **错误处理机制**：结构化的错误代码和异常处理
- **异步支持**：支持同步和异步两种发送方式
- **线程安全**：所有核心组件都是线程安全的
- **生命周期管理**：完整的发送器初始化和清理机制

## 重要的类和接口

### 核心接口

#### QRCodeSender
```kotlin
interface QRCodeSender
```
二维码发送器的核心接口，定义了发送器必须实现的所有操作：
- `val id: String` - 发送器唯一标识符
- `val name: String` - 发送器显示名称
- `fun send()` - 同步发送二维码
- `fun sendAsync()` - 异步发送二维码
- `fun isAvailable()` - 检查发送器可用性
- `fun checkDependencies()` - 检查依赖项状态
- `fun initialize()` / `fun shutdown()` - 生命周期管理

### 管理类

#### QRCodeSenderRegistry
```kotlin
object QRCodeSenderRegistry
```
单例注册表，负责管理所有二维码发送器：
- 发送器的注册和注销
- 激活状态管理（同时只能有一个激活发送器）
- 依赖检查和可用性验证
- 线程安全的并发访问控制

### 数据类

#### SendResult（密封类）
```kotlin
sealed class SendResult
```
表示发送结果的三种状态：
- `Success` - 发送成功，包含时间戳和元数据
- `Failure` - 发送失败，包含错误原因和重试标识
- `Partial` - 部分成功，用于批量操作场景

#### SendOptions
```kotlin
data class SendOptions
```
发送选项配置类：
- `expireTime` - 过期时间（默认60秒）
- `retryCount` - 重试次数（默认0）
- `customData` - 自定义参数映射

#### DependencyResult
```kotlin
data class DependencyResult
```
依赖检查结果：
- `satisfied` - 依赖是否满足
- `missingDependencies` - 缺失的必需依赖
- `missingSoftDependencies` - 缺失的可选依赖
- `details` - 详细依赖状态映射

### 枚举类

#### ErrorCode
定义了六种错误类型：
- `DEPENDENCY_MISSING` - 依赖缺失
- `SENDER_NOT_AVAILABLE` - 发送器不可用
- `INITIALIZATION_FAILED` - 初始化失败
- `SEND_FAILED` - 发送失败
- `INVALID_OPTIONS` - 选项无效
- `PLAYER_OFFLINE` - 玩家离线

#### DependencyStatus
依赖项状态：
- `PRESENT` - 存在且可用
- `MISSING` - 缺失
- `VERSION_MISMATCH` - 版本不匹配
- `DISABLED` - 被禁用

### 异常类

#### QRCodeSenderException
```kotlin
class QRCodeSenderException(message: String, cause: Throwable?, val errorCode: ErrorCode)
```
专用异常类，包含结构化的错误代码。

## 主要方法和功能点

### 发送器管理

```kotlin
// 注册发送器
QRCodeSenderRegistry.register(sender)

// 激活发送器
QRCodeSenderRegistry.activate("senderId")

// 获取当前激活的发送器
val activeSender = QRCodeSenderRegistry.getActiveSender()

// 获取所有可用发送器
val availableSenders = QRCodeSenderRegistry.getAvailableSenders()
```

### 二维码发送

```kotlin
// 同步发送
val result = sender.send(player, "qr_content", SendOptions())

// 异步发送
sender.sendAsync(player, "qr_content", SendOptions()) { result ->
    when (result) {
        is SendResult.Success -> println("发送成功")
        is SendResult.Failure -> println("发送失败: ${result.reason}")
        is SendResult.Partial -> println("部分成功")
    }
}
```

### 依赖检查

```kotlin
// 检查单个发送器依赖
val dependencyResult = sender.checkDependencies()
if (!dependencyResult.satisfied) {
    println("缺失依赖: ${dependencyResult.missingDependencies}")
}

// 检查所有发送器依赖
val allResults = QRCodeSenderRegistry.checkAllDependencies()
```

### 生命周期管理

```kotlin
// 插件启动时
val sender = MyQRCodeSender()
QRCodeSenderRegistry.register(sender)
QRCodeSenderRegistry.activate(sender.id)

// 插件关闭时
QRCodeSenderRegistry.shutdown()
```

## 使用示例

### 实现自定义发送器

```kotlin
class MyQRCodeSender : QRCodeSender {
    override val id = "my_sender"
    override val name = "我的发送器"

    override fun isAvailable(): Boolean {
        return true // 检查发送器是否可用
    }

    override fun checkDependencies(): DependencyResult {
        // 检查依赖项
        return DependencyResult(
            satisfied = true,
            missingDependencies = emptyList(),
            missingSoftDependencies = emptyList(),
            details = emptyMap()
        )
    }

    override fun send(player: Player, content: String, options: SendOptions): SendResult {
        return try {
            // 实现发送逻辑
            SendResult.Success(id, System.currentTimeMillis())
        } catch (e: Exception) {
            SendResult.Failure(id, "发送失败", e)
        }
    }

    override fun sendAsync(player: Player, content: String, options: SendOptions, callback: Consumer<SendResult>) {
        // 异步实现
        CompletableFuture.supplyAsync {
            send(player, content, options)
        }.thenAccept(callback)
    }

    override fun initialize() {
        // 初始化资源
    }

    override fun shutdown() {
        // 清理资源
    }
}
```

### 注册和使用发送器

```kotlin
// 在插件启动时注册发送器
val sender = MyQRCodeSender()
if (QRCodeSenderRegistry.register(sender)) {
    if (QRCodeSenderRegistry.activate(sender.id)) {
        plugin.logger.info("二维码发送器注册并激活成功")
    } else {
        plugin.logger.warning("二维码发送器激活失败")
    }
} else {
    plugin.logger.warning("二维码发送器注册失败")
}

// 发送二维码
val activeSender = QRCodeSenderRegistry.getActiveSender()
if (activeSender != null) {
    val options = SendOptions(expireTime = 120000) // 2分钟过期
    val result = activeSender.send(player, "https://example.com", options)

    when (result) {
        is SendResult.Success -> player.sendMessage("二维码发送成功！")
        is SendResult.Failure -> player.sendMessage("二维码发送失败：${result.reason}")
        is SendResult.Partial -> player.sendMessage("二维码部分发送成功")
    }
}
```

## 架构优势

1. **高度可扩展**：通过接口和注册表模式，可以轻松添加新的发送器实现
2. **错误处理完善**：结构化的错误代码和异常机制，便于调试和用户反馈
3. **资源管理安全**：完整的生命周期管理，防止资源泄漏
4. **并发安全**：核心组件使用线程安全设计，支持多线程环境
5. **依赖感知**：自动检查和管理依赖项，提高系统稳定性