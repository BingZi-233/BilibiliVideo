# Session 包功能说明

## 概述

`session` 包是 BilibiliVideo 插件的会话管理核心模块，负责管理玩家的 Bilibili 登录会话状态。该包提供了完整的会话生命周期管理，包括会话创建、维护、过期检查和清理等功能。

## 主要功能

- **会话状态管理**：维护玩家的 Bilibili 登录状态和认证信息
- **Cookie 管理**：安全存储和管理 Bilibili API 所需的 Cookie 信息
- **会话过期控制**：24小时自动过期机制，确保安全性
- **性能优化**：内存缓存配合数据库持久化，提升访问性能
- **线程安全**：支持并发访问，确保多线程环境下的数据一致性

## 核心类和接口

### LoginSession 数据类

登录会话的数据实体，包含玩家和 Bilibili 账户的完整信息。

**主要属性：**
- `playerUuid: UUID` - Minecraft 玩家唯一标识
- `playerName: String` - Minecraft 玩家名称
- `mid: Long` - Bilibili 用户 MID
- `nickname: String` - Bilibili 用户昵称
- `sessdata: String` - SESSDATA Cookie 值
- `buvid3: String` - buvid3 Cookie 值（设备标识）
- `biliJct: String` - bili_jct Cookie 值（CSRF 令牌）
- `refreshToken: String` - 刷新令牌
- `loginTime: Long` - 登录时间戳
- `lastActiveTime: Long` - 最后活跃时间戳

**主要方法：**

#### `isExpired(): Boolean`
检查会话是否已过期（超过24小时未活跃）

#### `updateActiveTime()`
更新会话活跃时间，延长有效期

#### `isValid(): Boolean`
综合检查会话有效性（Cookie 完整性 + 未过期）

### SessionManager 会话管理器

单例对象，负责全局会话管理和生命周期控制。

**主要方法：**

#### `getSession(player: Player): LoginSession?`
获取玩家的登录会话，自动处理过期清理和活跃时间更新

#### `createSession(...): LoginSession`
创建新的登录会话
```kotlin
createSession(
    player: Player,
    mid: Long,
    nickname: String,
    sessdata: String,
    buvid3: String,
    biliJct: String,
    refreshToken: String
)
```

#### `removeSession(player: Player)`
移除玩家的登录会话

#### `isPlayerLoggedIn(player: Player): Boolean`
检查玩家是否已登录

#### `getPlayerMid(player: Player): Long?`
获取玩家的 Bilibili MID

#### `loadSessionFromDatabase(player: Player, callback: (LoginSession?) -> Unit)`
从数据库异步加载玩家的持久化会话信息

#### `clearExpiredSessions()`
清理所有过期的会话

#### `getActiveSessionCount(): Int`
获取当前活跃会话数量

## 使用示例

### 检查玩家登录状态
```kotlin
val player = event.player
if (SessionManager.isPlayerLoggedIn(player)) {
    val session = SessionManager.getSession(player)
    player.sendMessage("欢迎回来，${session?.nickname}！")
} else {
    player.sendMessage("请先登录 Bilibili 账户")
}
```

### 创建新会话
```kotlin
// 玩家完成 OAuth 登录后
val session = SessionManager.createSession(
    player = player,
    mid = 12345678L,
    nickname = "用户昵称",
    sessdata = "cookie_sessdata_value",
    buvid3 = "cookie_buvid3_value",
    biliJct = "cookie_bili_jct_value",
    refreshToken = "refresh_token_value"
)
```

### 从数据库加载会话
```kotlin
SessionManager.loadSessionFromDatabase(player) { session ->
    if (session != null) {
        player.sendMessage("会话已恢复：${session.nickname}")
    } else {
        player.sendMessage("未找到登录信息，请重新登录")
    }
}
```

### 获取玩家 MID
```kotlin
val mid = SessionManager.getPlayerMid(player)
if (mid != null) {
    // 调用 Bilibili API
    BilibiliApiClient.getUserInfo(mid)
}
```

## 设计特点

1. **安全性**：24小时自动过期机制，防止长期未使用的会话带来安全风险
2. **性能优化**：内存缓存优先，减少数据库访问次数
3. **线程安全**：使用 `ConcurrentHashMap` 确保并发安全
4. **异步处理**：数据库操作使用异步模式，避免阻塞主线程
5. **自动清理**：定期清理过期会话，节省内存资源

## 依赖关系

- 依赖 `DatabaseService` 进行会话持久化
- 集成 TabooLib 的异步任务系统
- 与 Bukkit Player API 紧密集成