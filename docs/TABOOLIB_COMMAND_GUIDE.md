# TabooLib 命令系统完整指南

## 概述

TabooLib是一个强大的跨平台Minecraft插件开发框架，使用Kotlin语言开发。它提供了一套现代化的命令系统，通过注解驱动的方式简化命令的注册和处理，大大提高了开发效率。

## 核心概念

### 注解驱动设计
TabooLib命令系统采用注解驱动的设计模式，主要使用两个核心注解：
- `@CommandHeader`: 定义主命令的基本信息
- `@CommandBody`: 定义子命令和执行体

### 类型安全
支持强类型的执行上下文，包括：
- `CommandSender`: 通用命令发送者（控制台或玩家）
- `Player`: 仅限玩家执行
- `ProxyPlayer`: TabooLib的跨平台玩家对象

## @CommandHeader 注解详解

### 基本语法
```kotlin
@CommandHeader(
    name = "mycommand",           // 主命令名称（必填）
    aliases = ["mc", "cmd"],      // 命令别名数组（可选）
    description = "示例命令",      // 命令描述（可选）
    permission = "myplugin.use"   // 执行权限（可选）
)
object MyCommand {
    // 命令实现
}
```

### 参数说明
- **name**: String - 主命令名称，必填参数
- **aliases**: Array<String> - 命令别名，允许多个别名
- **description**: String - 命令描述，用于帮助信息生成
- **permission**: String - 基础执行权限

### 最佳实践
```kotlin
@CommandHeader(
    name = "bilibilipro", 
    aliases = ["bvp", "bilibili"], 
    description = "BilibiliVideoPro插件主命令 - 提供Bilibili账户绑定、视频交互验证等功能",
    permission = "bilibilipro.use"
)
object BilibiliVideoProCommand {
    // 实现内容
}
```

## @CommandBody 注解详解

### 基本语法
```kotlin
@CommandBody(
    aliases = ["bind", "connect"],     // 子命令别名（可选）
    permission = "bilibilipro.login",  // 子命令权限（可选）
    optional = true,                   // 可选参数标记（可选）
    hidden = false                     // 是否隐藏命令（可选）
)
val login = subCommand {
    // 子命令实现
}
```

### 参数说明
- **aliases**: Array<String> - 子命令别名数组
- **permission**: String - 执行权限，会覆盖@CommandHeader中的权限
- **optional**: Boolean - 是否为可选命令
- **hidden**: Boolean - 是否在帮助中隐藏该命令

### 权限层级设计
```kotlin
// 基础权限
@CommandBody
val info = subCommand { /* 继承@CommandHeader权限 */ }

// 特定权限
@CommandBody(permission = "bilibilipro.admin")
val admin = subCommand { /* 需要管理员权限 */ }

// 隐藏命令（调试用）
@CommandBody(permission = "bilibilipro.debug", hidden = true)
val debug = subCommand { /* 隐藏的调试命令 */ }
```

## 命令构建器方法

### mainCommand - 主命令处理器
```kotlin
@CommandBody
val main = mainCommand {
    execute<CommandSender> { sender, context, _ ->
        // 当用户只输入主命令时执行
        sender.sendMessage("欢迎使用插件！")
        showHelpInfo(sender)
    }
}
```

### subCommand - 子命令处理器
```kotlin
@CommandBody(aliases = ["bind", "connect"])
val login = subCommand {
    execute<Player> { player, context, _ ->
        // 处理登录逻辑
        startLoginProcess(player)
    }
}
```

### dynamic - 动态参数处理
```kotlin
val check = subCommand {
    dynamic("bvid") {
        // 参数建议补全
        suggest<Player> { sender, context ->
            // 返回BV号格式示例
            listOf("BV1xx411c7mD", "BV1yy411c7mE")
        }
        
        // 参数执行逻辑
        execute<Player> { player, context, bvid ->
            // bvid是用户输入的动态参数
            checkVideoStatus(player, bvid)
        }
    }
}
```

### literal - 字面量参数
```kotlin
val admin = subCommand {
    literal("reload") {
        execute<CommandSender> { sender, context, _ ->
            // 处理 /command admin reload
            reloadPlugin(sender)
        }
    }
    
    literal("status") {
        execute<CommandSender> { sender, context, _ ->
            // 处理 /command admin status
            showSystemStatus(sender)
        }
    }
}
```

### 复合参数处理
```kotlin
val manage = subCommand {
    dynamic("player") {
        suggest<CommandSender> { _, _ ->
            // 在线玩家名补全
            Bukkit.getOnlinePlayers().map { it.name }
        }
        
        dynamic("action") {
            suggest<CommandSender> { _, _ ->
                listOf("ban", "kick", "mute", "warn")
            }
            
            execute<CommandSender> { sender, context, action ->
                val playerName = context.argument(-1) // 获取前一个参数
                executePlayerAction(sender, playerName, action)
            }
        }
        
        execute<CommandSender> { sender, context, playerName ->
            // 只提供玩家名时的默认处理
            showPlayerInfo(sender, playerName)
        }
    }
}
```

## 高级功能

### 参数建议系统
```kotlin
dynamic("theme") {
    suggest<CommandSender> { sender, context ->
        // 动态生成建议列表
        when {
            sender.hasPermission("admin") -> listOf("default", "dark", "light", "custom")
            else -> listOf("default", "dark", "light")
        }
    }
    execute<CommandSender> { sender, context, theme ->
        switchTheme(sender, theme)
    }
}
```

### 可选参数处理
```kotlin
val teleport = subCommand {
    dynamic("target") {
        suggest<Player> { _, _ ->
            Bukkit.getOnlinePlayers().map { it.name }
        }
        
        // 可选的坐标参数
        dynamic("x", optional = true) {
            dynamic("y", optional = true) {
                dynamic("z", optional = true) {
                    execute<Player> { player, context, z ->
                        val target = context.argument(-3)
                        val x = context.argument(-2).toDoubleOrNull()
                        val y = context.argument(-1).toDoubleOrNull()
                        val zCoord = z.toDoubleOrNull()
                        
                        if (x != null && y != null && zCoord != null) {
                            teleportToCoordinates(player, x, y, zCoord)
                        } else {
                            teleportToPlayer(player, target)
                        }
                    }
                }
            }
        }
        
        execute<Player> { player, context, target ->
            // 仅传送到玩家
            teleportToPlayer(player, target)
        }
    }
}
```

### 权限集成和验证
```kotlin
@CommandBody(permission = "bilibilipro.admin")
val admin = subCommand {
    execute<CommandSender> { sender, context, _ ->
        // 注解权限会自动验证，无需手动检查
        // TabooLib会在执行前自动验证权限
        
        // 额外的权限检查（如果需要）
        if (sender.hasPermission("bilibilipro.admin.advanced")) {
            showAdvancedOptions(sender)
        } else {
            showBasicOptions(sender)
        }
    }
}
```

## 异步处理模式

### 基本异步模式
```kotlin
execute<Player> { player, context, argument ->
    submit(async = true) {
        // 异步操作（网络请求、数据库操作等）
        val result = performNetworkRequest(argument)
        
        submit(async = false) {
            // 回到主线程执行UI更新
            player.sendMessage("操作完成: $result")
        }
    }
}
```

### 复杂异步链
```kotlin
execute<Player> { player, context, bvid ->
    submit(async = true) {
        try {
            // 第一步：验证参数
            val validation = validateBvid(bvid)
            if (!validation.isValid) {
                submit(async = false) {
                    player.sendError("参数错误: ${validation.message}")
                }
                return@submit
            }
            
            // 第二步：网络请求
            val videoInfo = fetchVideoInfo(bvid)
            
            // 第三步：数据库操作
            val interactionStatus = checkInteractionStatus(player.uniqueId, bvid)
            
            submit(async = false) {
                // 第四步：UI更新
                displayResults(player, videoInfo, interactionStatus)
            }
            
        } catch (e: Exception) {
            submit(async = false) {
                player.sendError("操作失败: ${e.message}")
            }
        }
    }
}
```

## 错误处理和验证

### 输入验证模式
```kotlin
execute<Player> { player, context, input ->
    submit(async = true) {
        // 参数验证
        val validationResult = InputValidator.validate(input) {
            notBlank("输入不能为空")
            matches(Regex("BV[0-9a-zA-Z]+")) { "BV号格式不正确" }
            length(12) { "BV号长度必须为12位" }
        }
        
        if (!validationResult.isValid) {
            submit(async = false) {
                player.sendError("验证失败", validationResult.errors.joinToString(", "))
            }
            return@submit
        }
        
        // 处理有效输入
        processValidInput(player, input)
    }
}
```

### 异常处理模式
```kotlin
execute<Player> { player, context, argument ->
    submit(async = true) {
        try {
            val result = riskyOperation(argument)
            
            submit(async = false) {
                player.sendInfo("操作成功", result.toString())
            }
            
        } catch (e: NetworkException) {
            submit(async = false) {
                player.sendError("网络错误", "请检查网络连接")
            }
        } catch (e: ValidationException) {
            submit(async = false) {
                player.sendError("参数错误", e.message ?: "未知错误")
            }
        } catch (e: Exception) {
            submit(async = false) {
                player.sendError("系统错误", "请联系管理员")
            }
            // 记录详细错误日志
            logger.error("Command execution failed", e)
        }
    }
}
```

## 完整示例

### 完整的命令类实现
```kotlin
@CommandHeader(
    name = "bilibilipro",
    aliases = ["bvp", "bilibili"], 
    description = "BilibiliVideoPro插件主命令 - 提供Bilibili账户绑定、视频交互验证等功能",
    permission = "bilibilipro.use"
)
object BilibiliVideoProCommand {

    @Config("config.yml")
    lateinit var config: Configuration

    // 主命令 - 显示帮助
    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendInfo("commandHelpTitle")
            // 显示可用子命令列表
            showAvailableCommands(sender)
        }
    }

    // 登录命令
    @CommandBody(
        aliases = ["bind", "connect"],
        permission = "bilibilipro.login"
    )
    val login = subCommand {
        execute<Player> { player, _, _ ->
            submit(async = true) {
                startLoginProcess(player)
            }
        }
    }

    // 检查视频状态
    @CommandBody(aliases = ["verify", "validate"])
    val check = subCommand {
        dynamic("bvid") {
            suggest<Player> { _, _ ->
                listOf("BV1xx411c7mD") // BV号格式示例
            }
            execute<Player> { player, _, bvid ->
                submit(async = true) {
                    checkVideoInteraction(player, bvid)
                }
            }
        }
    }

    // 状态查询
    @CommandBody(aliases = ["stat", "profile"])
    val status = subCommand {
        execute<Player> { player, _, _ ->
            submit(async = true) {
                showPlayerStatus(player)
            }
        }
    }

    // 管理员命令
    @CommandBody(permission = "bilibilipro.admin")
    val admin = subCommand {
        literal("reload") {
            execute<CommandSender> { sender, _, _ ->
                submit(async = true) {
                    reloadPlugin(sender)
                }
            }
        }
        
        literal("status") {
            execute<CommandSender> { sender, _, _ ->
                submit(async = true) {
                    showSystemStatus(sender)
                }
            }
        }
        
        literal("unbind") {
            dynamic("player") {
                suggest<CommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                execute<CommandSender> { sender, _, playerName ->
                    submit(async = true) {
                        unbindPlayer(sender, playerName)
                    }
                }
            }
        }
    }

    // 帮助命令
    @CommandBody(aliases = ["?", "h"], optional = true)
    val help = subCommand {
        execute<CommandSender> { sender, _, _ ->
            showDetailedHelp(sender)
        }
        
        literal("admin") {
            execute<CommandSender> { sender, _, _ ->
                if (sender.hasPermission("bilibilipro.admin")) {
                    showAdminHelp(sender)
                } else {
                    sender.sendError("noPermission")
                }
            }
        }
    }

    // 隐藏的调试命令
    @CommandBody(permission = "bilibilipro.debug", hidden = true)
    val debug = subCommand {
        literal("cache") {
            literal("clear") {
                execute<CommandSender> { sender, _, _ ->
                    clearAllCaches()
                    sender.sendInfo("缓存已清空")
                }
            }
            literal("stats") {
                execute<CommandSender> { sender, _, _ ->
                    showCacheStatistics(sender)
                }
            }
        }
        
        literal("network") {
            literal("test") {
                execute<CommandSender> { sender, _, _ ->
                    submit(async = true) {
                        testNetworkConnectivity(sender)
                    }
                }
            }
        }
    }

    // 私有辅助方法
    private fun startLoginProcess(player: Player) {
        // 登录流程实现
    }
    
    private fun checkVideoInteraction(player: Player, bvid: String) {
        // 视频交互检查实现
    }
    
    private fun showPlayerStatus(player: Player) {
        // 状态显示实现
    }
    
    // 其他辅助方法...
}
```

## 最佳实践建议

### 1. 命令组织结构
- **单一职责**: 每个子命令只负责一个功能
- **层次清晰**: 使用合理的命令层次结构
- **权限分离**: 不同权限级别的命令分开定义

### 2. 参数处理
- **输入验证**: 始终验证用户输入
- **错误处理**: 提供清晰的错误信息
- **补全支持**: 为动态参数提供合理的建议

### 3. 异步设计
- **网络操作**: 所有网络请求必须异步执行
- **数据库操作**: 数据库查询和更新异步执行
- **UI更新**: 结果显示回到主线程执行

### 4. 用户体验
- **反馈及时**: 长时间操作提供进度反馈
- **错误友好**: 错误信息要用户可理解
- **帮助完整**: 提供详细的帮助信息

### 5. 代码维护
- **注释清晰**: 复杂逻辑添加注释
- **方法分离**: 将复杂逻辑抽取为私有方法
- **配置外置**: 可配置的参数放入配置文件

## 常见问题解答

### Q: 如何处理可选参数？
A: 使用嵌套的dynamic块，并在每个层级提供execute处理器：

```kotlin
dynamic("required_param") {
    dynamic("optional_param", optional = true) {
        execute<Player> { player, context, optional ->
            val required = context.argument(-1)
            handleWithOptional(player, required, optional)
        }
    }
    execute<Player> { player, context, required ->
        handleWithoutOptional(player, required)
    }
}
```

### Q: 如何实现命令冷却？
A: 结合缓存管理系统实现：

```kotlin
execute<Player> { player, _, _ ->
    if (CacheManager.isOnCooldown(player.uniqueId, "command_name")) {
        val remaining = CacheManager.getCooldownRemaining(player.uniqueId, "command_name")
        player.sendWarn("命令冷却中，剩余时间: ${remaining}秒")
        return@execute
    }
    
    // 设置冷却
    CacheManager.setCooldown(player.uniqueId, "command_name", 30) // 30秒冷却
    
    // 执行命令逻辑
    processCommand(player)
}
```

### Q: 如何处理权限层级？
A: 使用多层权限检查：

```kotlin
@CommandBody(permission = "bilibilipro.admin") // 基础管理员权限
val admin = subCommand {
    literal("basic") {
        execute<CommandSender> { sender, _, _ ->
            // 所有管理员都可以执行
        }
    }
    
    literal("advanced") {
        execute<CommandSender> { sender, _, _ ->
            if (sender.hasPermission("bilibilipro.admin.advanced")) {
                // 高级管理员功能
            } else {
                sender.sendError("需要高级管理员权限")
            }
        }
    }
}
```

### Q: 如何集成国际化？
A: 使用TabooLib的语言系统：

```kotlin
execute<Player> { player, _, _ ->
    // 使用语言键值
    player.sendInfo("welcome.message")
    player.sendInfo("status.info", arg1, arg2)
    
    // 获取本地化文本
    val localizedText = player.asLangText("complex.message", "defaultValue")
}
```

## 导入依赖

使用TabooLib命令系统需要导入以下包：

```kotlin
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.lang.asLangText
import taboolib.platform.util.sendInfo
import taboolib.platform.util.sendError
import taboolib.platform.util.sendWarn
```

## 总结

TabooLib命令系统为Minecraft插件开发提供了现代化、高效的解决方案。其主要优势包括：

1. **注解驱动**: 简化命令定义和注册过程
2. **类型安全**: 编译时类型检查，减少运行时错误
3. **异步友好**: 内置异步处理机制，避免服务器卡顿
4. **权限集成**: 原生支持Minecraft权限系统
5. **参数补全**: 提供良好的用户交互体验
6. **跨平台支持**: 同一套代码支持多个Minecraft平台
7. **DSL语法**: 使用Kotlin DSL，代码简洁易读

通过遵循本指南的最佳实践，可以构建出高质量、用户友好的Minecraft插件命令系统。