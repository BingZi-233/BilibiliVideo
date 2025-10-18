# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

BilibiliVideo 是一个基于 TabooLib 6.2.3 框架开发的 Minecraft Bukkit 插件，使用 Kotlin 语言实现。插件允许每个玩家登录自己的 Bilibili 账户，并提供视频三连状态查询、UP主关注状态查询等功能，同时支持基于 Kether 脚本的奖励系统。

**技术栈**: Kotlin 2.2.0 (JVM 1.8) + TabooLib 6.2.3 + Gradle (Kotlin DSL) + OkHttp3 + Exposed ORM

## 构建和开发命令

### 构建命令
```bash
# 清理构建文件
./gradlew clean

# 编译 Kotlin 代码
./gradlew compileKotlin

# 构建发行版本（不含 TabooLib 本体，用于服务器部署）
./gradlew build
# 产物位于: build/libs/${project.name}-${version}.jar

# 构建 API JAR（用于开发者依赖，包含 TabooLib 但移除逻辑代码）
./gradlew taboolibBuildApi -PDeleteCode
# 产物位于: build/libs/${project.name}-${version}-api.jar

# 构建完整开发版本（包含 TabooLib 本体）
./gradlew taboolibBuild

# 发布到 Maven 仓库（需要环境变量或 gradle.properties 中配置认证信息）
./gradlew publish
```

### 测试命令
```bash
# 运行测试（当前项目无测试目录，需要先创建 src/test/kotlin）
./gradlew test
```

### 重要编译参数
- `-Xjvm-default=all`: Kotlin 接口默认方法生成策略
- JVM Target: 1.8
- 编码: UTF-8

## 核心架构

### 包结构设计

```
online.bingzi.bilibili.bilibilivideo/
├── BilibiliVideo.kt                # 主插件类（Object，继承 Plugin）
├── api/                            # 公共 API 包（供第三方开发者使用）
│   ├── event/                      # 事件定义
│   │   ├── BilibiliLoginEvent      # 登录事件
│   │   ├── BilibiliLogoutEvent     # 登出事件
│   │   ├── VideoTripleStatusCheckEvent  # 视频三连状态检查事件
│   │   └── UpFollowStatusCheckEvent     # UP主关注状态检查事件
│   └── qrcode/                     # 二维码发送系统（可扩展框架）
│       ├── sender/QRCodeSender     # 发送器接口
│       ├── registry/QRCodeSenderRegistry  # 注册中心
│       ├── result/SendResult       # 发送结果（密封类）
│       └── options/SendOptions     # 发送选项
└── internal/                       # 内部实现包（不保证向后兼容）
    ├── bilibili/                   # Bilibili API 实现
    │   ├── api/                    # API 请求封装
    │   │   ├── QrCodeApi          # 二维码登录 API
    │   │   ├── VideoApi           # 视频相关 API
    │   │   └── UserApi            # 用户相关 API
    │   ├── model/                  # 数据模型
    │   ├── client/HttpClientFactory # HTTP 客户端工厂
    │   └── helper/CookieHelper     # Cookie 管理（包含风控机制）
    ├── command/                    # 命令系统
    │   ├── BilibiliCommand         # 主命令类（声明式命令定义）
    │   └── handler/                # 命令处理器
    │       ├── LoginCommandHandler
    │       ├── LogoutCommandHandler
    │       ├── TripleStatusCommandHandler
    │       └── FollowStatusCommandHandler
    ├── config/                     # 配置管理
    │   ├── DatabaseConfig          # 数据库配置
    │   └── SettingConfig           # 插件设置配置
    ├── database/                   # 数据持久化层
    │   ├── DatabaseManager         # 生命周期管理
    │   ├── DatabaseService         # 统一数据服务接口
    │   ├── factory/TableFactory    # 表工厂
    │   ├── table/                  # 表定义（Exposed Table）
    │   ├── entity/                 # 数据实体类
    │   └── dao/                    # 数据访问接口
    ├── event/EventManager          # 内部事件管理
    ├── helper/KetherHelper         # Kether 脚本辅助
    ├── manager/                    # 业务管理器
    │   ├── BvManager              # BV 号管理
    │   └── SessionManager         # 会话管理
    ├── reward/RewardManager        # 奖励系统（基于 Kether 脚本）
    └── session/LoginSession        # 登录会话
```

### 关键架构模式

#### 1. 命令系统（TabooLib CommandHelper）
使用声明式命令定义，通过注解 `@CommandHeader` 和 `@CommandBody` 定义命令结构：
- 主命令: `/bili` (别名: `/bilibili`, `/bl`)
- 子命令: `login`, `logout`, `triple`, `follow`
- 权限: `bilibili.use`（默认 true）, `bilibili.admin`（操作其他玩家）
- 所有命令支持补全和参数验证（通过 `suggestion` 和 `restrict` DSL）

#### 2. 数据库架构（Exposed ORM）
- 双数据库支持: MySQL / SQLite（通过配置自动切换）
- 异步操作: 所有数据库操作通过 `DatabaseService` 异步执行
- 数据表:
  - `player_bindings`: 玩家-MID 绑定关系（一对一）
  - `bilibili_accounts`: Bilibili 账户信息（Cookie、刷新令牌）
  - `video_triple_status`: 视频三连状态记录
  - `up_follow_status`: UP主关注状态记录
  - `video_reward_records`: 奖励发放历史记录
- 初始化: 插件启用时自动通过 `@Awake(LifeCycle.ENABLE)` 初始化

#### 3. 奖励系统（Kether 脚本驱动）
- 配置文件: `src/main/resources/setting.yml`
- 支持两种奖励配置:
  - `default-reward`: 默认奖励（未特殊配置的 BV 号）
  - `videos[bvid]`: 特定 BV 号专属奖励
- 奖励执行: 通过 `KetherHelper.ketherEval()` 执行 Kether 脚本
- 防重复机制: 通过 `video_reward_records` 表防止重复领奖
- 事件驱动: 监听 `VideoTripleStatusCheckEvent` 自动发放奖励

#### 4. Bilibili API 集成
- HTTP 客户端: OkHttp3 + 日志拦截器
- 依赖重定位:
  - `com.google.gson` → `online.bingzi.bilibili.bilibilivideo.library.gson`
  - `okhttp3` → `online.bingzi.bilibili.bilibilivideo.library.okhttp3`
- 风控机制:
  - `buvid3` 自动生成和管理
  - WBI 签名机制（UP主关注状态查询）
  - Cookie 自动刷新（RSA-OAEP 加密）
- 异步请求: 使用 TabooLib 任务调度系统 (`submit(async = true)`)

#### 5. 会话管理
- 每个玩家独立登录会话（`LoginSession`）
- 会话存储在 `SessionManager` 中（内存 + 数据库持久化）
- 登录流程: 二维码生成 → 轮询扫码状态 → 获取 Cookie → 保存会话
- 登出清理: 删除内存会话 + 触发 `BilibiliLogoutEvent`

## 关键实现细节

### BV 号格式验证
```kotlin
// 正则表达式: BV + 10位字符（不包含 0、I、O、l）
Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$")
```
文件位置: `src/main/kotlin/.../internal/command/BilibiliCommand.kt:183`

### Kether 脚本常用语法
```yaml
rewards:
  - 'command "eco give " + player.name + " 100"'  # 执行命令
  - 'tell player "&a消息内容"'                     # 发送消息
  - 'sound player "ENTITY_PLAYER_LEVELUP" 1.0 1.0' # 播放音效
  - 'if player.level > 50 then { ... } else { ... }' # 条件判断
  - 'random(1, 100)'                               # 随机数
```

### 数据库操作模式
所有数据库操作都使用回调模式，确保异步执行：
```kotlin
DatabaseService.bindPlayer(playerUuid, mid, playerName) { success ->
    if (success) {
        // 处理成功逻辑
    } else {
        // 处理失败逻辑（错误已记录到日志）
    }
}
```

### TabooLib 生命周期
- `@Awake(LifeCycle.LOAD)`: 插件加载阶段
- `@Awake(LifeCycle.ENABLE)`: 插件启用阶段（数据库初始化）
- `@Awake(LifeCycle.ACTIVE)`: 插件激活阶段
- `@Awake(LifeCycle.DISABLE)`: 插件禁用阶段（资源清理）

## 配置文件

### 主配置 (src/main/resources/setting.yml)
- `default-reward`: 默认奖励配置
  - `enabled`: 是否启用
  - `require-complete-triple`: 是否需要完整三连
  - `rewards`: Kether 脚本列表
- `videos[bvid]`: 特定 BV 号奖励配置
  - `name`: 视频显示名称
  - `enabled`: 是否启用
  - `require-complete-triple`: 三连要求
  - `rewards`: Kether 脚本列表
- `settings`: 系统设置
  - `prevent-duplicate-rewards`: 是否阻止重复领奖
  - `reward-delay`: 奖励发放延迟（毫秒）
  - `play-sound`: 是否播放音效
  - `sound-type/volume/pitch`: 音效配置

### 数据库配置 (src/main/resources/database.yml)
通过 TabooLib DatabasePlayer 模块管理，支持 MySQL 和 SQLite 配置。

### 多语言配置 (src/main/resources/lang/)
- `zh_CN.yml`: 简体中文
- `en_US.yml`: 英文

## 开发注意事项

### 代码风格
- 使用 Kotlin `object` 作为主插件类和单例管理器
- 缩进: 4 空格
- 命名: 包名小写，类名 PascalCase，方法/变量 camelCase
- 可空性: 显式标记可空类型，避免 `!!` 操作符
- 公共 API 必须包含 KDoc 文档

### TabooLib 模块依赖
项目使用以下 TabooLib 模块（在 build.gradle.kts 中定义）:
- `Basic`: 基础模块
- `BukkitHook`, `BukkitUtil`: Bukkit 集成
- `CommandHelper`: 命令系统
- `I18n`: 国际化
- `Metrics`: bStats 统计
- `MinecraftChat`: 聊天消息处理
- `Bukkit`: Bukkit 平台核心
- `Kether`: 脚本引擎
- `Database`: 数据库支持

### 依赖管理
- 外部依赖通过 `taboo()` 或 `compileOnly()` 添加
- 需要重定位的依赖在 `taboolib {}` 块中使用 `relocate()` 配置
- 新增库前必须评估是否与服务器或其他插件冲突

### 测试建议
当前项目无测试目录，建议新增 `src/test/kotlin` 并使用 JUnit 5 + MockK。测试文件以 `*Test.kt` 结尾，与被测类同包。

### Git 提交规范
- 提交信息使用中文简体，动词开头（如：修复、重构、新增）
- PR 需包含: 变更摘要、动机、影响面、关联 Issue
- 修改配置文件或命令时，需同步更新相关文档（`docs/*`）和多语言文件（`lang/*`）

### 安全要求
- 切勿提交密钥、令牌、Cookie、环境变量到版本控制
- 使用 `.env` 或 `gradle.properties` 管理敏感配置
- 项目已配置 `.gitignore` 忽略敏感文件

## 常见任务

### 添加新的 Bilibili API
1. 在 `internal/bilibili/api/` 创建新的 API 类
2. 在 `internal/bilibili/model/` 定义数据模型
3. 使用 `HttpClientFactory.createClient()` 创建 HTTP 客户端
4. 异步执行请求: `submit(async = true) { ... }`
5. 在 `CookieHelper` 中添加风控机制支持（如需要）

### 添加新命令
1. 在 `BilibiliCommand.kt` 中使用 `@CommandBody` 定义子命令
2. 在 `handler/` 目录创建对应的命令处理器
3. 使用 `suggestion` DSL 提供补全
4. 使用 `restrict` DSL 验证参数
5. 在多语言文件中添加相关提示消息

### 扩展奖励系统
1. 在 `setting.yml` 的 `videos` 下添加新的 BV 号配置
2. 编写 Kether 脚本定义奖励逻辑（参考 TabooLib Kether 文档）
3. `RewardManager` 会自动处理新配置的奖励

### 添加新的数据表
1. 在 `database/entity/` 创建实体类（Kotlin data class）
2. 在 `database/table/` 创建表定义（继承 `org.jetbrains.exposed.sql.Table`）
3. 在 `TableFactory.initializeTables()` 中注册新表
4. 在 `DatabaseService` 中添加相关数据操作方法

## 文档资源

项目包含完整的模块文档（`docs/` 目录）:
- `docs/API.md`: API 开发文档
- `docs/bilibilivideo/*/README.md`: 各模块详细说明
- `docs/bilibili-API-collect/*.md`: Bilibili API 集成文档
- `docs/taboolib/*.md`: TabooLib 模块参考

## 外部依赖和仓库

### Maven 仓库
- Maven Central (默认)
- `https://repo.aeoliancloud.com/repository/releases/` (自定义仓库)

### 主要依赖版本
- TabooLib: 6.2.3-2eb93b5
- Kotlin: 2.2.0
- OkHttp3: 4.12.0
- Gson: 2.10.1
- Spigot API: 12004 (mapped & universal)

## 许可证

MIT License - 允许自由使用、修改和分发，但需保留版权声明。

## 联系信息

- 作者: BingZi-233
- 项目地址: https://github.com/BingZi-233/BilibiliVideo
