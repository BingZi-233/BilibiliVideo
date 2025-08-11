# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 开发环境和构建

### 构建项目
```bash
# 构建发行版本
./gradlew build

# 构建开发版本（不含 TabooLib 本体，用于开发者使用）
./gradlewtaboolibBuildApi -PDeleteCode
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

### 架构概述
这是一个基于TabooLib开发的Minecraft插件，使用TabooLib的跨平台架构：

1. **插件主类**: `BilibiliVideo` - 继承自TabooLib的`Plugin`接口
2. **模块依赖**: 
   - Basic模块（基础功能）
   - I18n模块（国际化）
   - Metrics模块（数据统计）
   - MinecraftChat模块（聊天组件）
   - CommandHelper模块（命令系统）
   - Bukkit相关模块（Bukkit、Kether、BukkitHook、BukkitUtil）

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

### 插件功能
根据项目名称，这似乎是一个与哔哩哔哩视频相关的Minecraft插件。开发时应考虑：

1. **网络请求**: 使用TabooLib的异步系统处理API调用
2. **数据缓存**: 合理缓存视频信息减少API调用
3. **用户界面**: 利用TabooLib的UI系统创建用户友好的界面
4. **配置管理**: 支持API密钥、缓存设置等配置项

### 扩展建议
1. 使用TabooLib的数据库模块存储用户数据
2. 利用Kether脚本引擎提供自定义脚本功能
3. 使用Metrics模块收集使用统计
4. 考虑BukkitHook模块与其他插件的兼容性

### 调试和测试
1. 使用TabooLib的调试工具进行问题诊断
2. 在不同Minecraft版本上测试兼容性
3. 使用TabooLib的测试工具验证功能正确性