# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个名为 BilibiliVideo 的 Kotlin 项目，基于 TabooLib 框架开发的 Bukkit 插件。项目使用 Gradle 构建系统，目标 JVM 版本为 1.8。

## 构建和开发命令

### 构建发行版本
用于正常使用，不含 TabooLib 本体：
```
./gradlew build
```

### 构建开发版本
包含 TabooLib 本体，用于开发者使用但不可运行：
```
./gradlew taboolibBuildApi -PDeleteCode
```
参数 `-PDeleteCode` 表示移除所有逻辑代码以减少体积。

### 其他常用命令
```
./gradlew clean          # 清理构建文件
./gradlew compileKotlin  # 编译 Kotlin 代码
```

## 项目架构

### 技术栈
- **语言**: Kotlin (目标 JVM 1.8)
- **框架**: TabooLib 6.2.3 (Bukkit 插件开发框架)
- **构建工具**: Gradle with Kotlin DSL
- **平台**: Bukkit/Spigot/Paper

### 核心依赖
- TabooLib 各模块: Basic, BukkitHook, BukkitUtil, CommandHelper, I18n, Metrics, MinecraftChat, Bukkit, Kether, DatabasePlayer
- online.bingzi:onebot:1.2.0 - OneBot 协议相关功能
- ink.ptms.core (v12004) - mapped 和 universal 版本

### Maven 仓库
- Maven Central (默认)
- https://repo.aeoliancloud.com/repository/releases/ - 用于获取 OneBot 依赖

### 项目结构
```
src/main/kotlin/online/bingzi/bilibili/bilibilivideo/
├── BilibiliVideo.kt  # 主插件类，继承 TabooLib Plugin
```

主插件类使用 TabooLib 的 `Plugin` 对象模式，在 `onEnable()` 方法中输出启动信息。

### 代码规范
- 使用 Kotlin 对象（object）作为主插件类
- 遵循 TabooLib 框架的约定
- UTF-8 编码
- JVM target 1.8，使用 `-Xjvm-default=all` 编译参数

## 文档索引和问题解决指南

当遇到开发问题时，可以根据以下索引快速定位相关文档：

### TabooLib 模块相关问题

#### 基础和核心问题
- **插件启动/生命周期问题** → [Basic 模块](docs/Basic.md) - 插件对象模式、日志输出、生命周期管理
- **事件监听/服务器 API 问题** → [Bukkit 模块](docs/Bukkit.md) - 事件系统、任务调度、跨版本兼容

#### 功能集成问题  
- **其他插件集成/PlaceholderAPI/Vault** → [BukkitHook 模块](docs/BukkitHook.md) - 插件钩子、依赖管理
- **物品构建/库存操作/玩家工具** → [BukkitUtil 模块](docs/BukkitUtil.md) - ItemBuilder、库存管理、位置操作
- **命令系统/参数解析/补全** → [CommandHelper 模块](docs/CommandHelper.md) - 声明式命令、多级子命令、权限检查

#### 用户体验问题
- **配置文件管理/@Config注解/ConfigNodeTransfer** → [Configuration 模块](docs/Configuration.md) - 配置文件加载、代理映射、节点转换、自动监听
- **多语言/消息发送/国际化** → [I18n 模块](docs/I18n.md) - sendInfo/sendWarn/sendError、语言文件管理
- **聊天消息/RGB颜色/JSON格式** → [MinecraftChat 模块](docs/MinecraftChat.md) - Component 消息、悬停点击事件
- **脚本执行/动态配置** → [Kether 模块](docs/Kether.md) - 脚本引擎、条件判断、变量函数

#### 数据和监控问题
- **数据库操作/SQL查询/事务管理** → [Database 模块](docs/Database.md) - MySQL/SQLite支持、数据表创建、增删改查操作
- **玩家数据存储/数据库** → [DatabasePlayer 模块](docs/DatabasePlayer.md) - 数据持久化、多数据库支持
- **插件统计/bStats集成** → [Metrics 模块](docs/Metrics.md) - 使用统计、自定义图表

### Bilibili API 集成问题

#### 用户认证问题
- **玩家登录/二维码登录** → [二维码登录](docs/bilibili-API-collect/QrCodeLogin.md) - 多玩家账户系统、登录流程
- **Cookie过期/自动刷新** → [Cookie 刷新](docs/bilibili-API-collect/CookieRefresh.md) - RSA加密、刷新机制、定时任务

#### 内容交互问题
- **视频点赞投币收藏状态** → [视频三连状态](docs/bilibili-API-collect/VideoTripleStatus.md) - 状态查询、风控规避、批量处理
- **UP主关注状态/粉丝关系** → [UP主关注状态](docs/bilibili-API-collect/UpFollowStatus.md) - WBI签名、关系查询、互关检测

#### 常见技术问题解决路径

**问题：请求被风控/403/412错误**
→ 查看 [视频三连状态](docs/bilibili-API-collect/VideoTripleStatus.md) 或 [UP主关注状态](docs/bilibili-API-collect/UpFollowStatus.md)
→ 重点关注 buvid3 Cookie 设置和请求头配置

**问题：多玩家账户管理**  
→ 查看任意 bilibili-API-collect 文档中的 `PlayerBilibiliAccountManager` 实现
→ 重点关注账户隔离和数据持久化

**问题：命令系统集成**
→ 查看 [CommandHelper 模块](docs/CommandHelper.md) 了解 TabooLib 命令系统
→ 查看各 API 文档中的 TabooLib 命令实现示例

**问题：国际化消息**
→ 查看 [I18n 模块](docs/I18n.md) 了解 sendInfo/sendWarn/sendError 用法
→ 注意语言文件使用扁平结构和小驼峰命名，颜色代码使用 & 而非 §

**问题：异步网络请求**
→ 查看任意 bilibili-API-collect 文档了解 OkHttp3 + TabooLib 异步处理模式
→ 使用 TabooLib 的任务调度系统，避免使用 Kotlin Coroutines
→ 重点关注 TabooLib 异步任务和错误处理机制

## ⚠️ 重要技术说明

**异步处理方式**：
- 项目使用 TabooLib 的任务调度系统而非 Kotlin Coroutines
- 现有 `docs/bilibili-API-collect/` 中的文档使用了 Kotlin Coroutines 示例代码
- 实际开发时需要将 `suspend fun`、`withContext(Dispatchers.IO)`、`launch` 等协程代码替换为 TabooLib 的异步处理方式
- 参考 [Bukkit 模块](docs/Bukkit.md) 中的 `submitTask` 异步任务示例