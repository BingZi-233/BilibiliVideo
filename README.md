<div align="center">

# 🎬 BilibiliVideo

**一个让 Minecraft 与哔哩哔哩无缝对接的全功能插件**

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-purple.svg)](https://kotlinlang.org)
[![TabooLib](https://img.shields.io/badge/TabooLib-6.2.4-green.svg)](https://taboolib.org)
[![Version](https://img.shields.io/badge/version-2.0.0--beta-orange.svg)](https://github.com/BingZi-233/BilibiliVideo)

*让玩家在游戏内直接完成 B 站账号绑定、三连检测、奖励领取等操作*

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [命令系统](#-命令系统) • [配置指南](#%EF%B8%8F-配置指南) • [开发文档](#-开发文档)

</div>

---

## 📖 项目简介

**BilibiliVideo** 是一个基于 TabooLib 6.2.4 框架开发的 Minecraft 服务器插件，为服务器提供完整的哔哩哔哩平台集成能力。每个玩家可以独立登录自己的 B 站账号，在游戏内完成视频三连检测、领取奖励等互动功能。

### 🎯 核心亮点

- 🗺️ **游戏内二维码登录** - 创新性地使用地图物品展示 B 站登录二维码
- 👥 **多账户隔离系统** - 每位玩家使用独立的 B 站账号，数据完全隔离
- ⚡ **异步架构** - 所有网络请求异步执行，不影响服务器性能
- 🎁 **灵活奖励系统** - 基于 Kether 脚本引擎，支持自定义奖励逻辑
- 🛡️ **风控规避机制** - 内置 buvid3 参数管理，保证请求稳定性
- 🗄️ **双数据库支持** - SQLite 开箱即用，可切换至 MySQL

---

## ✨ 功能特性

### 🔐 账号系统

| 功能 | 说明 |
|------|------|
| **二维码登录** | 在游戏内生成 B 站登录二维码地图物品，扫码即可绑定 |
| **账号绑定** | 每个玩家绑定独立的 B 站账号，支持查看绑定状态 |
| **Cookie 自动刷新** | 自动维护登录凭证有效性，避免过期失效 |
| **凭证管理** | 完整的凭证生命周期管理（创建、更新、过期、禁用） |

### 🎬 互动功能

| 功能 | 说明 |
|------|------|
| **三连状态检测** | 检测玩家是否完成视频的点赞、投币、收藏 |
| **UP 主关注查询** | 查询玩家对指定 UP 主的关注状态（已有文档支持） |
| **奖励发放** | 基于三连状态自动发放游戏内奖励 |
| **防重复领取** | 智能记录奖励领取历史，避免重复领取 |

### 🏗️ 技术架构

```
┌─────────────────────────────────────────┐
│           命令层 (Command)               │  玩家交互入口
├─────────────────────────────────────────┤
│          服务层 (Service)                │  业务逻辑处理
│  ├─ BindingService   (账号绑定)         │
│  ├─ TripleCheckService (三连检测)       │
│  ├─ RewardService    (奖励发放)         │
│  └─ CredentialService (凭证管理)        │
├─────────────────────────────────────────┤
│         仓库层 (Repository)              │  数据访问层
│  ├─ BoundAccountRepository              │
│  ├─ TripleStatusRepository              │
│  ├─ RewardRecordRepository              │
│  └─ CredentialRepository                │
├─────────────────────────────────────────┤
│      数据库层 (Database ORM)             │  Ktorm + HikariCP
│  ├─ SQLite (默认)                       │
│  └─ MySQL (可选)                        │
├─────────────────────────────────────────┤
│       外部 API 层 (Bilibili)             │  网络请求封装
│  ├─ QrLoginService   (二维码登录)       │
│  ├─ TripleActionApi  (三连检测)         │
│  └─ BilibiliHttpClient (HTTP 客户端)    │
└─────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 📋 环境要求

| 组件 | 版本要求 |
|------|----------|
| **Minecraft 服务端** | Bukkit/Spigot/Paper (推荐 1.12+) |
| **Java** | JRE 8 或更高版本 |
| **TabooLib** | 6.2.4+ (自动加载) |
| **数据库** | SQLite (默认) 或 MySQL 5.7+ |

### 📥 安装步骤

1. **下载插件**
   ```bash
   # 从 Releases 页面下载最新版本
   wget https://github.com/BingZi-233/BilibiliVideo/releases/download/v2.0.0-beta/BilibiliVideo-2.0.0-beta.jar
   ```

2. **放置插件文件**
   ```
   plugins/
   └── BilibiliVideo-2.0.0-beta.jar
   ```

3. **启动服务器**

   插件会自动创建配置文件和数据库：
   ```
   plugins/BilibiliVideo/
   ├── config.yml          # 奖励配置
   ├── database.yml        # 数据库配置
   ├── lang/               # 语言文件
   └── bilibili_video.db   # SQLite 数据库
   ```

4. **验证安装**

   在控制台或游戏内执行：
   ```
   /bv help
   ```

---

## 💻 命令系统

### 权限节点

| 权限节点 | 说明 | 默认许可 |
|----------|------|----------|
| `bilibili.video.use` | 玩家基础命令（qrcode/status/triple/reward） | 所有人 |
| `bilibili.video.admin` | 管理员命令（unbind/credential/reload） | 仅 OP |

### 玩家命令

| 命令 | 功能 | 权限节点 |
|------|------|----------|
| `/bv help` | 查看帮助信息 | `bilibili.video.use` |
| `/bv qrcode` | 生成 B 站登录二维码地图 | `bilibili.video.use` |
| `/bv status` | 查看账号绑定状态 | `bilibili.video.use` |
| `/bv triple <bvid>` | 检测视频三连状态 | `bilibili.video.use` |
| `/bv reward <bvid>` | 领取三连奖励 | `bilibili.video.use` |

### 管理员命令

| 命令 | 功能 | 权限节点 |
|------|------|----------|
| `/bv admin reload` | 重载配置文件 | `bilibili.video.admin` |
| `/bv admin unbind <target>` | 解除玩家绑定（支持玩家名/UUID/B站UID） | `bilibili.video.admin` |
| `/bv admin credential list` | 列出所有登录凭证 | `bilibili.video.admin` |
| `/bv admin credential info <label>` | 查看凭证详细信息 | `bilibili.video.admin` |
| `/bv admin credential refresh <label>` | 刷新指定凭证（待实现） | `bilibili.video.admin` |

### 🎮 使用示例

```bash
# 1. 玩家绑定 B 站账号
/bv qrcode
# 系统会给玩家一张地图物品，展示登录二维码
# 用 B 站 APP 扫码并确认登录

# 2. 查看绑定状态
/bv status
# 输出: ✓ 已绑定 B 站账号: 用户名 (UID: 123456789)

# 3. 检测视频三连
/bv triple BV1xx411c7mD
# 输出: ✓ 点赞 | ✓ 投币(2) | ✓ 收藏 - 已完成三连!

# 4. 领取奖励
/bv reward BV1xx411c7mD
# 系统执行奖励脚本并提示领取成功
```

---

## ⚙️ 配置指南

### 数据库配置 (`database.yml`)

```yaml
database:
  # 是否启用数据库（必须为 true）
  enabled: true

  # 数据库类型: sqlite (默认) 或 mysql
  type: sqlite

  # SQLite 配置（开箱即用）
  sqlite:
    file: "bilibili_video.db"

  # MySQL 配置（可选）
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "bilibili_video"
    username: "root"
    password: "your_password"
    params: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"

  # HikariCP 连接池配置
  hikari:
    pool-name: "BilibiliVideoPool"
    maximum-pool-size: 10        # 最大连接数
    minimum-idle: 2              # 最小空闲连接
    connection-timeout: 30000    # 连接超时 (ms)
    idle-timeout: 600000         # 空闲超时 (ms)
    max-lifetime: 1800000        # 最大生命周期 (ms)

  # 数据库选项
  options:
    table-prefix: "bv_"          # 表名前缀
    show-sql: false              # 是否打印 SQL
    slow-sql-threshold-ms: 500   # 慢查询阈值 (ms)
```

### 奖励配置 (`config.yml`)

```yaml
reward:
  # 奖励模板定义
  templates:
    # 默认模板（通用奖励）
    default:
      description: "完成三连时发放的默认奖励"
      kether:
        - 'tell "&a[BilibiliVideo] &f感谢你的三连支持！"'
        - 'command papi "give %player_name% diamond 3"'

    # VIP 模板（高价值奖励）
    vip:
      description: "特殊视频的 VIP 奖励"
      kether:
        - 'tell "&6[BilibiliVideo] &f你获得了 VIP 三连奖励！"'
        - 'command papi "give %player_name% diamond 10"'
        - 'command papi "give %player_name% emerald 5"'

    # 活动模板（限时活动）
    event:
      description: "活动期间的特殊奖励"
      kether:
        - 'tell "&d[活动奖励] &f恭喜完成三连！"'
        - 'command papi "crate givekey %player_name% activity 1"'

  # 视频与奖励映射
  videos:
    # 为特定视频指定奖励模板
    BV1xx411c7mD:
      rewardKey: "vip"           # 使用 VIP 模板

    BV1yy411c7mE:
      rewardKey: "event"         # 使用活动模板

    BV1zz411c7mF: {}             # 使用默认模板（可省略 rewardKey）
```

#### 🎨 Kether 脚本语法示例

[Kether Explorer](https://taboo.8aka.org/kether-list/)

---

## 📊 数据库结构

### 表设计概览

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `bv_credential` | 存储 B 站登录凭证 | `sessdata`, `bili_jct`, `bilibili_mid`, `refresh_token` |
| `bv_bound_account` | 玩家与 B 站账号绑定关系 | `player_uuid`, `bilibili_mid`, `bilibili_name` |
| `bv_triple_status` | 视频三连状态记录 | `player_uuid`, `target_bvid`, `last_status` |
| `bv_reward_record` | 奖励领取记录 | `player_uuid`, `target_key`, `reward_key`, `status` |

### ER 关系图

```
┌─────────────────┐       ┌──────────────────┐
│  bv_credential  │──────▶│ bv_bound_account │
│  (凭证表)        │ 1:N   │  (绑定表)         │
└─────────────────┘       └──────────────────┘
                                    │
                                    │ 1:N
                                    ▼
                          ┌──────────────────┐
                          │ bv_triple_status │
                          │  (三连状态表)     │
                          └──────────────────┘
                                    │
                                    │ 1:N
                                    ▼
                          ┌──────────────────┐
                          │ bv_reward_record │
                          │  (奖励记录表)     │
                          └──────────────────┘
```

---

## 🔧 技术栈

### 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| **Kotlin** | 2.2.0 | 主开发语言 |
| **TabooLib** | 6.2.4 | 插件开发框架 |
| **Gradle** | Kotlin DSL | 构建工具 |

### 数据库层

| 依赖 | 版本 | 说明 |
|------|------|------|
| **Ktorm** | 3.6.0 | Kotlin ORM 框架 |
| **HikariCP** | 4.0.3 | 高性能连接池 |
| **SQLite JDBC** | 3.45.1.0 | SQLite 驱动 |
| **MySQL Connector** | 8.3.0 | MySQL 驱动 |

### 网络与序列化

| 依赖 | 版本 | 说明 |
|------|------|------|
| **OkHttp3** | 4.12.0 | HTTP 客户端 |
| **Okio** | 3.6.0 | 高效 I/O 库 |
| **Gson** | 2.11.0 | JSON 序列化 |
| **ZXing** | 3.5.2 | 二维码生成 |

### TabooLib 模块

```
Basic, BukkitHook, BukkitNMS, BukkitUtil,
CommandHelper, I18n, Metrics, MinecraftChat,
Kether, Bukkit
```

---

## 🛠️ 开发文档

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/BingZi-233/BilibiliVideo.git
cd BilibiliVideo

# 构建发行版本（不含 TabooLib 本体）
./gradlew build
# 输出: build/libs/BilibiliVideo-2.0.0-beta.jar

# 构建 API 版本（包含 TabooLib API，用于开发依赖）
./gradlew taboolibBuildApi
# 输出: build/libs/BilibiliVideo-2.0.0-beta-api.jar

# 发布到 Maven 仓库
./gradlew publish
```

### 开发环境

| 要求 | 版本 |
|------|------|
| **JDK** | 1.8+ |
| **IntelliJ IDEA** | 推荐使用 |
| **Kotlin 插件** | 内置支持 |

### 代码规范

```kotlin
// 使用 Kotlin object 作为插件主类
object BilibiliVideo : Plugin() {
    override fun onEnable() {
        // 初始化逻辑
    }
}

// 统一使用 TabooLib 的扩展函数
fun Player.sendColoredMessage(message: String) {
    sendMessage(message.colored())
}

// 异步执行网络请求
submit(async = true) {
    val response = BilibiliHttpClient.get("https://api.bilibili.com/...")
    // 处理响应
}
```

### 项目结构

```
src/main/kotlin/online/bingzi/bilibili/video/
├── BilibiliVideo.kt                          # 插件主类
└── internal/
    ├── bilibili/                             # B 站 API 封装
    │   ├── TripleActionApi.kt
    │   └── dto/
    ├── command/                              # 命令系统
    │   └── BilibiliVideoCommand.kt
    ├── config/                               # 配置管理
    │   ├── DatabaseConfig.kt
    │   └── RewardConfig.kt
    ├── credential/                           # 登录凭证
    │   └── QrLoginService.kt
    ├── database/                             # 数据库层
    │   ├── DatabaseFactory.kt
    │   └── DatabaseSchemaInitializer.kt
    ├── entity/                               # 数据实体
    │   ├── CredentialEntities.kt
    │   ├── BoundAccountEntities.kt
    │   ├── TripleStatusEntities.kt
    │   └── RewardRecordEntities.kt
    ├── http/                                 # HTTP 客户端
    │   └── BilibiliHttpClient.kt
    ├── repository/                           # 数据仓库
    │   ├── CredentialRepository.kt
    │   ├── BoundAccountRepository.kt
    │   ├── TripleStatusRepository.kt
    │   └── RewardRecordRepository.kt
    ├── service/                              # 业务服务
    │   ├── BindingService.kt
    │   ├── CredentialService.kt
    │   ├── TripleCheckService.kt
    │   ├── RewardService.kt
    │   └── RewardKetherExecutor.kt
    └── ui/                                   # UI 层
        ├── QrMapService.kt
        ├── QrMapRenderer.kt
        ├── MapViewCompat.kt
        └── MapMaterialCompat.kt
```

---

## 🤝 贡献指南

我们欢迎所有形式的贡献！

### 提交流程

1. **Fork** 本仓库
2. 创建特性分支
   ```bash
   git checkout -b feature/AmazingFeature
   ```
3. 提交你的修改
   ```bash
   git commit -m '✨ feat: 添加了某个很棒的功能'
   ```
4. 推送到分支
   ```bash
   git push origin feature/AmazingFeature
   ```
5. 开启 **Pull Request**

### 提交规范

我们使用 [约定式提交](https://www.conventionalcommits.org/zh-hans/) 规范：

```
类型(范围): 简短描述

详细描述（可选）

关联 Issue（可选）
```

**类型说明**：
- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构代码
- `test`: 测试相关
- `chore`: 构建/工具链相关

---

## 📄 许可证

本项目采用 **MIT License** 开源协议。

这意味着你可以：
- ✅ 商业使用
- ✅ 修改源代码
- ✅ 分发
- ✅ 私用

详见 [LICENSE](LICENSE) 文件。

---

## 🔗 相关链接

| 资源 | 链接 |
|------|------|
| **项目主页** | [GitHub Repository](https://github.com/BingZi-233/BilibiliVideo) |
| **问题反馈** | [Issues](https://github.com/BingZi-233/BilibiliVideo/issues) |
| **TabooLib 官网** | [https://www.tabooproject.org](https://www.tabooproject.org) |
| **Bilibili API 文档** | [SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) |

---

## 👨‍💻 作者信息

- **开发者**: BingZi-233
- **联系方式**: 见 GitHub Profile

---

## 🙏 致谢

感谢以下项目和开源社区的支持：

- [TabooLib](https://github.com/TabooLib/taboolib) - 强大的 Bukkit 插件开发框架
- [Ktorm](https://github.com/kotlin-orm/ktorm) - 轻量级 Kotlin ORM 框架
- [OkHttp](https://github.com/square/okhttp) - 高效的 HTTP 客户端
- [SocialSisterYi/bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) - B 站 API 文档收集
- [ZXing](https://github.com/zxing/zxing) - 二维码生成库

---

## 💰 赞助支持

如果这个项目对你有帮助，欢迎通过以下方式支持开发：

<div align="center">

| 微信支付 | 支付宝 |
|:-------:|:------:|
| <img src="images/weixin.png" width="200"/> | <img src="images/zhifubao.png" width="200"/> |

</div>

你的支持是我持续维护和改进项目的动力！🙏

---

<div align="center">

**如果这个项目对你有帮助，请给个 ⭐ Star 支持一下！**

Made with ❤️ by [BingZi-233](https://github.com/BingZi-233)

</div>