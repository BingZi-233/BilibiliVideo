# Bilibili 事件 API (Event API)

## 包功能概述

`online.bingzi.bilibili.bilibilivideo.api.event` 包提供了 BilibiliVideo 插件的核心事件系统，允许其他插件或系统监听和处理与 Bilibili 相关的用户行为事件。该包基于 TabooLib 的事件系统构建，提供了四个主要的事件类型，涵盖用户登录/登出和 Bilibili 内容交互检查。

## 重要的类和接口

### 1. BilibiliLoginEvent
**用途**: 当玩家成功登录 Bilibili 账户时触发
- **构造参数**:
  - `player: Player` - 登录的玩家
  - `session: LoginSession` - 登录会话信息
- **继承**: `BukkitProxyEvent()`

### 2. BilibiliLogoutEvent
**用途**: 当玩家退出登录 Bilibili 账户时触发
- **构造参数**:
  - `player: Player` - 登出的玩家
  - `previousSession: LoginSession` - 之前的登录会话信息
- **继承**: `BukkitProxyEvent()`

### 3. UpFollowStatusCheckEvent
**用途**: 当检查玩家对某个 UP 主的关注状态时触发
- **构造参数**:
  - `player: Player` - 执行检查的玩家
  - `followData: UpFollowData` - UP 主关注数据
- **继承**: `BukkitProxyEvent()`

### 4. VideoTripleStatusCheckEvent
**用途**: 当检查玩家对某个视频的三连状态（点赞、投币、收藏）时触发
- **构造参数**:
  - `player: Player` - 执行检查的玩家
  - `tripleData: VideoTripleData` - 视频三连数据
- **继承**: `BukkitProxyEvent()`

## 主要方法和功能点

### BilibiliLoginEvent 方法
- `getMid(): Long` - 获取用户 MID
- `getNickname(): String` - 获取用户昵称
- `getLoginTime(): Long` - 获取登录时间戳（毫秒）

### BilibiliLogoutEvent 方法
- `getMid(): Long` - 获取已登出的用户 MID
- `getNickname(): String` - 获取已登出的用户昵称
- `getSessionDuration(): Long` - 获取会话持续时间（毫秒）

### UpFollowStatusCheckEvent 方法
- `getUpMid(): Long` - 获取 UP 主 MID
- `getUpName(): String` - 获取 UP 主名称
- `getFollowerMid(): Long` - 获取关注者 MID
- `isFollowing(): Boolean` - 检查是否已关注该 UP 主

### VideoTripleStatusCheckEvent 方法
- `hasTripleAction(): Boolean` - 检查是否有任何三连操作
- `getBvid(): String` - 获取视频 BV 号
- `getMid(): Long` - 获取用户 MID
- `isLiked(): Boolean` - 检查是否已点赞
- `getCoinCount(): Int` - 获取投币数量（0-2）
- `isFavorited(): Boolean` - 检查是否已收藏

## 使用示例

### 监听用户登录事件
```kotlin
@EventHandler
fun onBilibiliLogin(event: BilibiliLoginEvent) {
    val player = event.player
    val mid = event.getMid()
    val nickname = event.getNickname()

    player.sendMessage("欢迎 $nickname (MID: $mid) 登录 Bilibili!")

    // 执行登录后的逻辑，例如数据同步、权限更新等
}
```

### 监听用户登出事件
```kotlin
@EventHandler
fun onBilibiliLogout(event: BilibiliLogoutEvent) {
    val player = event.player
    val sessionDuration = event.getSessionDuration()
    val minutes = sessionDuration / (1000 * 60)

    player.sendMessage("您已登出 Bilibili，本次会话时长: ${minutes} 分钟")

    // 清理缓存、保存数据等
}
```

### 监听关注状态检查事件
```kotlin
@EventHandler
fun onUpFollowCheck(event: UpFollowStatusCheckEvent) {
    val player = event.player
    val upName = event.getUpName()
    val isFollowing = event.isFollowing()

    if (isFollowing) {
        player.sendMessage("检测到您已关注 UP 主: $upName")
        // 发放关注奖励
        rewardManager.giveFollowReward(player, event.getUpMid())
    } else {
        player.sendMessage("您尚未关注 UP 主: $upName")
    }
}
```

### 监听视频三连检查事件
```kotlin
@EventHandler
fun onVideoTripleCheck(event: VideoTripleStatusCheckEvent) {
    val player = event.player
    val bvid = event.getBvid()

    if (event.hasTripleAction()) {
        val actions = mutableListOf<String>()
        if (event.isLiked()) actions.add("点赞")
        if (event.getCoinCount() > 0) actions.add("投币${event.getCoinCount()}个")
        if (event.isFavorited()) actions.add("收藏")

        val actionText = actions.joinToString("、")
        player.sendMessage("检测到您对视频 $bvid 进行了: $actionText")

        // 根据三连情况发放不同奖励
        rewardManager.giveTripleReward(player, event.tripleData)
    }
}
```

## 技术特性

- **基于 TabooLib**: 所有事件都继承自 `BukkitProxyEvent`，兼容 Bukkit/Spigot 生态
- **类型安全**: 使用 Kotlin 的强类型系统，提供编译时安全保证
- **完整文档**: 所有类和方法都有完整的 KDoc 文档注释
- **版本标记**: 使用 `@since 1.0.0` 标记 API 版本
- **扩展性**: 事件系统便于其他插件集成和扩展功能

## 集成说明

要监听这些事件，您需要:

1. 添加 BilibiliVideo 插件作为依赖
2. 在您的插件中注册事件监听器
3. 使用 `@EventHandler` 注解标记监听方法
4. 确保您的插件在 BilibiliVideo 之后加载（在 plugin.yml 中设置 depend 或 softdepend）

该事件 API 为 BilibiliVideo 插件生态系统提供了强大的扩展能力，允许开发者构建丰富的 Bilibili 集成功能。