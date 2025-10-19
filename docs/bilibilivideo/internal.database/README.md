# Database 包文档

## 包的主要用途和功能概述

`online.bingzi.bilibili.bilibilivideo.internal.database` 包是 BilibiliVideo 插件的数据持久化层，负责管理所有与数据库相关的操作。该包提供了完整的数据库抽象层，支持 MySQL 和 SQLite 双数据库，实现了 Bilibili 账户管理、玩家绑定、视频三连状态跟踪等核心功能的数据存储和访问。

### 主要功能特性：
- **双数据库支持**：自动适配 MySQL 和 SQLite 数据库
- **异步操作**：所有数据库操作均为异步执行，保证服务器性能
- **生命周期管理**：基于 TabooLib 生命周期自动初始化和清理
- **统一数据访问**：提供统一的 API 接口封装底层数据库操作
- **完善的实体映射**：使用 Kotlin data class 进行 ORM 映射

## 重要的类和接口

### 核心管理类

#### DatabaseManager
- **功能**：数据库生命周期管理器
- **职责**：
  - 插件启用时自动初始化数据库
  - 插件禁用时清理数据库资源
  - 提供数据库状态查询功能

#### DatabaseService
- **功能**：统一数据库服务层
- **职责**：
  - 提供异步数据库操作 API
  - 封装所有数据访问逻辑
  - 统一错误处理和日志记录

#### TableFactory
- **功能**：表工厂和管理类
- **职责**：
  - 创建和初始化所有数据表
  - 提供表实例访问方法
  - 管理数据源连接

### 数据实体类（Entity）

#### PlayerBinding
- **功能**：玩家-MID绑定实体
- **用途**：表示 Minecraft 玩家与 Bilibili MID 的一对一绑定关系

#### BilibiliAccount
- **功能**：Bilibili账户信息实体
- **用途**：存储用户完整认证信息，包括Cookie和刷新令牌

#### VideoTripleStatus
- **功能**：视频三连状态实体
- **用途**：记录玩家对特定视频的点赞、投币、收藏状态

#### UpFollowStatus
- **功能**：UP主关注状态实体
- **用途**：记录玩家对UP主的关注状态

#### VideoRewardRecord
- **功能**：视频奖励记录实体
- **用途**：记录玩家领取视频奖励的历史记录，防止重复领取

### 数据访问接口（DAO）

#### VideoRewardRecordDao
- **功能**：视频奖励记录数据访问接口
- **用途**：定义视频奖励记录的基本数据操作方法

### 数据表定义类（Table）

所有表定义类位于 `table/` 目录下，每个类负责创建对应的数据表结构：
- `BilibiliAccountTable` - Bilibili账户表
- `PlayerBindingTable` - 玩家绑定表
- `UpFollowStatusTable` - UP主关注状态表
- `VideoTripleStatusTable` - 视频三连状态表
- `VideoRewardRecordTable` - 视频奖励记录表

## 主要方法和功能点

### 数据库管理功能

#### 初始化和清理
```kotlin
// 数据库初始化
DatabaseManager.initialize()

// 检查初始化状态
val isInit = DatabaseManager.isInitialized()

// 获取数据库信息
val info = DatabaseManager.getDatabaseInfo()
```

### 玩家绑定管理

#### 绑定操作
```kotlin
// 绑定玩家与MID
DatabaseService.bindPlayer(playerUuid, mid, playerName) { success ->
    // 处理绑定结果
}

// 查询玩家绑定的MID
DatabaseService.getPlayerMid(playerUuid) { mid ->
    // 处理查询结果
}

// 根据MID查询玩家
DatabaseService.getPlayerByMid(mid) { playerUuid ->
    // 处理查询结果
}

// 解除绑定
DatabaseService.unbindPlayer(playerUuid) { success ->
    // 处理解绑结果
}
```

### Bilibili账户管理

#### 账户信息操作
```kotlin
// 保存Bilibili账户信息
DatabaseService.saveBilibiliAccount(
    mid, nickname, sessdata, buvid3,
    biliJct, refreshToken, playerName
) { success ->
    // 处理保存结果
}

// 获取账户信息
DatabaseService.getBilibiliAccount(mid) { account ->
    // 处理账户信息
}

// 更新Cookie信息
DatabaseService.updateCookies(
    mid, sessdata, buvid3, biliJct,
    refreshToken, playerName
) { success ->
    // 处理更新结果
}
```

### 视频三连状态管理

#### 三连状态操作
```kotlin
// 保存视频三连状态
DatabaseService.saveVideoTripleStatus(
    bvid, mid, playerUuid, isLiked,
    isCoined, isFavorited, playerName
) { success ->
    // 处理保存结果
}

// 获取视频三连状态
DatabaseService.getVideoTripleStatus(bvid, mid, playerUuid) { status ->
    // 处理状态信息
}
```

### UP主关注管理

#### 关注状态操作
```kotlin
// 保存UP主关注状态
DatabaseService.saveUpFollowStatus(
    upMid, followerMid, playerUuid,
    isFollowing, playerName
) { success ->
    // 处理保存结果
}

// 获取关注状态
DatabaseService.getUpFollowStatus(upMid, followerMid, playerUuid) { status ->
    // 处理状态信息
}
```

### 便捷查询方法

#### 状态检查
```kotlin
// 检查玩家是否已绑定
DatabaseService.isPlayerBound(playerUuid) { isBound ->
    // 处理检查结果
}

// 检查MID是否已被绑定
DatabaseService.isMidBound(mid) { isBound ->
    // 处理检查结果
}
```

## 使用示例或说明

### 基本使用流程

#### 1. 自动初始化
数据库在插件启用时自动初始化，无需手动调用：
```kotlin
// 插件启用时自动执行
@Awake(LifeCycle.ENABLE)
fun initialize() {
    // 自动创建所有数据表
    TableFactory.initializeTables()
}
```

#### 2. 玩家绑定流程
```kotlin
// 检查玩家是否已绑定
DatabaseService.isPlayerBound(playerUuid) { isBound ->
    if (!isBound) {
        // 执行绑定操作
        DatabaseService.bindPlayer(playerUuid, mid, playerName) { success ->
            if (success) {
                // 绑定成功，保存账户信息
                DatabaseService.saveBilibiliAccount(
                    mid, nickname, sessdata, buvid3,
                    biliJct, refreshToken, playerName
                ) { saved ->
                    // 处理保存结果
                }
            }
        }
    }
}
```

#### 3. 视频奖励记录管理
```kotlin
// 创建奖励记录实体
val rewardRecord = VideoRewardRecord(
    bvid = "BV1234567890",
    mid = 123456789L,
    playerUuid = playerUuid,
    rewardType = "specific",
    rewardData = """{"coins": 10, "items": ["diamond", "emerald"]}""",
    isLiked = true,
    isCoined = true,
    isFavorited = true,
    createPlayer = playerName,
    updatePlayer = playerName
)

// 检查奖励记录状态
val hasComplete = rewardRecord.hasCompleteTriple() // true
val statusDesc = rewardRecord.getTripleStatusDescription() // "点赞、投币、收藏"
```

### 错误处理

所有数据库操作都包含完善的错误处理机制：
```kotlin
DatabaseService.bindPlayer(playerUuid, mid, playerName) { success ->
    if (success) {
        // 操作成功
        player.sendMessage("绑定成功！")
    } else {
        // 操作失败，错误信息已记录到日志
        player.sendMessage("绑定失败，请检查参数或联系管理员")
    }
}
```

### 数据库兼容性

该包完全支持 MySQL 和 SQLite 双数据库：
- **生产环境**：建议使用 MySQL 获得更好的性能和并发支持
- **测试环境**：可使用 SQLite 简化部署和测试

表结构定义会根据数据库类型自动适配，开发者无需关心底层实现差异。