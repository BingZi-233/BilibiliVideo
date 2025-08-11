# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发环境和构建

### 构建项目
```bash
# 构建发行版本
./gradlew build

# 构建开发版本（包含 TabooLib 本体，用于开发者使用）
./gradlew taboolibBuildApi -PDeleteCode
```

### 测试
由于项目结构简单，主要测试通过构建和运行来验证。如果需要单元测试，可以使用：
```bash
./gradlew test
```

## 项目架构和技术栈

### 技术栈
- **框架**: TabooLib v6.2.3（跨平台Minecraft插件开发框架）
- **语言**: Kotlin（主要）+ Java（目标版本：Java 1.8）
- **构建工具**: Gradle 8.x + Kotlin DSL
- **依赖管理**: TabooLib的模块化系统
- **网络请求**: OkHttp 4.12.0
- **JSON处理**: Gson 2.10.1
- **二维码生成**: ZXing 3.5.3
- **异步处理**: Kotlin Coroutines 1.7.3
- **OneBot集成**: OneBot 1.0.0（QQ机器人通讯）

### 架构概述
这是一个基于TabooLib开发的Minecraft插件，实现了完整的Bilibili API集成功能：

1. **插件主类**: `BilibiliVideo` - 继承自TabooLib的`Plugin`接口，负责插件生命周期管理
2. **核心模块架构**:
   - `commands/` - 命令处理层（登录、测试等命令）
   - `internal.network/` - 网络服务层（API调用、Cookie管理、用户服务）
   - `internal.qrcode/` - 二维码处理层（生成、发送模式、多平台发送器）
   - `internal.network.entity/` - 数据实体层（API响应、用户信息、视频信息等）

3. **关键服务组件**:
   - `BilibiliApiClient` - 核心HTTP客户端
   - `EnhancedLoginService` - 增强登录服务（支持二维码登录）
   - `BilibiliVideoService` - 视频操作服务（获取信息、点赞、投币、收藏）
   - `BilibiliUserService` - 用户服务（个人信息、统计数据）
   - `QRCodeSendService` - 二维码发送服务（支持聊天、地图、OneBot多种模式）

4. **TabooLib模块依赖**: 
   - Basic（基础功能）、I18n（国际化）、Metrics（数据统计）
   - MinecraftChat（聊天组件）、CommandHelper（命令系统）
   - Bukkit系列模块（Bukkit、Kether、BukkitHook、BukkitUtil）

### TabooLib关键概念

#### 生命周期管理
TabooLib使用生命周期阶段管理插件初始化：
- `CONST`: 常量初始化
- `INIT`: 初始化阶段
- `LOAD`: 加载阶段
- `ENABLE`: 启用阶段
- `ACTIVE`: 激活阶段
- `DISABLE`: 禁用阶段

#### 依赖注入系统
使用`@Awake`注解进行自动依赖注入：
```kotlin
@Awake
object SomeManager {
    // 自动在适当的生命周期阶段初始化
}
```

#### 国际化系统
支持基于小驼峰命名的扁平化YAML语言文件：
```yaml
# 正确的格式
messageWelcome: "欢迎！"
errorNotFound: "未找到资源"

# 错误的格式（不要嵌套）
message:
  welcome: "欢迎！"  # ❌ 不要这样做
```

#### 消息发送
使用TabooLib的统一消息发送接口：
```kotlin
// 发送信息消息
player.sendInfo("messageKey", arg1, arg2)
// 发送警告消息  
player.sendWarn("warningKey")
// 发送错误消息
player.sendError("errorKey")
```

## 代码组织规范

### 包结构
- `api/` - 对外提供的API和事件
- `internal/` - 内部功能模块
- 每个功能都应该有自己单独的包名

### 命名规范
- **语言文件**: 使用小驼峰命名（camelCase）
- **类名**: 使用大驼峰命名（PascalCase）
- **方法和变量**: 使用小驼峰命名（camelCase）

### 代码风格
- **注释**: 编写详细的中文注释，便于后续阅读
- **日志**: 避免硬编码消息文本，使用国际化系统
- **异常处理**: 使用TabooLib的错误处理机制

## 开发最佳实践

### TabooLib模块使用
1. **查询DeepWiki**: 遇到不确定的TabooLib功能时，积极使用DeepWiki查询`TabooLib/taboolib`仓库
2. **配置系统**: 使用TabooLib的配置注入系统而不是手动读取配置
3. **命令系统**: 利用CommandHelper模块简化命令处理
4. **事件系统**: 使用TabooLib的跨平台事件系统

### 性能考虑
1. **异步操作**: 网络请求和文件IO操作应使用TabooLib的异步执行器
2. **缓存**: 合理使用缓存减少重复计算
3. **资源管理**: 正确释放资源，避免内存泄漏

### 安全注意事项
1. **输入验证**: 验证用户输入和外部数据
2. **权限检查**: 实现适当的权限检查机制
3. **敏感信息**: 不要在代码中硬编码敏感信息

## 特定于此项目的指导

### 核心功能实现
这是一个完整的Bilibili API集成Minecraft插件，主要功能包括：

1. **登录认证系统**:
   - 二维码登录（支持聊天框、游戏内地图、QQ机器人多种显示方式）
   - Cookie持久化管理
   - 登录会话自动清理

2. **视频交互功能**:
   - 视频信息获取（标题、描述、统计数据等）
   - 三连操作（点赞、投币、收藏）
   - 视频状态查询

3. **用户信息服务**:
   - 个人资料获取
   - 用户统计数据
   - 账号状态查询

4. **多平台二维码发送**:
   - 游戏内聊天框发送
   - 游戏内地图显示
   - OneBot（QQ机器人）发送

### 开发注意事项
1. **异步处理**: 所有网络请求均使用CompletableFuture异步处理，避免阻塞游戏主线程
2. **错误处理**: 使用TabooLib的错误处理机制和国际化消息系统
3. **Cookie管理**: 实现了自定义CookieJar用于B站登录状态管理
4. **定时任务**: 插件启动时自动注册会话清理定时任务
5. **多平台兼容**: 支持通过OneBot与QQ机器人系统集成

### 关键API参考
1. **Bilibili API**: 项目使用官方API，参考文档：https://github.com/SocialSisterYi/bilibili-API-collect
2. **OneBot标准**: QQ机器人通讯协议，源码参考：https://github.com/BingZi-233/OneBot
3. **TabooLib文档**: 开发过程中遇到TabooLib相关问题，使用DeepWiki查询`TabooLib/taboolib`仓库

### 调试和测试
1. 使用TabooLib的调试工具进行问题诊断
2. 在不同Minecraft版本上测试兼容性
3. 测试二维码发送的各种模式（聊天、地图、OneBot）
4. 验证登录会话的正确管理和清理