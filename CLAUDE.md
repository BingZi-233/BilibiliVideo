# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

BilibiliVideo 是一个基于 TabooLib 6.2.4 框架开发的 Minecraft 服务器插件，让玩家在游戏内完成 B 站账号绑定、视频三连检测和奖励领取。

- **语言**: Kotlin 2.2.0 (目标 Java 8)
- **构建工具**: Gradle (Kotlin DSL)
- **框架**: TabooLib 6.2.4
- **包名**: `online.bingzi.bilibili.video`

## 构建命令

```bash
# 构建发行版本（不含 TabooLib 本体）
./gradlew build
# 输出: build/libs/BilibiliVideo-<version>.jar

# 构建 API 版本（包含 TabooLib API，用于依赖）
./gradlew taboolibBuildApi

# 清理构建产物
./gradlew clean

# 发布到 Maven（需要 MAVEN_USERNAME 和 MAVEN_PASSWORD）
./gradlew publish
```

**注意**：项目目前没有单元测试。

## 关键约束（违反即崩溃）

### 1. 异步执行规则

**所有网络请求和数据库操作必须在异步上下文中执行，所有 Bukkit API 必须在主线程执行。**

```kotlin
// ✅ 正确：异步 → 主线程
submit(async = true) {
    val result = apiCall()  // 在工作线程执行网络请求
    submit {
        player.sendMessage(result)  // 回到主线程操作 Bukkit API
    }
}

// ❌ 错误：阻塞主线程
fun someCommand() {
    val result = apiCall()  // 💥 阻塞主线程，卡服
    player.sendMessage(result)
}

// ❌ 错误：在工作线程操作 Bukkit API
submit(async = true) {
    player.sendMessage("Hi")  // 💥 线程安全问题，随机崩溃
}
```

### 2. 依赖加载机制

**所有外部依赖在 `build.gradle.kts` 中使用 `compileOnly`，因为 TabooLib 使用隔离类加载器在运行时加载。**

不要改成 `implementation`，否则依赖会被重复打包并在类加载时冲突。

当前依赖版本：
- Ktorm: 3.6.0（ORM 框架）
- HikariCP: 4.0.3（连接池）
- OkHttp: 4.12.0（HTTP 客户端）
- Gson: 2.11.0（JSON 序列化）
- ZXing: 3.5.2（二维码生成）

### 3. 数据库表前缀初始化顺序

**表前缀 `DATABASE_TABLE_PREFIX` 必须在 `DatabaseFactory.initFromConfig()` 中初始化，且必须在任何实体访问之前完成。**

```kotlin
// internal/DatabaseTablePrefix.kt
internal var DATABASE_TABLE_PREFIX: String = "bv_"  // 默认值

// internal/database/DatabaseFactory.kt
fun initFromConfig() {
    DATABASE_TABLE_PREFIX = config.getString("options.table-prefix")  // 从配置读取
    // ... 之后才能安全访问实体
}
```

**违反此顺序会导致表名错误，无法找到数据。**

### 4. Service 层设计原则

**Service 层不能持有任何 Bukkit 对象（如 `Player`），只能使用基本类型（UUID、String）。**

```kotlin
// ✅ 正确
object SomeService {
    fun doSomething(playerUuid: UUID): Result {
        // 使用 UUID 而不是 Player 对象
    }
}

// ❌ 错误
object SomeService {
    fun doSomething(player: Player): Result {
        // 💥 持有 Player 引用可能导致内存泄漏
    }
}
```

这是为了避免内存泄漏，因为 Service 是 Kotlin `object`（单例），会在整个插件生命周期中存活。

## 架构概览

项目使用标准分层架构：

```
Command (命令层)
  ↓ 调用
Service (业务逻辑层)
  ↓ 调用
Repository (数据访问层)
  ↓ 使用
Entity (ORM 实体层)
  ↓ 映射
Database (数据库)
```

核心模块位于 `internal/` 包下：
- `command/` - TabooLib CommandHelper 命令定义
- `service/` - 业务逻辑（绑定、三连检测、奖励发放等）
- `repository/` - Ktorm 数据访问封装
- `entity/` - Ktorm Entity 定义和表对象
- `database/` - HikariCP 数据源和 Schema 初始化
- `http/` - OkHttp3 封装的 B 站 API 客户端
- `credential/` - 二维码登录流程管理
- `ui/` - Minecraft 地图物品二维码渲染
- `config/` - 配置文件映射

## Ktorm ORM 使用

项目使用 Ktorm 3.6.0 的 Entity API 风格：

```kotlin
// 定义实体接口
interface Credential : Entity<Credential> {
    val id: Long
    var sessdata: String
}

// 定义表对象（注意表前缀变量）
object Credentials : Table<Credential>("${DATABASE_TABLE_PREFIX}credential") {
    val id = long("id").primaryKey().bindTo { it.id }
    val sessdata = varchar("sessdata").bindTo { it.sessdata }
}

// CRUD 操作
database.sequenceOf(Credentials).find { it.id eq 1 }
database.sequenceOf(Credentials).add(entity)
```

**重要**：Repository 层只做数据操作，不包含业务逻辑。业务逻辑属于 Service 层。

## 配置文件

### database.yml
- `type`: `sqlite`（默认）或 `mysql`
- `options.table-prefix`: 数据库表前缀（默认 `bv_`）
- `hikari.*`: HikariCP 连接池配置

### config.yml
- `reward.templates`: 奖励模板，使用 Kether 脚本定义
- `reward.videos`: 针对特定 bvid 的奖励配置

Kether 脚本示例：
```yaml
kether:
  - 'tell "&a感谢你的三连！"'
  - 'command papi "give %player_name% diamond 3"'
```

参考：[Kether Explorer](https://taboo.8aka.org/kether-list/)

## 数据库 Schema 变更

1. 在 `internal/entity/*Entities.kt` 中修改实体定义
2. 在 `DatabaseSchemaInitializer.ensureSchema()` 中添加建表/迁移逻辑
3. **必须保证向后兼容**：使用 `ALTER TABLE` 而不是 `DROP TABLE`

## 提交规范

使用约定式提交（Conventional Commits）：

```
类型(范围): 简短描述

详细描述（可选）
```

类型：
- `feat`: 新功能
- `fix`: Bug 修复
- `refactor`: 重构
- `docs`: 文档更新
- `chore`: 构建/工具链相关
- `test`: 测试相关

示例：
```
feat(credential): 添加凭证自动刷新功能
fix(database): 修复 MySQL 连接池泄漏问题
refactor(service): 重构 BindingService 简化逻辑
```

## 相关文档

- [TabooLib 官方文档](https://docs.tabooproject.org/)
- [Ktorm 官方文档](https://www.ktorm.org/)
- [B 站 API 文档收集](https://github.com/SocialSisterYi/bilibili-API-collect)
