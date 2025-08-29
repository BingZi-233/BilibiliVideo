# TabooLib Command 模块使用指南

## 概述

TabooLib Command 模块提供了强大而灵活的命令系统，支持两种主要的命令定义方式：
1. **DSL方式** - 使用 `command {}` 函数式编程风格
2. **注解方式** - 使用 `@CommandHeader` 和 `@CommandBody` 注解

本文档将详细介绍这两种方式的使用方法和最佳实践。

## 安装配置

在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(CommandHelper)
        install(Bukkit) // 需要与平台模块一起使用
    }
}
```

## 注解详解

### @CommandHeader 注解参数

`@CommandHeader` 用于定义主命令，支持以下参数：

```kotlin
@CommandHeader(
    name = "命令名称",              // 必填：主命令名称
    aliases = ["别名1", "别名2"],   // 可选：命令别名数组
    description = "命令描述",        // 可选：命令描述信息
    usage = "使用方法",             // 可选：命令使用方法
    permission = "权限节点",         // 可选：执行权限
    permissionMessage = "权限提示",   // 可选：无权限时的提示消息
    permissionDefault = PermissionDefault.OP,  // 可选：默认权限级别
    newParser = false              // 可选：是否使用新解析器
)
```

### PermissionDefault 枚举值

```kotlin
enum class PermissionDefault {
    TRUE,    // 所有玩家默认拥有权限
    FALSE,   // 所有玩家默认没有权限
    OP,      // 仅OP默认拥有权限
    NOT_OP   // 仅非OP默认拥有权限
}
```

### @CommandBody 注解参数

`@CommandBody` 用于定义子命令，支持以下参数：

```kotlin
@CommandBody(
    aliases = ["别名1", "别名2"],    // 可选：子命令别名数组
    optional = false,              // 可选：是否为可选参数
    permission = "权限节点",         // 可选：执行权限
    permissionDefault = PermissionDefault.OP,  // 可选：默认权限级别
    hidden = false                 // 可选：是否在帮助中隐藏
)
```

## 1. DSL方式 - command {} 函数

### 基本用法

```kotlin
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.command

// 简单命令
command("about", description = "关于此服务器") {
    execute<ProxyCommandSender> { sender, context, argument ->
        sender.sendMessage("§a服务器信息")
    }
}
```

### 带别名的命令

```kotlin
command("help", aliases = listOf("?", "h"), description = "显示帮助信息") {
    execute<ProxyCommandSender> { sender, context, argument ->
        sender.sendMessage("§e=== 帮助信息 ===")
        sender.sendMessage("§7使用命令获得帮助")
    }
}
```

## 2. 动态参数和字面量参数

### literal() - 字面量参数

字面量参数是固定的子命令或选项：

```kotlin
command("admin") {
    literal("reload") {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§a重载配置中...")
        }
    }
    
    literal("info") {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§e插件信息显示")
        }
    }
}
```

### dynamic() - 动态参数

动态参数允许用户输入自定义值：

```kotlin
command("teleport") {
    dynamic("player") {
        suggestion<ProxyCommandSender> { sender, context ->
            // 返回在线玩家列表
            Bukkit.getOnlinePlayers().map { it.name }
        }
        
        restrict<ProxyCommandSender> { sender, context, argument ->
            // 验证玩家是否在线
            Bukkit.getPlayer(argument) != null
        }
        
        execute<ProxyCommandSender> { sender, context, argument ->
            val targetPlayer = Bukkit.getPlayer(argument)
            if (sender is Player && targetPlayer != null) {
                sender.teleport(targetPlayer.location)
                sender.sendMessage("§a已传送到 ${targetPlayer.name}")
            }
        }
    }
}
```

### 嵌套动态参数

```kotlin
command("give") {
    dynamic("player") {
        suggestion<ProxyCommandSender> { sender, context ->
            Bukkit.getOnlinePlayers().map { it.name }
        }
        
        dynamic("item") {
            suggestion<ProxyCommandSender> { sender, context ->
                listOf("diamond", "iron_ingot", "gold_ingot", "stone")
            }
            
            dynamic("amount", optional = true) {
                restrict<ProxyCommandSender> { sender, context, argument ->
                    argument.toIntOrNull()?.let { it > 0 && it <= 64 } ?: false
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val player = context.arg(0)  // 第一个参数
                    val item = context.arg(1)    // 第二个参数
                    val amount = argument?.toIntOrNull() ?: 1
                    
                    sender.sendMessage("§a给予 $player $amount 个 $item")
                }
            }
            
            // 没有数量参数时的默认处理
            execute<ProxyCommandSender> { sender, context, argument ->
                val player = context.arg(0)
                val item = context.arg(1)
                sender.sendMessage("§a给予 $player 1 个 $item")
            }
        }
    }
}
```

## 3. 注解方式 - @CommandHeader 和 @CommandBody

### 基本命令定义

```kotlin
import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender

@CommandHeader(
    name = "myplugin",
    aliases = ["mp"],
    description = "插件主命令",
    usage = "/myplugin <subcommand>",
    permission = "myplugin.use",
    permissionMessage = "§c你没有权限使用此命令！",
    permissionDefault = PermissionDefault.OP
)
object MyPluginCommand {
    
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§c请使用子命令！")
        }
    }
}
```

### 子命令定义

```kotlin
@CommandHeader(name = "admin", permission = "myplugin.admin")
object AdminCommand {
    
    @CommandBody(aliases = ["reload"], permission = "myplugin.admin.reload")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§a配置已重载！")
        }
    }
    
    @CommandBody(aliases = ["info"])
    val info = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§e插件信息显示")
        }
    }
    
    @CommandBody(aliases = ["give"])
    val give = subCommand {
        dynamic("player") {
            suggestion<ProxyCommandSender> { sender, context ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            dynamic("item") {
                suggestion<ProxyCommandSender> { sender, context ->
                    listOf("diamond", "iron_ingot", "gold_ingot")
                }
                
                dynamic("amount", optional = true) {
                    restrict<ProxyCommandSender> { sender, context, argument ->
                        argument.toIntOrNull()?.let { it > 0 } ?: false
                    }
                    
                    execute<ProxyCommandSender> { sender, context, argument ->
                        val player = context.arg(0)
                        val item = context.arg(1)
                        val amount = argument?.toIntOrNull() ?: 1
                        
                        sender.sendMessage("§a给予 $player $amount 个 $item")
                    }
                }
            }
        }
    }
}
```

## 4. CommandContext 详细API

`CommandContext` 提供命令执行时的上下文信息：

```kotlin
execute<ProxyCommandSender> { sender, context, argument ->
    // 获取命令发送者
    val commandSender = context.sender
    
    // 获取命令名称
    val commandName = context.name
    
    // 获取所有原始参数
    val allArgs = context.realArgs
    
    // 获取指定位置的参数
    val firstArg = context.arg(0)   // 第一个参数
    val secondArg = context.arg(1)  // 第二个参数
    
    // 获取当前处理的参数索引
    val currentIndex = context.index
    
    sender.sendMessage("命令: $commandName")
    sender.sendMessage("参数: ${allArgs.joinToString(" ")}")
    sender.sendMessage("当前参数: $argument")
}
```

## 5. 权限系统

### DSL方式权限控制

```kotlin
command("admin") {
    // 在execute中手动检查权限
    execute<ProxyCommandSender> { sender, context, argument ->
        if (!sender.hasPermission("myplugin.admin")) {
            sender.sendMessage("§c权限不足！")
            return@execute
        }
        sender.sendMessage("§a管理员命令执行")
    }
}
```

### 注解方式权限控制

```kotlin
@CommandHeader(
    name = "admin",
    permission = "myplugin.admin",
    permissionMessage = "§c你没有权限使用此命令！"
)
object AdminCommand {
    
    @CommandBody(permission = "myplugin.admin.reload")
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§a配置已重载！")
        }
    }
}
```

## 6. 命令补全系统

### 基本补全

```kotlin
command("gamemode") {
    dynamic("mode") {
        suggestion<ProxyCommandSender> { sender, context ->
            listOf("survival", "creative", "adventure", "spectator")
        }
        
        dynamic("player", optional = true) {
            suggestion<ProxyCommandSender> { sender, context ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            execute<ProxyCommandSender> { sender, context, argument ->
                val mode = context.arg(0)
                val targetPlayer = argument ?: (sender as? Player)?.name ?: return@execute
                
                sender.sendMessage("§a设置 $targetPlayer 的游戏模式为 $mode")
            }
        }
        
        execute<ProxyCommandSender> { sender, context, argument ->
            val mode = context.arg(0)
            if (sender is Player) {
                sender.sendMessage("§a你的游戏模式已设置为 $mode")
            }
        }
    }
}
```

### 条件补全

```kotlin
command("economy") {
    literal("give") {
        dynamic("player") {
            suggestion<ProxyCommandSender> { sender, context ->
                if (sender.hasPermission("economy.give")) {
                    Bukkit.getOnlinePlayers().map { it.name }
                } else {
                    emptyList()
                }
            }
            
            dynamic("amount") {
                suggestion<ProxyCommandSender> { sender, context ->
                    listOf("100", "500", "1000", "5000")
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val player = context.arg(0)
                    val amount = argument?.toDoubleOrNull() ?: return@execute
                    
                    sender.sendMessage("§a给予 $player $amount 金币")
                }
            }
        }
    }
}
```

## 7. 异步命令处理

TabooLib 提供了多种异步处理方式。详细的异步处理指南请参考 [TabooLib Chain 任务链使用指南](TabooLibChain.md)。

### 基本异步处理

```kotlin
import taboolib.common.platform.function.submit

command("database") {
    literal("query") {
        dynamic("table") {
            execute<ProxyCommandSender> { sender, context, argument ->
                val tableName = argument ?: return@execute
                
                sender.sendMessage("§e正在查询数据库表: $tableName")
                
                // 异步执行数据库操作
                submit(async = true) {
                    // 模拟数据库查询
                    Thread.sleep(2000)
                    val result = "查询结果数据"
                    
                    // 回到主线程发送结果
                    submit(async = false) {
                        sender.sendMessage("§a查询完成: $result")
                    }
                }
            }
        }
    }
}
```

### 使用 Chain 任务链 (推荐)

对于复杂的异步处理，建议使用 TabooLib 的 Chain API：

```kotlin
import taboolib.expansion.chain

command("process") {
    execute<ProxyCommandSender> { sender, context, argument ->
        chain {
            sync {
                sender.sendMessage("§a开始处理...")
            }
            
            val result = async {
                // 耗时操作
                performLongOperation()
            }
            
            wait(20)  // 等待 1 秒
            
            sync {
                sender.sendMessage("§a处理完成: $result")
            }
        }
    }
}
```

**更多 Chain API 用法请查看**: [TabooLib Chain 任务链使用指南](TabooLibChain.md)

## 8. 参数验证 restrict()

```kotlin
command("teleport") {
    dynamic("x") {
        restrict<ProxyCommandSender> { sender, context, argument ->
            // 验证X坐标是否为有效数字
            argument.toDoubleOrNull() != null
        }
        
        dynamic("y") {
            restrict<ProxyCommandSender> { sender, context, argument ->
                val y = argument.toDoubleOrNull()
                y != null && y >= -64 && y <= 320  // Minecraft世界高度限制
            }
            
            dynamic("z") {
                restrict<ProxyCommandSender> { sender, context, argument ->
                    argument.toDoubleOrNull() != null
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    if (sender !is Player) {
                        sender.sendMessage("§c只有玩家才能使用传送命令！")
                        return@execute
                    }
                    
                    val x = context.arg(0)?.toDoubleOrNull() ?: return@execute
                    val y = context.arg(1)?.toDoubleOrNull() ?: return@execute
                    val z = argument?.toDoubleOrNull() ?: return@execute
                    
                    val location = Location(sender.world, x, y, z)
                    sender.teleport(location)
                    sender.sendMessage("§a已传送到 ($x, $y, $z)")
                }
            }
        }
    }
}
```

## 9. 完整示例 - 经济系统命令

```kotlin
import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.submit
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandHeader(
    name = "economy",
    aliases = ["eco", "money"],
    description = "经济系统命令",
    permission = "economy.use"
)
object EconomyCommand {
    
    @CommandBody
    val main = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            if (sender is Player) {
                val balance = getPlayerBalance(sender)
                sender.sendMessage("§a你的余额: §e$balance")
            } else {
                sender.sendMessage("§c请使用子命令！")
            }
        }
    }
    
    @CommandBody(aliases = ["give"], permission = "economy.admin")
    val give = subCommand {
        dynamic("player") {
            suggestion<ProxyCommandSender> { sender, context ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            restrict<ProxyCommandSender> { sender, context, argument ->
                Bukkit.getPlayer(argument) != null
            }
            
            dynamic("amount") {
                suggestion<ProxyCommandSender> { sender, context ->
                    listOf("100", "500", "1000", "5000", "10000")
                }
                
                restrict<ProxyCommandSender> { sender, context, argument ->
                    val amount = argument.toDoubleOrNull()
                    amount != null && amount > 0 && amount <= 1000000
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val playerName = context.arg(0) ?: return@execute
                    val amount = argument?.toDoubleOrNull() ?: return@execute
                    val targetPlayer = Bukkit.getPlayer(playerName) ?: return@execute
                    
                    submit(async = true) {
                        // 异步数据库操作
                        addPlayerBalance(targetPlayer, amount)
                        
                        submit(async = false) {
                            sender.sendMessage("§a已给予 $playerName $amount 金币")
                            targetPlayer.sendMessage("§a你收到了 $amount 金币！")
                        }
                    }
                }
            }
        }
    }
    
    @CommandBody(aliases = ["pay"])
    val pay = subCommand {
        dynamic("player") {
            suggestion<ProxyCommandSender> { sender, context ->
                Bukkit.getOnlinePlayers()
                    .filter { it.name != (sender as? Player)?.name }
                    .map { it.name }
            }
            
            dynamic("amount") {
                restrict<ProxyCommandSender> { sender, context, argument ->
                    if (sender !is Player) return@restrict false
                    
                    val amount = argument.toDoubleOrNull()
                    if (amount == null || amount <= 0) return@restrict false
                    
                    val balance = getPlayerBalance(sender)
                    amount <= balance
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    if (sender !is Player) {
                        sender.sendMessage("§c只有玩家可以转账！")
                        return@execute
                    }
                    
                    val targetName = context.arg(0) ?: return@execute
                    val amount = argument?.toDoubleOrNull() ?: return@execute
                    val targetPlayer = Bukkit.getPlayer(targetName) ?: return@execute
                    
                    submit(async = true) {
                        removePlayerBalance(sender, amount)
                        addPlayerBalance(targetPlayer, amount)
                        
                        submit(async = false) {
                            sender.sendMessage("§a已向 $targetName 转账 $amount 金币")
                            targetPlayer.sendMessage("§a收到来自 ${sender.name} 的转账: $amount 金币")
                        }
                    }
                }
            }
        }
    }
    
    @CommandBody(aliases = ["top", "baltop"])
    val top = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            sender.sendMessage("§e正在查询富豪榜...")
            
            submit(async = true) {
                val topPlayers = getTopPlayers(10)
                
                submit(async = false) {
                    sender.sendMessage("§6=== 富豪榜 TOP 10 ===")
                    topPlayers.forEachIndexed { index, (name, balance) ->
                        sender.sendMessage("§e${index + 1}. §f$name §a- §e$balance")
                    }
                }
            }
        }
    }
}

// 模拟经济系统方法
private fun getPlayerBalance(player: Player): Double {
    // 实际应该从数据库获取
    return 1000.0
}

private fun addPlayerBalance(player: Player, amount: Double) {
    // 实际应该写入数据库
}

private fun removePlayerBalance(player: Player, amount: Double) {
    // 实际应该写入数据库
}

private fun getTopPlayers(limit: Int): List<Pair<String, Double>> {
    // 实际应该从数据库查询
    return listOf(
        "Player1" to 50000.0,
        "Player2" to 30000.0,
        "Player3" to 20000.0
    )
}
```

## 10. 最佳实践

### 错误处理

```kotlin
command("manage") {
    dynamic("action") {
        suggestion<ProxyCommandSender> { sender, context ->
            listOf("start", "stop", "restart", "status")
        }
        
        execute<ProxyCommandSender> { sender, context, argument ->
            try {
                when (argument?.lowercase()) {
                    "start" -> {
                        // 启动逻辑
                        sender.sendMessage("§a服务已启动")
                    }
                    "stop" -> {
                        // 停止逻辑
                        sender.sendMessage("§c服务已停止")
                    }
                    "restart" -> {
                        // 重启逻辑
                        sender.sendMessage("§e服务重启中...")
                    }
                    "status" -> {
                        // 状态查询
                        sender.sendMessage("§b服务状态: 运行中")
                    }
                    else -> {
                        sender.sendMessage("§c无效的操作: $argument")
                        sender.sendMessage("§7可用操作: start, stop, restart, status")
                    }
                }
            } catch (e: Exception) {
                sender.sendMessage("§c命令执行出错: ${e.message}")
            }
        }
    }
}
```

### 命令帮助生成

```kotlin
import taboolib.module.command.createHelper

@CommandHeader(name = "myplugin")
object MyPluginCommand {
    
    @CommandBody
    val help = subCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            // 自动生成帮助信息
            createHelper(sender)
        }
    }
}
```

### 性能优化

```kotlin
// 缓存补全结果
private val materialCache = Material.values().map { it.name.lowercase() }

command("item") {
    dynamic("material") {
        suggestion<ProxyCommandSender> { sender, context ->
            // 使用预缓存的材质列表
            materialCache
        }
        
        restrict<ProxyCommandSender> { sender, context, argument ->
            // 快速验证
            materialCache.contains(argument.lowercase())
        }
        
        execute<ProxyCommandSender> { sender, context, argument ->
            // 执行逻辑
        }
    }
}
```

## 注意事项

1. **权限检查**: 始终验证命令发送者的权限
2. **参数验证**: 使用 `restrict()` 进行输入验证
3. **异步处理**: 耗时操作应放在异步任务中执行
4. **错误处理**: 妥善处理异常和边界情况
5. **用户友好**: 提供清晰的错误消息和帮助信息
6. **性能优化**: 避免在补全函数中执行耗时操作
7. **类型安全**: 正确处理类型转换，避免 ClassCastException

TabooLib 的命令系统提供了强大而灵活的功能，通过合理使用DSL和注解两种方式，可以构建出功能完善、用户友好的命令系统。