# BilibiliVideo

[![Gradle](https://img.shields.io/badge/Gradle-8.x-brightgreen.svg)](https://gradle.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-blue.svg)](https://kotlinlang.org)
[![TabooLib](https://img.shields.io/badge/TabooLib-6.2.3-orange.svg)](https://tabooproject.org)
[![Java](https://img.shields.io/badge/Java-1.8%2B-red.svg)](https://openjdk.java.net)

一个功能丰富的Minecraft插件，提供完整的Bilibili API集成功能，包括登录认证、视频互动、奖励系统等。

## 🌟 核心功能

### 🔐 登录认证系统
- **二维码登录**：支持聊天框、游戏内地图、QQ机器人多种显示方式
- **Cookie持久化**：自动管理登录状态和会话
- **多平台支持**：无缝集成OneBot协议的QQ机器人

### 🎬 视频交互功能
- **视频信息获取**：获取视频标题、描述、统计数据等详细信息
- **三连操作**：支持点赞、投币、收藏等交互操作
- **状态查询**：实时查询视频和用户交互状态

### 🎁 智能奖励系统
- **三连奖励**：基于Kether脚本的可配置奖励系统
- **奖励检查**：智能检查玩家奖励资格和条件
- **统计追踪**：完整的奖励记录和统计系统

### 🤖 智能命令建议系统 ✨
- **真实数据驱动**：所有命令参数建议来自实际数据而非示例内容
- **高性能缓存**：5分钟TTL缓存，确保100ms内响应
- **智能过滤**：根据玩家状态和数据有效性过滤建议
- **优雅降级**：数据获取失败时提供配置化的备选建议

### 👥 用户管理
- **绑定系统**：支持Bilibili UID和QQ号码双向绑定
- **UP主监控**：监控指定UP主的视频更新
- **权限管理**：细粒度的权限控制系统

## 🚀 命令使用

### 基础命令
- `/bv` 或 `/bili` 或 `/bilibilivideo` - 主命令

### 子命令列表

#### 登录相关
- `/bv login [模式]` - 登录Bilibili账号
  - 智能建议：显示可用的二维码发送器（聊天、地图、OneBot等）

#### 绑定管理
- `/bv bind qq <QQ号>` - 绑定QQ号码
  - 智能建议：显示最近绑定的真实QQ号码
- `/bv bind bilibili` - 绑定Bilibili账号
- `/bv bind unbind <类型> [目标]` - 解绑操作
  - 智能建议：根据类型显示对应的绑定数据

#### UP主管理
- `/bv uploader add <UID>` - 添加UP主监控
  - 智能建议：显示热门UP主的真实UID
- `/bv uploader remove <UID>` - 移除UP主监控
  - 智能建议：显示已监控的UP主和名称
- `/bv uploader sync <UID>` - 同步UP主视频
  - 智能建议：显示已监控的UP主信息
- `/bv uploader toggle <UID>` - 切换UP主监控状态
  - 智能建议：显示可操作的UP主列表

#### 奖励系统
- `/bv reward claim <BV号>` - 领取视频三连奖励
  - 智能建议：显示玩家可领取奖励的真实视频BV号

#### 信息查询
- `/bv info <BV号>` - 查询视频信息
- `/bv info user [UID]` - 查询用户信息

## ⚙️ 配置说明

### 主要配置文件：`config.yml`

```yaml
# 数据库配置
database:
  enabled: true
  type: "sqlite"  # sqlite 或 mysql

# 登录配置
login:
  qr-code-refresh-interval: 3
  session-cleanup-interval: 10

# 命令建议系统配置
command-suggestion:
  cache-ttl-minutes: 5        # 缓存TTL（分钟）
  max-suggestions: 10         # 最大建议数量
  response-timeout-ms: 100    # 响应超时（毫秒）
  cache-enabled: true         # 是否启用缓存
  
  # 降级示例数据配置
  fallback-examples:
    qq-numbers: ["1234567890", "9876543210"]
    uploader-uids: ["703007996", "546195"]
    bv-numbers: ["BV1xx411c7mD", "BV1yy411c7mE"]

# 奖励系统配置
reward:
  enabled: true
  daily-limit: 3
  require-full-triple: false
  minimum-actions: ["LIKE", "COIN"]
```

## 🛠️ 构建说明

### 构建发行版本

用于生产环境部署，不包含TabooLib本体：

```bash
./gradlew build
```

### 构建开发版本

包含TabooLib本体，用于开发调试：

```bash
./gradlew taboolibBuildApi -PDeleteCode
```

> 参数 `-PDeleteCode` 用于移除逻辑代码以减少体积

### 运行测试

```bash
./gradlew test
```

## 📚 技术架构

### 核心技术栈
- **Kotlin 2.2.0** - 主要开发语言
- **TabooLib 6.2.3** - 跨平台插件开发框架
- **OkHttp 4.12.0** - HTTP客户端
- **Gson 2.10.1** - JSON处理
- **OrmLite 6.1** - 数据库ORM
- **ZXing 3.5.3** - 二维码生成

### 架构特点
- **模块化设计**：清晰的包结构和职责分离
- **异步处理**：使用CompletableFuture和TabooLib异步执行器
- **缓存优化**：智能TTL缓存机制，提升性能
- **事件驱动**：完整的事件系统支持扩展
- **国际化支持**：完整的多语言支持

### 数据库支持
- **SQLite**：默认本地数据库
- **MySQL**：生产环境数据库支持
- **自动迁移**：版本升级时的数据库结构自动迁移

## 🔧 开发指南

### 环境要求
- Java 1.8+
- Kotlin 2.2.0+
- Gradle 8.x
- Minecraft 1.12.2+（推荐1.16+）

### 代码风格
- 详细的中文注释
- 遵循Kotlin编码规范
- 使用TabooLib最佳实践
- 扁平化YAML配置文件

## 📄 许可证

本项目采用 [许可证名称] 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🤝 贡献

欢迎提交Issue和Pull Request来改进这个项目！

## 📞 支持

如有问题或建议，请通过以下方式联系：
- 提交 [GitHub Issues](../../issues)
- 查看 [项目文档](./docs)

---

**注意**：本插件仅用于学习和研究目的，请遵守Bilibili的服务条款和相关法律法规。