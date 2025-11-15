# CLAUDE.md

这个文件为 Claude Code (claude.ai/code) 提供指导，帮助其在此仓库中工作。

## 项目概述

BilibiliVideo 是一个基于 TabooLib 6.2.4 框架开发的 Minecraft (Bukkit/Spigot/Paper) 服务器插件，提供 B 站账号绑定、视频三连检测和奖励发放功能。

- **语言**: Kotlin 2.2.0
- **构建工具**: Gradle (Kotlin DSL)
- **目标 JVM**: Java 8
- **框架**: TabooLib 6.2.4
- **包名**: `online.bingzi.bilibili.video`
- **当前版本**: 2.0.0-beta

## 常用命令

### 构建与开发

```bash
# 构建发行版本(不包含 TabooLib 本体)
./gradlew build
# 输出: build/libs/BilibiliVideo-2.0.0-beta.jar

# 构建 API 版本(包含 TabooLib API，用于开发依赖)
./gradlew taboolibBuildApi
# 输出: build/libs/BilibiliVideo-2.0.0-beta-api.jar

# 清理构建产物
./gradlew clean

# 发布到 Maven 仓库(需要配置 MAVEN_USERNAME 和 MAVEN_PASSWORD)
./gradlew publish

# 编译不运行测试
./gradlew build -x test
```

### 依赖管理

所有依赖都在 `build.gradle.kts` 中配置为 `compileOnly`，因为 TabooLib 使用隔离类加载器机制:

```kotlin
// 核心依赖版本
ktormVersion = "3.6.0"           // ORM 框架
hikariVersion = "4.0.3"          // 连接池
okhttpVersion = "4.12.0"         // HTTP 客户端
gsonVersion = "2.11.0"           // JSON 序列化
zxingVersion = "3.5.2"           // 二维码生成
```

## 架构设计

项目采用经典的分层架构:

```
命令层 (Command)
    ↓
服务层 (Service)
    ↓
仓库层 (Repository)
    ↓
数据库层 (Database/ORM)
    ↓
外部 API 层 (Bilibili API / HTTP)
```

### 核心模块

#### 1. 数据库层 (`internal/database/`)

- **DatabaseFactory**: 负责初始化 HikariCP 数据源和 Ktorm Database 实例
- **DatabaseSchemaInitializer**: 自动建表和 Schema 管理
- 支持 SQLite(默认) 和 MySQL 两种数据库
- 配置文件: `src/main/resources/database.yml`

#### 2. 实体层 (`internal/entity/`)

使用 Ktorm 的 Entity API 定义数据实体:

- `CredentialEntities.kt`: B 站登录凭证(SESSDATA, bili_jct, refresh_token 等)
- `BoundAccountEntities.kt`: 玩家与 B 站账号的绑定关系
- `TripleStatusEntities.kt`: 视频三连状态记录
- `RewardRecordEntities.kt`: 奖励领取记录

表前缀在 `DatabaseTablePrefix.kt` 中定义(默认 `bv_`)，可通过 `database.yml` 的 `options.table-prefix` 配置修改。

#### 3. 仓库层 (`internal/repository/`)

提供纯数据访问接口，封装 Ktorm 的 CRUD 操作:

- `CredentialRepository`: 凭证数据访问
- `BoundAccountRepository`: 绑定关系数据访问
- `TripleStatusRepository`: 三连状态数据访问
- `RewardRecordRepository`: 奖励记录数据访问

**重要**: Repository 层只做数据操作，不包含业务逻辑。

#### 4. 服务层 (`internal/service/`)

包含核心业务逻辑:

- **BindingService**: 账号绑定/解绑业务，确保一对一绑定规则
- **CredentialService**: 凭证管理，提供通过玩家/标签获取凭证的功能
- **TripleCheckService**: 调用 B 站 API 检测视频三连状态
- **RewardService**: 奖励发放业务，检查三连状态并记录奖励
- **RewardKetherExecutor**: 执行 Kether 脚本奖励

**设计原则**: Service 层不持有任何 Bukkit 对象(如 Player)，避免内存泄漏。所有需要 Player 的场景都传递 UUID/名称等基本类型。

#### 5. 命令层 (`internal/command/`)

使用 TabooLib CommandHelper 模块定义命令:

主命令: `/bv` (别名: `/bilibili`)

玩家命令:
- `/bv qrcode` - 生成登录二维码地图
- `/bv status` - 查看绑定状态
- `/bv triple <bvid>` - 检测视频三连状态
- `/bv reward <bvid>` - 领取三连奖励

管理员命令:
- `/bv admin reload` - 重载配置
- `/bv admin credential list` - 列出所有凭证
- `/bv admin credential info <label>` - 查看凭证详情
- `/bv admin credential refresh <label>` - 刷新凭证(待实现)

#### 6. HTTP 层 (`internal/http/`)

**BilibiliHttpClient**: 统一的 B 站 API 调用底座

- 使用 OkHttp3 作为 HTTP 客户端
- 封装了 GET 和 POST Form 方法
- 统一处理 Cookie 头、User-Agent、Referer
- 使用 Gson 进行 JSON 序列化/反序列化

#### 7. B 站 API 集成 (`internal/bilibili/`)

**TripleActionApi**: 封装 B 站视频三连相关 API

- `checkTripleStatus()`: 检查视频的点赞、投币、收藏状态
- 返回 `TripleStatus` 数据类，包含 `liked`, `coinCount`, `favoured`, `isTriple` 等字段

DTO 定义在 `internal/bilibili/dto/` 目录下:
- `TripleDtos.kt`: 三连状态响应
- `QrLoginDtos.kt`: 二维码登录响应

#### 8. 二维码登录 (`internal/credential/`)

**QrLoginService**: 管理 B 站二维码登录流程

- `startLogin(player)`: 发起登录，返回二维码 URL
- 使用内部线程池轮询登录状态
- 登录成功后自动保存凭证并建立绑定关系

#### 9. UI 层 (`internal/ui/`)

**QrMapService**: 将二维码渲染到 Minecraft 地图物品

- `createQrMapItem(player, qrUrl)`: 创建包含二维码的地图物品
- **QrMapRenderer**: 使用 MapView API 渲染二维码
- **MapViewCompat / MapMaterialCompat**: 跨版本兼容性封装

#### 10. 配置层 (`internal/config/`)

- **DatabaseConfig**: 数据库配置(`database.yml`)
- **RewardConfig**: 奖励配置(`config.yml`)

奖励配置使用模板系统:
```yaml
reward:
  templates:
    default:
      kether: [...]  # Kether 脚本列表
  videos:
    BV1xxxxx:
      rewardKey: "default"  # 指定使用的模板
```

## 重要技术细节

### TabooLib 框架

项目使用以下 TabooLib 模块:

```kotlin
install(Basic)            // 基础模块
install(BukkitHook)       // PlaceholderAPI 等集成
install(BukkitNMS)        // NMS 抽象层
install(BukkitUtil)       // Bukkit 工具
install(CommandHelper)    // 命令系统
install(I18n)            // 国际化
install(Metrics)         // bStats 统计
install(MinecraftChat)   // 聊天消息
install(Kether)          // 脚本引擎
install(Bukkit)          // Bukkit 核心
```

关键 API:
- `submit(async = true) { ... }`: 异步执行任务
- `Plugin.onEnable()` / `Plugin.onDisable()`: 插件生命周期
- `@CommandHeader` / `@CommandBody`: 声明式命令定义

### Ktorm ORM

使用 Ktorm 3.6.0 的 Entity API 风格:

```kotlin
// 定义实体接口
interface Credential : Entity<Credential> {
    val id: Long
    var sessdata: String
    // ...
}

// 定义表对象
object Credentials : Table<Credential>("${DATABASE_TABLE_PREFIX}credential") {
    val id = long("id").primaryKey().bindTo { it.id }
    val sessdata = varchar("sessdata").bindTo { it.sessdata }
    // ...
}

// CRUD 操作
database.sequenceOf(Credentials).find { it.id eq 1 }
database.sequenceOf(Credentials).add(entity)
```

### 异步模式

所有网络请求和数据库操作都应该异步执行:

```kotlin
submit(async = true) {
    // 异步操作(在工作线程执行)
    val result = someBlockingOperation()

    submit {
        // 回到主线程(操作 Bukkit API)
        player.sendMessage("结果: $result")
    }
}
```

**规则**:
- Repository 层的数据库调用必须在异步上下文中调用
- Service 层的网络请求必须在异步上下文中调用
- 所有 Bukkit API(如 `player.sendMessage()`, `player.inventory.addItem()`)必须在主线程执行

### 数据库表前缀

表前缀通过 `DATABASE_TABLE_PREFIX` 全局变量控制:

```kotlin
internal var DATABASE_TABLE_PREFIX: String = "bv_"
```

在 `DatabaseFactory.init()` 中从配置文件读取并覆盖，必须在访问任何实体之前完成。

### Kether 脚本执行

奖励使用 TabooLib 的 Kether 脚本引擎:

```kotlin
KetherShell.eval(scriptLines, sender = player) {
    // 脚本执行成功
}.thenApply { result ->
    // 处理结果
}
```

脚本示例:
```yaml
kether:
  - 'tell "&a感谢你的三连！"'
  - 'command papi "give %player_name% diamond 3"'
```

## 开发指南

### 添加新的 Service

1. 在 `internal/service/` 创建新的 Service object
2. 定义 data class 作为返回值(包含 success 字段和 message)
3. 不要持有 Bukkit 对象(如 Player)，只使用 UUID/String 等基本类型
4. 依赖 Repository 层进行数据操作

### 添加新的 Repository

1. 在 `internal/entity/` 定义实体接口和表对象
2. 在 `internal/repository/` 创建 Repository object
3. 使用 Ktorm 的 `sequenceOf()` API 进行 CRUD
4. 所有方法都应该是纯数据操作，不包含业务逻辑

### 添加新的命令

1. 在 `BilibiliVideoCommand` 中添加新的 `@CommandBody val xxx = subCommand { ... }`
2. 使用 `execute<Player>` 或 `execute<ProxyCommandSender>`
3. 在 execute 块中使用 `submit(async = true)` 进行异步操作
4. 使用嵌套的 `submit {}` 回到主线程操作 Bukkit API

### 添加新的 B 站 API

1. 在 `internal/bilibili/dto/` 定义 DTO data class
2. 在 `internal/bilibili/` 创建 API object
3. 使用 `BilibiliHttpClient.get()` 或 `postForm()` 进行请求
4. 传入凭证的 Cookie 字符串

### 数据库 Schema 变更

1. 在对应的 `*Entities.kt` 中修改实体定义
2. 在 `DatabaseSchemaInitializer.ensureSchema()` 中添加建表/迁移逻辑
3. **重要**: 确保向后兼容，使用 `ALTER TABLE` 而不是 `DROP TABLE`

## 测试与调试

### 本地测试环境

1. 准备一个 Bukkit/Spigot/Paper 测试服务器(1.12+)
2. 构建插件: `./gradlew build`
3. 将 `build/libs/BilibiliVideo-2.0.0-beta.jar` 复制到 `plugins/` 目录
4. 启动服务器并查看日志

### 调试数据库

SQLite 数据库文件位于: `plugins/BilibiliVideo/bilibili_video.db`

可以使用 SQLite 客户端查看:
```bash
sqlite3 plugins/BilibiliVideo/bilibili_video.db
.tables
.schema bv_credential
SELECT * FROM bv_credential;
```

### 查看日志

插件使用 TabooLib 的 `info()`, `warning()` 等函数输出日志:

```kotlin
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

info("[Database] 初始化完成")
warning("[API] 请求失败: ${e.message}")
```

日志会输出到服务器控制台。

## 常见问题

### 数据库连接失败

检查 `database.yml` 配置:
- SQLite: 确保 `file` 字段正确，插件会自动创建文件
- MySQL: 检查 `host`, `port`, `username`, `password` 是否正确

### Kether 脚本不执行

1. 检查 `config.yml` 的 `reward.templates` 配置
2. 确保脚本语法正确(参考 TabooLib Kether 文档)
3. 查看服务器日志中的错误信息

### 玩家绑定失败

1. 检查数据库是否初始化成功
2. 确保玩家有权限: `bilibili.command.qrcode`
3. 查看日志中的详细错误信息

## 相关文档

- [TabooLib 官方文档](https://docs.tabooproject.org/)
- [Ktorm 官方文档](https://www.ktorm.org/)
- [B 站 API 文档收集](https://github.com/SocialSisterYi/bilibili-API-collect)
- 项目内 B 站 API 文档: `docs/bilibili-API-collect/`
- 项目内 TabooLib 模块文档: `docs/taboolib/`

## 提交规范

使用约定式提交(Conventional Commits):

```
类型(范围): 简短描述

详细描述(可选)
```

类型:
- `feat`: 新功能
- `fix`: Bug 修复
- `refactor`: 重构
- `docs`: 文档更新
- `chore`: 构建/工具链相关
- `test`: 测试相关

示例:
```
feat(credential): 添加凭证自动刷新功能
fix(database): 修复 MySQL 连接池泄漏问题
refactor(service): 重构 BindingService 简化逻辑
docs(readme): 更新安装步骤说明
```
