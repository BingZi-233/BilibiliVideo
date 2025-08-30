# BilibiliVideo API 开发文档

本文档详细说明了 BilibiliVideo 插件 API 包的架构、功能和使用方法。

## 概述

API 包位于 `online.bingzi.bilibili.bilibilivideo.api` 下，主要为开发者提供以下功能：

- **事件系统**：监听和处理 Bilibili 相关事件
- **二维码发送系统**：可扩展的二维码发送框架

## 包结构

```
api/
├── event/                          # 事件定义
│   ├── BilibiliLoginEvent.kt       # 登录事件
│   ├── BilibiliLogoutEvent.kt      # 登出事件
│   ├── UpFollowStatusCheckEvent.kt # UP主关注状态检查事件
│   └── VideoTripleStatusCheckEvent.kt # 视频三连状态检查事件
└── qrcode/                         # 二维码发送系统
    ├── exception/
    │   └── QRCodeSenderException.kt
    ├── metadata/
    │   ├── DependencyResult.kt
    │   └── DependencyStatus.kt
    ├── options/
    │   └── SendOptions.kt
    ├── registry/
    │   └── QRCodeSenderRegistry.kt
    ├── result/
    │   ├── ErrorCode.kt
    │   └── SendResult.kt
    └── sender/
        └── QRCodeSender.kt
```

## 事件系统

### 1. BilibiliLoginEvent

当玩家成功登录 Bilibili 账户时触发。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLoginEvent`

**构造参数**:
- `player: Player` - 登录的玩家
- `session: LoginSession` - 登录会话信息

**可用方法**:
```kotlin
fun getMid(): Long              // 获取用户MID
fun getNickname(): String       // 获取用户昵称  
fun getLoginTime(): Long        // 获取登录时间戳
```

**使用示例**:
```kotlin
@SubscribeEvent
fun onBilibiliLogin(event: BilibiliLoginEvent) {
    val player = event.player
    val mid = event.getMid()
    val nickname = event.getNickname()
    
    player.sendMessage("§a欢迎 ${nickname}(${mid}) 登录！")
}
```

### 2. BilibiliLogoutEvent

当玩家登出 Bilibili 账户时触发。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLogoutEvent`

**构造参数**:
- `player: Player` - 登出的玩家
- `previousSession: LoginSession` - 之前的登录会话

**可用方法**:
```kotlin
fun getMid(): Long                  // 获取用户MID
fun getNickname(): String           // 获取用户昵称
fun getSessionDuration(): Long      // 获取会话持续时间（毫秒）
```

### 3. UpFollowStatusCheckEvent

当检查UP主关注状态时触发。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.event.UpFollowStatusCheckEvent`

**构造参数**:
- `player: Player` - 执行检查的玩家
- `followData: UpFollowData` - 关注数据

**可用方法**:
```kotlin
fun getUpMid(): Long            // 获取UP主MID
fun getUpName(): String         // 获取UP主名称
fun getFollowerMid(): Long      // 获取粉丝MID
fun isFollowing(): Boolean      // 是否正在关注
```

### 4. VideoTripleStatusCheckEvent

当检查视频三连状态时触发。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.event.VideoTripleStatusCheckEvent`

**构造参数**:
- `player: Player` - 执行检查的玩家  
- `tripleData: VideoTripleData` - 三连数据

**可用方法**:
```kotlin
fun hasTripleAction(): Boolean  // 是否有三连操作
fun getBvid(): String          // 获取视频BVID
fun getMid(): Long             // 获取用户MID
fun isLiked(): Boolean         // 是否点赞
fun getCoinCount(): Int        // 投币数量
fun isFavorited(): Boolean     // 是否收藏
```

## 二维码发送系统

二维码发送系统是一个可扩展的框架，允许开发者实现不同的二维码发送方式（如聊天栏、GUI、Web等）。

### 核心接口

#### QRCodeSender

二维码发送器的基础接口。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.sender.QRCodeSender`

**接口定义**:
```kotlin
interface QRCodeSender {
    val id: String                      // 唯一标识符
    val name: String                    // 显示名称
    
    fun isAvailable(): Boolean          // 检查发送器是否可用
    fun checkDependencies(): DependencyResult   // 检查依赖
    
    // 同步发送
    fun send(player: Player, content: String, options: SendOptions): SendResult
    
    // 异步发送  
    fun sendAsync(player: Player, content: String, options: SendOptions, callback: (SendResult) -> Unit)
    
    fun initialize()                    // 初始化资源
    fun shutdown()                      // 清理资源
}
```

### 注册中心

#### QRCodeSenderRegistry

管理所有二维码发送器的注册、激活和生命周期。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.registry.QRCodeSenderRegistry`

**主要方法**:
```kotlin
// 注册发送器
fun register(sender: QRCodeSender): Boolean

// 注销发送器
fun unregister(senderId: String): Boolean

// 激活发送器
fun activate(senderId: String): Boolean

// 获取当前激活的发送器
fun getActiveSender(): QRCodeSender?

// 获取所有可用的发送器
fun getAvailableSenders(): Map<String, QRCodeSender>

// 检查所有发送器的依赖状态
fun checkAllDependencies(): Map<String, DependencyResult>

// 关闭所有发送器
fun shutdown()
```

### 数据类

#### SendOptions

发送选项配置。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.options.SendOptions`

```kotlin
data class SendOptions(
    val expireTime: Long = 60000,                       // 过期时间（毫秒）
    val retryCount: Int = 0,                            // 重试次数
    val customData: Map<String, Any> = emptyMap()       // 自定义参数
)
```

#### SendResult

发送结果（密封类）。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.result.SendResult`

```kotlin
sealed class SendResult {
    // 成功
    data class Success(
        val senderId: String,
        val timestamp: Long,
        val metadata: Map<String, Any> = emptyMap()
    ) : SendResult()
    
    // 失败  
    data class Failure(
        val senderId: String,
        val reason: String,
        val exception: Exception? = null,
        val canRetry: Boolean = false
    ) : SendResult()
    
    // 部分成功
    data class Partial(
        val senderId: String,
        val successCount: Int,
        val failureCount: Int,
        val details: String
    ) : SendResult()
}
```

#### DependencyResult

依赖检查结果。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyResult`

```kotlin
data class DependencyResult(
    val satisfied: Boolean,                         // 依赖是否满足
    val missingDependencies: List<String>,          // 缺失的必需依赖
    val missingSoftDependencies: List<String>,      // 缺失的可选依赖
    val details: Map<String, DependencyStatus>      // 每个依赖的详细状态
)
```

#### DependencyStatus

依赖状态枚举。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyStatus`

```kotlin
enum class DependencyStatus {
    PRESENT,            // 依赖存在且可用
    MISSING,            // 依赖缺失  
    VERSION_MISMATCH,   // 版本不匹配
    DISABLED            // 依赖被禁用
}
```

#### ErrorCode

错误代码枚举。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.result.ErrorCode`

```kotlin
enum class ErrorCode {
    DEPENDENCY_MISSING,         // 依赖缺失
    SENDER_NOT_AVAILABLE,       // 发送器不可用
    INITIALIZATION_FAILED,      // 初始化失败
    SEND_FAILED,               // 发送失败
    INVALID_OPTIONS,           // 选项无效
    PLAYER_OFFLINE             // 玩家离线
}
```

#### QRCodeSenderException

二维码发送异常。

**包路径**: `online.bingzi.bilibili.bilibilivideo.api.qrcode.exception.QRCodeSenderException`

```kotlin
class QRCodeSenderException(
    message: String,                    // 异常消息
    cause: Throwable? = null,           // 异常原因
    val errorCode: ErrorCode            // 错误代码
) : Exception(message, cause)
```

## 开发指南

### 实现自定义二维码发送器

1. **实现 QRCodeSender 接口**:

```kotlin
class ChatQRCodeSender : QRCodeSender {
    override val id = "chat"
    override val name = "聊天栏二维码发送器"
    
    override fun isAvailable(): Boolean = true
    
    override fun checkDependencies(): DependencyResult {
        return DependencyResult(
            satisfied = true,
            missingDependencies = emptyList(),
            missingSoftDependencies = emptyList(),
            details = emptyMap()
        )
    }
    
    override fun send(player: Player, content: String, options: SendOptions): SendResult {
        return try {
            player.sendMessage(content)
            SendResult.Success(id, System.currentTimeMillis())
        } catch (e: Exception) {
            SendResult.Failure(id, "发送失败: ${e.message}", e)
        }
    }
    
    override fun sendAsync(player: Player, content: String, options: SendOptions, callback: (SendResult) -> Unit) {
        // 使用 TabooLib 异步任务
        submitAsync {
            val result = send(player, content, options)
            callback(result)
        }
    }
    
    override fun initialize() {
        // 初始化逻辑
    }
    
    override fun shutdown() {
        // 清理逻辑  
    }
}
```

2. **注册发送器**:

```kotlin
@Awake(LifeCycle.ENABLE)
object MyPlugin : Plugin() {
    fun registerSenders() {
        val chatSender = ChatQRCodeSender()
        QRCodeSenderRegistry.register(chatSender)
        QRCodeSenderRegistry.activate("chat")
    }
}
```

### 监听事件

```kotlin
@SubscribeEvent
fun onLogin(event: BilibiliLoginEvent) {
    val player = event.player
    val nickname = event.getNickname()
    
    player.sendMessage("§a${nickname} 登录成功！")
}

@SubscribeEvent  
fun onVideoCheck(event: VideoTripleStatusCheckEvent) {
    if (event.hasTripleAction()) {
        val player = event.player
        player.sendMessage("§a检测到视频三连操作！")
    }
}
```

## 最佳实践

1. **事件处理**：
   - 事件监听方法应该尽可能快速执行
   - 避免在事件处理中执行耗时操作
   - 使用异步任务处理复杂逻辑

2. **二维码发送器**：
   - 实现前检查依赖是否满足
   - 正确处理初始化和清理资源
   - 提供有意义的错误信息
   - 支持异步操作以避免阻塞主线程

3. **异常处理**：
   - 使用 `QRCodeSenderException` 抛出特定错误
   - 在 `SendResult.Failure` 中提供详细的错误信息
   - 正确设置 `canRetry` 标志

4. **生命周期管理**：
   - 在插件启用时注册发送器
   - 在插件禁用时清理资源
   - 使用 `QRCodeSenderRegistry.shutdown()` 进行统一清理

## 注意事项

1. 所有事件都继承自 `BukkitProxyEvent`，遵循 TabooLib 事件系统规范
2. 二维码发送系统设计为可扩展架构，支持多种发送方式
3. 异步操作应使用 TabooLib 的任务调度系统
4. 依赖检查机制确保发送器在合适的环境下工作
5. 注册中心提供统一的生命周期管理