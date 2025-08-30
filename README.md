# BilibiliVideo

基于 TabooLib 框架开发的 Bukkit 插件，提供 Bilibili 相关功能集成。

## 项目概述

这是一个 Kotlin 开发的 Minecraft 服务器插件，使用 TabooLib 6.2.3 框架构建。插件支持多种 Bilibili API 集成功能，允许每个玩家登录自己的 Bilibili 账户并进行相关操作。

### 核心特性

- 🔐 **多玩家账户系统**: 每个玩家可以登录自己的 Bilibili 账户
- 🌐 **完整 API 支持**: 二维码登录、视频三连状态、UP主关注状态、Cookie 刷新等
- 🛡️ **风控机制**: 内置 buvid3 等风控规避机制
- 🚀 **异步处理**: 基于 TabooLib 任务调度系统的异步网络请求
- 🎯 **TabooLib 集成**: 完整的命令系统、国际化、任务调度等

## 构建说明

### 构建发行版本

发行版本用于正常使用，不含 TabooLib 本体：

```bash
./gradlew build
```

### 构建开发版本

开发版本包含 TabooLib 本体，用于开发者使用，但不可运行：

```bash
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 `-PDeleteCode` 表示移除所有逻辑代码以减少体积。

## 文档索引

### 📚 TabooLib 模块文档

#### 核心模块
- **[Basic 模块](docs/taboolib/Basic.md)** - 基础模块，提供核心功能和平台抽象
- **[Bukkit 模块](docs/taboolib/Bukkit.md)** - Bukkit 平台核心，事件监听和服务器 API 封装

#### 功能模块
- **[BukkitHook 模块](docs/taboolib/BukkitHook.md)** - Bukkit 插件集成，支持 PlaceholderAPI、Vault 等
- **[BukkitUtil 模块](docs/taboolib/BukkitUtil.md)** - Bukkit 实用工具，包含 ItemBuilder、库存管理等
- **[CommandHelper 模块](docs/taboolib/CommandHelper.md)** - 命令系统，支持声明式命令定义和自动补全
- **[I18n 模块](docs/taboolib/I18n.md)** - 国际化支持，多语言消息管理
- **[MinecraftChat 模块](docs/taboolib/MinecraftChat.md)** - 聊天消息处理，支持 JSON 格式和 RGB 颜色
- **[Kether 模块](docs/taboolib/Kether.md)** - 脚本引擎，动态脚本执行和配置

#### 数据和工具模块
- **[DatabasePlayer 模块](docs/taboolib/DatabasePlayer.md)** - 玩家数据库，数据持久化存储
- **[Metrics 模块](docs/taboolib/Metrics.md)** - 插件统计，集成 bStats 数据收集

### 🎬 Bilibili API 集成文档

#### 认证和登录
- **[二维码登录](docs/bilibili-API-collect/QrCodeLogin.md)** - 完整的二维码登录流程实现
  - 支持每个玩家独立登录
  - 包含玩家账户管理系统
  - 集成 TabooLib 命令系统

#### 内容交互
- **[视频三连状态获取](docs/bilibili-API-collect/VideoTripleStatus.md)** - 查询视频点赞、投币、收藏状态
  - **包含 buvid3 风控机制**
  - 支持批量查询和风控规避
  - 每个玩家使用自己的账户查询

- **[UP主关注状态获取](docs/bilibili-API-collect/UpFollowStatus.md)** - 查询 UP主关注状态
  - 支持简单和详细的关注状态查询
  - 包含互关、拉黑等状态识别
  - 集成 WBI 签名机制

#### 账户维护
- **[Cookie 刷新](docs/bilibili-API-collect/CookieRefresh.md)** - 自动刷新登录状态
  - RSA-OAEP 加密实现
  - 自动检查和刷新机制
  - 定时任务支持

## 技术栈

- **语言**: Kotlin (目标 JVM 1.8)
- **框架**: TabooLib 6.2.3
- **构建工具**: Gradle with Kotlin DSL
- **平台**: Bukkit/Spigot/Paper
- **网络请求**: OkHttp3
- **异步处理**: TabooLib 任务调度系统
- **数据序列化**: Gson

## 核心依赖

### TabooLib 模块
- Basic, BukkitHook, BukkitUtil, CommandHelper
- I18n, Metrics, MinecraftChat, Bukkit
- Kether, DatabasePlayer

### 外部依赖
- `online.bingzi:onebot:1.2.0` - OneBot 协议相关功能
- `ink.ptms.core:v12004` - mapped 和 universal 版本

### Maven 仓库
- Maven Central (默认)
- https://repo.aeoliancloud.com/repository/releases/

## 开发指南

### 环境要求
- JDK 1.8 或更高版本
- Kotlin 2.2.0
- Bukkit/Spigot/Paper 服务器

### 代码规范
- 使用 Kotlin 对象（object）作为主插件类
- 遵循 TabooLib 框架的约定
- UTF-8 编码，JVM target 1.8
- 使用 `-Xjvm-default=all` 编译参数

### 常用命令
```bash
# 清理构建文件
./gradlew clean

# 编译 Kotlin 代码  
./gradlew compileKotlin

# 运行测试
./gradlew test
```

## 许可证

本项目使用 MIT 许可证，详细信息请查看 LICENSE 文件。

## 贡献指南

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的修改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 联系方式

- 作者: BingZi-233
- 项目链接: [GitHub Repository](https://github.com/BingZi-233/BilibiliVideo)