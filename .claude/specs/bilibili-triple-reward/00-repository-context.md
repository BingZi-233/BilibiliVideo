# BilibiliVideo 项目全面代码库上下文报告

## 项目概述

### 项目类型和目的
这是一个基于TabooLib开发的Minecraft Bukkit/Spigot/Paper插件，实现了完整的Bilibili API集成功能。项目的主要目的是为Minecraft服务器提供与B站（Bilibili）交互的能力，包括账户登录、视频信息获取、三连操作（点赞、投币、收藏）、用户信息管理等功能。

### 核心特性
- **Bilibili账户登录系统**：支持二维码登录，多种发送方式（游戏内聊天、地图显示、QQ机器人）
- **视频交互功能**：获取视频信息、执行三连操作（点赞、投币、收藏）
- **用户数据管理**：用户信息获取、统计数据查询、账户状态监控
- **多平台集成**：支持OneBot协议与QQ机器人系统集成
- **数据库支持**：支持SQLite和MySQL数据库存储用户绑定信息
- **UP主监控**：自动监控UP主视频更新，定期同步视频数据

## 技术栈摘要

### 核心框架和语言
- **开发语言**: Kotlin（主要）+ Java（Java 1.8目标版本）
- **插件框架**: TabooLib v6.2.3-ee81cb0（跨平台Minecraft插件开发框架）
- **构建工具**: Gradle 8.x + Kotlin DSL
- **最低Java版本**: Java 1.8
- **支持平台**: Bukkit/Spigot/Paper 1.8-1.21+

### 依赖库详细信息
#### 网络和HTTP
- **OkHttp**: v5.0.0 - 现代HTTP客户端
- **Gson**: v2.10.1 - JSON序列化/反序列化

#### 二维码处理
- **ZXing Core**: v3.5.3 - 二维码生成核心库
- **ZXing JavaSE**: v3.5.3 - Java SE环境支持

#### 数据库ORM
- **OrmLite Core**: v6.1 - 轻量级ORM框架
- **OrmLite JDBC**: v6.1 - JDBC连接支持
- **SQLite JDBC**: v3.45.1.0 - SQLite数据库驱动
- **MySQL Connector**: v8.3.0 - MySQL数据库驱动

#### 第三方集成
- **OneBot**: v1.0.0-97bdc38 - QQ机器人协议支持

#### TabooLib模块
- **Basic**: 基础功能模块
- **I18n**: 国际化支持模块
- **Metrics**: 数据统计模块
- **MinecraftChat**: 聊天组件模块
- **CommandHelper**: 命令系统模块
- **Bukkit**: Bukkit平台支持
- **Kether**: 脚本引擎支持
- **BukkitHook**: Bukkit钩子模块
- **BukkitUtil**: Bukkit实用工具模块

## 代码组织模式

### 目录结构架构
项目采用标准的Maven/Gradle项目结构，源码位于`src/main/kotlin`下，包结构清晰：

```
online.bingzi.bilibili.video/
├── BilibiliVideo.kt                    # 插件主类
├── api/                                # 对外API和事件定义
│   ├── event/                         # 事件系统
│   │   ├── database/                  # 数据库相关事件
│   │   ├── network/                   # 网络相关事件
│   │   └── qrcode/                    # 二维码相关事件
│   └── qrcode/                        # 二维码API
└── internal/                          # 内部实现模块
    ├── binding/                       # 自动绑定服务
    ├── cache/                         # 缓存系统
    ├── commands/                      # 命令处理系统
    ├── config/                        # 配置管理
    ├── database/                      # 数据库服务层
    ├── network/                       # 网络服务层
    ├── qrcode/                        # 二维码处理系统
    ├── scheduler/                     # 定时任务调度器
    └── verification/                  # 验证服务
```

### 分层架构设计
1. **API层** (`api/`): 对外提供的事件和接口
2. **服务层** (`internal/`): 核心业务逻辑实现
3. **数据访问层** (`database/dao/`): 数据库访问对象
4. **网络层** (`network/`): HTTP API调用和响应处理
5. **表示层** (`commands/`): 命令处理和用户交互

### 模块化设计
每个功能模块都有独立的包空间：
- **网络模块**: 处理所有HTTP请求和Bilibili API调用
- **数据库模块**: 处理数据持久化和ORM操作
- **二维码模块**: 处理二维码生成和多平台发送
- **命令模块**: 处理所有用户命令和交互
- **配置模块**: 处理插件配置和参数管理

## 编码标准和约定

### 命名规范
- **类名**: 使用大驼峰命名（PascalCase）
- **方法和变量**: 使用小驼峰命名（camelCase）
- **包名**: 全小写，使用点分隔
- **语言文件**: 使用小驼峰命名，扁平化YAML结构（不嵌套）

### 代码风格约定
- **注释**: 编写详细的中文注释，便于后续维护
- **异步处理**: 网络请求统一使用CompletableFuture异步处理
- **错误处理**: 使用TabooLib的sendInfo/sendWarn/sendError进行国际化消息发送
- **资源管理**: 正确使用try-with-resources和CompletableFuture处理资源
- **线程安全**: 注意Cookie管理和数据库操作的线程安全

### 设计模式使用
- **单例模式**: 服务类多使用object单例
- **工厂模式**: ServiceFactory用于创建数据库服务
- **观察者模式**: 基于TabooLib事件系统的事件驱动架构
- **策略模式**: 二维码发送器的注册机制
- **依赖注入**: 使用TabooLib的@Awake注解自动依赖注入

## API结构和集成点

### Bilibili API集成
项目集成了完整的Bilibili API，参考[bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)：

#### 认证API
- **二维码登录**: `/qrcode/generate` 和 `/qrcode/poll`
- **Cookie管理**: 自定义CookieJar实现B站登录状态管理
- **WBI签名**: 实现B站最新的WBI参数签名机制

#### 用户API
- **用户信息**: `/x/space/wbi/acc/info` - 获取用户基本信息
- **用户统计**: `/x/relation/stat` - 获取关注、粉丝等统计
- **用户详情**: 扩展的用户信息获取

#### 视频API
- **视频信息**: `/x/web-interface/view` - 根据BV号获取视频详情
- **视频统计**: 播放量、点赞数、投币数、收藏数等
- **三连操作**: 点赞(`/x/web-interface/archive/like`)、投币(`/x/web-interface/coin/add`)、收藏(`/x/v3/fav/resource/deal`)
- **视频评论**: `/x/v2/reply/wbi/main` - 获取视频评论，支持WBI签名

### 数据库API
使用OrmLite框架，支持多种数据库：
- **SQLite**: 默认数据库，适合单机部署
- **MySQL**: 支持集群部署，数据共享

#### 实体类
- `Player`: 玩家基础信息
- `QQBinding`: QQ号绑定信息
- `BilibiliBinding`: Bilibili账户绑定信息
- `BilibiliCookie`: 登录状态Cookie信息
- `UploaderVideo`: UP主视频监控数据

#### 服务类
- `PlayerDataService`: 玩家数据CRUD操作
- `PlayerQQBindingService`: QQ绑定服务
- `BilibiliBindingDaoService`: B站绑定DAO服务

### 事件系统API
基于TabooLib事件系统的完整事件定义：

#### 网络事件
- `LoginSuccessEvent`: 登录成功事件
- `VideoInfoFetchEvent`: 视频信息获取事件
- `ApiRequestFailureEvent`: API请求失败事件

#### 数据库事件
- `PlayerDataCreateEvent`: 玩家数据创建事件
- `BilibiliBindingCreateEvent`: B站绑定创建事件

### 二维码系统API
模块化的二维码发送系统：
- `QRCodeSender`: 二维码发送器接口
- `QRCodeSenderRegistry`: 发送器注册中心
- 支持的发送器：聊天框、OneBot（QQ机器人）

## 开发工作流

### Git工作流
- **主分支**: `master` - 稳定发布版本
- **提交规范**: 使用约定式提交格式
  - `feat:` - 新功能
  - `fix:` - 修复
  - `docs:` - 文档
  - `refactor:` - 重构

### CI/CD管道
完整的GitHub Actions工作流（`.github/workflows/build.yml`）：

#### 构建流程
1. **环境设置**: Java 8 + Gradle 8.14.3
2. **依赖缓存**: Gradle依赖和Kotlin DSL缓存
3. **版本管理**: 基于Git哈希的自动版本号生成
4. **双重构建**:
   - 用户版本：完整功能版本
   - API版本：用于开发者集成的API版本

#### 质量检测
- **Qodana扫描**: JetBrains代码质量检测
- **SARIF上传**: 将结果集成到GitHub Security

#### 自动发布
- **发布条件**: master分支推送触发
- **Release生成**: 自动创建GitHub Release
- **更新日志**: 基于Git提交历史自动生成
- **多文件上传**: 同时发布用户版和API版构建产物

### 构建命令
```bash
# 构建发行版本（不含TabooLib本体）
./gradlew build

# 构建开发版本（包含TabooLib本体，用于开发）
./gradlew taboolibBuildApi -PDeleteCode

# 测试（可选）
./gradlew test
```

## 配置系统

### 主要配置文件
- `config.yml`: 插件主配置
- `database.yml`: 数据库配置
- `lang/zh_CN.yml`: 中文语言包

### 配置特点
- **国际化支持**: 完整的中文语言包，387条消息
- **二维码配置**: 支持多种发送模式配置
- **登录配置**: 超时时间、轮询间隔等参数可配置
- **数据库配置**: 支持SQLite和MySQL配置切换

## 潜在约束和考虑因素

### 技术约束
1. **Java版本**: 必须兼容Java 1.8
2. **Minecraft版本**: 支持1.8-1.21+的广泛版本兼容性
3. **TabooLib依赖**: 紧密依赖TabooLib框架，升级需谨慎
4. **Bilibili API限制**: 需要遵守B站API调用频率限制

### 安全考虑
1. **Cookie安全**: 加密存储用户登录凭证
2. **输入验证**: 对所有用户输入进行严格验证
3. **权限控制**: 管理员命令需要适当的权限检查
4. **依赖安全**: 使用了依赖重定位避免冲突

### 性能考虑
1. **异步处理**: 所有网络请求使用异步模式避免阻塞主线程
2. **缓存策略**: WBI密钥缓存、用户信息缓存等
3. **数据库优化**: 使用连接池和ORM优化数据库访问
4. **内存管理**: 及时清理过期会话和缓存数据

### 维护性考虑
1. **国际化**: 完整的中文语言支持，易于多语言扩展
2. **模块化**: 清晰的模块划分，便于功能扩展
3. **事件驱动**: 基于事件的松耦合架构
4. **文档完整**: 详细的中文注释和配置说明

## 新功能集成建议

### 三连奖励系统集成点
基于现有架构，建议的集成点：

1. **奖励配置模块**: 在`internal/config/`下添加奖励配置管理
2. **奖励执行器**: 在`internal/rewards/`下实现奖励分发逻辑
3. **监控服务**: 扩展现有的UP主监控系统，添加三连检测
4. **数据库扩展**: 在现有实体基础上添加奖励记录表
5. **命令扩展**: 在现有命令系统基础上添加奖励管理命令
6. **事件集成**: 利用现有事件系统添加奖励相关事件

### 推荐的开发路径
1. 熟悉现有的视频服务(`BilibiliVideoService`)和三连操作API
2. 了解UP主监控系统(`UploaderVideoScheduler`)的工作机制
3. 扩展数据库实体，添加奖励配置和记录表
4. 实现奖励检测和分发逻辑
5. 集成到现有的命令和配置系统中
6. 添加相应的事件和国际化消息

这个项目具有良好的架构基础和完整的Bilibili API集成，为实现三连奖励系统提供了坚实的技术底座。