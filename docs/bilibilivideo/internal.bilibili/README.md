# Bilibili包功能说明文档

## 概述

该包提供了完整的Bilibili API集成功能，专为Minecraft插件设计。支持二维码登录、用户认证、视频状态查询、用户信息获取等核心功能。所有网络请求均采用异步设计，确保不会阻塞Minecraft服务器主线程。

## 核心功能

### 🔐 用户认证系统
- 二维码登录功能，无需用户输入密码
- 自动Cookie管理和会话维护
- 登录状态实时轮询

### 📺 视频交互功能
- 视频三连状态查询（点赞、投币、收藏）
- BV号与AV号自动转换
- 并发查询优化，提高响应速度

### 👤 用户信息管理
- 用户基本信息查询
- UP主关注状态检查
- MID格式验证

### 🌐 HTTP客户端
- 专业的HTTP客户端工厂
- 自动Cookie管理
- 请求头标准化
- 连接池复用

## 包结构

```
online.bingzi.bilibili.bilibilivideo.internal.bilibili/
├── api/                    # API接口层
│   ├── VideoApi.kt        # 视频相关API
│   ├── UserApi.kt         # 用户相关API
│   └── QrCodeApi.kt       # 二维码登录API
├── client/                # HTTP客户端
│   └── HttpClientFactory.kt
├── helper/                # 辅助工具
│   └── CookieHelper.kt    # Cookie处理工具
└── model/                 # 数据模型
    ├── VideoTripleData.kt # 视频三连数据模型
    ├── UpFollowData.kt    # 用户关注数据模型
    └── QrCodeData.kt      # 二维码数据模型
```

## 重要类和接口

### API接口层

#### VideoApi
视频API工具类，提供视频相关功能。

**主要方法：**
```kotlin
// 获取视频三连状态（点赞、投币、收藏）
fun getTripleStatus(
    bvid: String,           // 视频BV号
    sessdata: String,       // 用户认证Cookie
    buvid3: String,         // 设备标识Cookie
    callback: (VideoTripleData?) -> Unit  // 结果回调
)
```

**核心特性：**
- 自动BV号到AV号转换
- 三个API并发查询，提高效率
- 完整的错误处理机制

#### UserApi
用户API工具类，提供用户信息查询功能。

**主要方法：**
```kotlin
// 获取UP主关注状态
fun getFollowStatus(
    upMid: Long,
    sessdata: String,
    callback: (UpFollowData?) -> Unit
)

// 获取用户基本信息
fun getUserBasicInfo(
    mid: Long,
    sessdata: String?,
    callback: (UserCardData?) -> Unit
)

// MID格式验证
fun isValidMid(mid: Long): Boolean
```

#### QrCodeApi
二维码登录API工具类，提供完整的登录流程。

**主要方法：**
```kotlin
// 生成登录二维码
fun generateQrCode(callback: (QrCodeData?) -> Unit)

// 轮询二维码登录状态
fun pollQrCodeStatus(
    qrcodeKey: String,
    callback: (LoginStatus, QrCodePollData?, LoginInfo?) -> Unit
)

// 提取登录信息
fun extractLoginInfo(
    pollData: QrCodePollData,
    cookies: List<String>
): LoginInfo?
```

**登录信息类：**
```kotlin
data class LoginInfo(
    val mid: Long,           // 用户MID
    val sessdata: String,    // 会话认证Cookie
    val buvid3: String,      // 设备标识Cookie
    val biliJct: String,     // CSRF保护令牌
    val refreshToken: String // 刷新令牌
)
```

### HTTP客户端工厂

#### HttpClientFactory
HTTP客户端管理工具，提供标准化的网络请求功能。

**主要特性：**
- 自动Cookie管理
- 标准HTTP请求头设置
- 连接超时和读取超时配置
- 自定义Cookie注入支持

**主要方法：**
```kotlin
// 获取默认HTTP客户端
val httpClient: OkHttpClient

// 创建带认证Cookie的客户端
fun createCustomClient(
    sessdata: String? = null,
    buvid3: String? = null,
    biliJct: String? = null
): OkHttpClient
```

### 辅助工具

#### CookieHelper
Cookie处理辅助工具。

**主要方法：**
```kotlin
// 提取特定Cookie值
fun extractSessdata(cookies: List<Cookie>): String?
fun extractBuvid3(cookies: List<Cookie>): String?
fun extractBiliJct(cookies: List<Cookie>): String?

// Cookie格式验证
fun isCookieValid(sessdata: String?): Boolean

// Cookie字符串操作
fun createCookieString(...): String
fun parseCookieString(cookieString: String): Map<String, String>
```

## 数据模型

### VideoTripleData
视频三连状态数据类。

```kotlin
data class VideoTripleData(
    val bvid: String,         // 视频BV号
    val playerUuid: String,   // 玩家UUID
    val mid: Long,           // 用户MID
    val isLiked: Boolean,    // 是否点赞
    val coinCount: Int,      // 投币数量
    val isFavorited: Boolean, // 是否收藏
    val timestamp: Long      // 时间戳
) {
    // 检查是否完成三连
    fun hasTripleAction(): Boolean

    // 获取状态描述
    fun getStatusMessage(): String
}
```

### UpFollowData
UP主关注状态数据类。

```kotlin
data class UpFollowData(
    val upMid: Long,          // UP主MID
    val upName: String,       // UP主昵称
    val followerMid: Long,    // 关注者MID
    val playerUuid: String,   // 玩家UUID
    val isFollowing: Boolean, // 关注状态
    val timestamp: Long       // 时间戳
) {
    // 获取关注状态描述
    fun getStatusMessage(): String
}
```

### QrCodeData与LoginStatus
二维码登录相关数据模型。

```kotlin
// 二维码数据
data class QrCodeData(
    val url: String,          // 二维码图片URL
    val qrcodeKey: String     // 轮询密钥
)

// 登录状态枚举
enum class LoginStatus(val code: Int) {
    SUCCESS(0),               // 登录成功
    NOT_SCANNED(86101),      // 未扫描
    SCANNED_WAITING(86090),  // 已扫描待确认
    EXPIRED(86038)           // 已过期
}
```

## 使用示例

### 1. 二维码登录流程

```kotlin
// 步骤1：生成二维码
QrCodeApi.generateQrCode { qrCodeData ->
    if (qrCodeData != null) {
        // 显示二维码给用户扫描
        showQrCode(qrCodeData.url)

        // 步骤2：轮询登录状态
        pollLogin(qrCodeData.qrcodeKey)
    }
}

fun pollLogin(qrcodeKey: String) {
    QrCodeApi.pollQrCodeStatus(qrcodeKey) { status, data, loginInfo ->
        when (status) {
            LoginStatus.SUCCESS -> {
                // 登录成功，保存认证信息
                loginInfo?.let { saveLoginInfo(it) }
            }
            LoginStatus.NOT_SCANNED -> {
                // 继续等待扫描
                scheduleNextPoll(qrcodeKey, 3000)
            }
            LoginStatus.SCANNED_WAITING -> {
                // 提示用户确认登录
                showMessage("请在手机上确认登录")
                scheduleNextPoll(qrcodeKey, 1000)
            }
            LoginStatus.EXPIRED -> {
                // 二维码过期，重新生成
                showMessage("二维码已过期，请重新获取")
            }
        }
    }
}
```

### 2. 查询视频三连状态

```kotlin
VideoApi.getTripleStatus(
    bvid = "BV1xx411c7mD",
    sessdata = playerLoginInfo.sessdata,
    buvid3 = playerLoginInfo.buvid3
) { tripleData ->
    if (tripleData != null) {
        val message = tripleData.getStatusMessage()
        player.sendMessage("视频状态：$message")

        // 检查是否完成三连
        if (tripleData.hasTripleAction()) {
            giveReward(player, "triple_reward")
        }
    } else {
        player.sendMessage("查询失败，请检查登录状态")
    }
}
```

### 3. 查询UP主关注状态

```kotlin
UserApi.getFollowStatus(
    upMid = 12345678L,
    sessdata = playerLoginInfo.sessdata
) { followData ->
    if (followData != null) {
        val status = followData.getStatusMessage()
        player.sendMessage("${followData.upName} $status")

        if (followData.isFollowing) {
            giveReward(player, "follow_reward")
        }
    }
}
```

### 4. 获取用户基本信息

```kotlin
UserApi.getUserBasicInfo(
    mid = 12345678L,
    sessdata = null  // 可选参数，不需要登录
) { userData ->
    userData?.let {
        player.sendMessage("""
            用户信息：
            昵称：${it.name}
            等级：${it.level_info?.current_level ?: "未知"}
            粉丝数：${it.fans}
            个性签名：${it.sign}
        """.trimIndent())
    }
}
```

## 技术特点

### 异步设计
- 所有API调用都使用`submitAsync`异步执行
- 通过回调函数返回结果，不阻塞主线程
- 支持并发查询，提高性能

### 安全认证
- 标准Cookie认证流程
- 自动CSRF保护
- 设备指纹验证

### 错误处理
- 完善的异常捕获机制
- 网络请求失败自动重试逻辑
- 用户友好的错误提示

### 可扩展性
- 模块化设计，易于扩展新功能
- 标准化的HTTP客户端工厂
- 灵活的数据模型设计

## 注意事项

1. **认证要求**：大部分功能需要用户登录后的Cookie信息
2. **频率限制**：请注意Bilibili API的调用频率限制
3. **异步处理**：所有API调用都是异步的，需要通过回调处理结果
4. **Cookie管理**：系统自动管理Cookie，无需手动处理
5. **错误处理**：务必检查回调参数是否为null，做好异常处理

该包为Minecraft插件提供了完整的Bilibili集成能力，支持用户认证、视频交互、用户信息查询等核心功能，所有设计都考虑了Minecraft服务器环境的特殊需求。