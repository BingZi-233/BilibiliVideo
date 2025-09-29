# Bilibili命令包功能分析文档

## 概述

`online.bingzi.bilibili.bilibilivideo.internal.command`包是BilibiliVideo插件的命令系统核心，负责处理所有与Bilibili相关的玩家命令交互。该包采用TabooLib的声明式命令系统，提供了完整的Bilibili账户管理、视频状态查询和UP主关注状态查询功能。

## 包结构

```
command/
├── BilibiliCommand.kt                    # 主命令类，定义命令结构
└── handler/                             # 命令处理器目录
    ├── LoginCommandHandler.kt           # 登录命令处理器
    ├── LogoutCommandHandler.kt          # 登出命令处理器
    ├── TripleStatusCommandHandler.kt    # 视频三连状态查询处理器
    └── FollowStatusCommandHandler.kt    # UP主关注状态查询处理器
```

## 核心类和组件

### 1. BilibiliCommand (主命令类)

**功能**: 插件的主命令入口，使用TabooLib CommandHelper模块实现声明式命令系统

**主要特性**:
- 支持多个命令别名：`/bili`, `/bilibili`, `/bl`
- 完整的权限管理系统
- 智能参数验证和自动补全
- 支持管理员操作其他玩家

**命令结构**:
```kotlin
@CommandHeader(
    name = "bili",
    aliases = ["bilibili", "bl"],
    description = "Bilibili相关命令",
    permission = "bilibili.use",
    permissionDefault = PermissionDefault.TRUE
)
```

**支持的子命令**:
- `login [玩家]` - 登录Bilibili账户
- `logout [玩家]` - 登出Bilibili账户
- `triple <BV号> [玩家]` - 查询视频三连状态
- `follow <MID> [玩家]` - 查询UP主关注状态

### 2. LoginCommandHandler (登录处理器)

**功能**: 处理玩家Bilibili账户登录流程

**核心流程**:
1. 检查玩家登录状态和登录进程
2. 生成二维码并发送给玩家
3. 轮询二维码状态（扫码->确认->成功）
4. 保存账户信息并创建会话
5. 触发登录成功事件

**重要方法**:
- `handleLogin(player: Player)` - 主登录处理逻辑
- `startPolling(player: Player, qrcodeKey: String)` - 开始轮询二维码状态
- `pollQrCodeStatus(task: LoginTask)` - 轮询二维码状态
- `handleLoginSuccess()` - 处理登录成功逻辑

**登录任务管理**:
```kotlin
private data class LoginTask(
    val player: Player,
    val qrcodeKey: String,
    val startTime: Long = System.currentTimeMillis()
) {
    companion object {
        const val TIMEOUT_MILLIS = 3 * 60 * 1000L // 3分钟超时
    }
}
```

### 3. LogoutCommandHandler (登出处理器)

**功能**: 处理玩家Bilibili账户登出

**核心流程**:
1. 验证玩家登录状态
2. 触发登出事件
3. 清除会话缓存
4. 取消进行中的登录任务
5. 发送登出成功消息

**主要方法**:
- `handleLogout(player: Player)` - 处理登出逻辑

### 4. TripleStatusCommandHandler (三连状态查询处理器)

**功能**: 查询指定视频的三连状态（点赞、投币、收藏）

**核心流程**:
1. 验证玩家登录状态
2. 调用VideoApi查询三连状态
3. 保存状态数据到数据库
4. 向玩家展示查询结果
5. 触发三连状态检查事件

**主要方法**:
- `handleTripleStatus(player: Player, bvid: String)` - 处理三连状态查询

### 5. FollowStatusCommandHandler (关注状态查询处理器)

**功能**: 查询对指定UP主的关注状态

**核心流程**:
1. 验证玩家登录状态
2. 调用UserApi查询关注状态
3. 保存关注数据到数据库
4. 向玩家展示查询结果
5. 触发关注状态检查事件

**主要方法**:
- `handleFollowStatus(player: Player, mid: Long)` - 处理关注状态查询

## 权限系统

### 基础权限
- `bilibili.use` - 基础使用权限（默认为true）
- `bilibili.admin` - 管理员权限，允许操作其他玩家

### 权限检查逻辑
```kotlin
private fun resolveTargetPlayer(sender: ProxyCommandSender, playerName: String?): Player? {
    return if (playerName != null) {
        // 指定了玩家，需要admin权限
        if (!sender.hasPermission("bilibili.admin")) {
            sender.sendError("noPermissionForOthers")
            return null
        }
        // ... 获取目标玩家逻辑
    } else {
        // 未指定玩家，使用执行者
        // ... 验证执行者是玩家的逻辑
    }
}
```

## 参数验证

### BV号验证
```kotlin
private fun isValidBvid(bvid: String): Boolean {
    // BV号格式：BV + 10位大小写字母和数字（不包含0、I、O、l）
    return bvid.matches(Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$"))
}
```

### MID验证
```kotlin
private fun isValidMid(midStr: String): Boolean {
    val mid = midStr.toLongOrNull()
    return mid != null && mid > 0 && midStr.length >= 6 && midStr.length <= 12
}
```

## 使用示例

### 基本命令使用
```bash
# 玩家自己登录
/bili login

# 管理员为其他玩家登录
/bili login PlayerName

# 查询视频三连状态
/bili triple BV1234567890

# 查询UP主关注状态
/bili follow 123456789

# 登出账户
/bili logout
```

### 自动补全支持
- BV号自动补全：从配置文件中的BV号列表获取
- 玩家名自动补全：当前在线玩家列表
- 命令补全：支持所有子命令的Tab补全

## 事件系统集成

该命令包与插件的事件系统深度集成：

- **BilibiliLoginEvent** - 玩家登录成功时触发
- **BilibiliLogoutEvent** - 玩家登出时触发
- **VideoTripleStatusCheckEvent** - 视频三连状态查询完成时触发
- **UpFollowStatusCheckEvent** - UP主关注状态查询完成时触发

## 外部依赖

命令包依赖以下内部组件：
- `SessionManager` - 会话管理
- `DatabaseService` - 数据库服务
- `EventManager` - 事件管理
- `QrCodeApi`、`VideoApi`、`UserApi` - Bilibili API调用
- `QRCodeSenderRegistry` - 二维码发送器注册表
- `BvManager` - BV号管理器

## 设计特点

1. **命令处理器分离**: 每个命令功能独立成处理器，职责清晰
2. **异步处理**: 所有API调用都采用异步方式，避免阻塞主线程
3. **完整的错误处理**: 各种异常情况都有相应的错误提示
4. **权限管控**: 细粒度的权限控制，保证安全性
5. **事件驱动**: 与插件事件系统集成，支持扩展功能
6. **用户友好**: 丰富的提示信息和自动补全功能

该命令包为BilibiliVideo插件提供了完整、安全、用户友好的命令行交互界面，是插件核心功能的重要入口。