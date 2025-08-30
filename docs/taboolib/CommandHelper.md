# CommandHelper 模块

## 概述
CommandHelper 模块简化了 Bukkit 插件中命令的创建和管理，提供强大的命令处理框架和自动补全功能。

## 功能特性
- 声明式命令定义
- 自动参数解析
- 权限检查
- 命令补全
- 多级子命令支持
- 错误处理

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(CommandHelper)
        install(Bukkit) // 需要与 Bukkit 模块一起使用
    }
}
```

## 基本用法

### 简单命令
```kotlin
import taboolib.common.platform.command.*
import taboolib.common.platform.ProxyCommandSender

@CommandBody
object ExampleCommand {
    
    @CommandHeader(
        name = "example",
        description = "示例命令"
    )
    fun main(sender: ProxyCommandSender) {
        sender.sendMessage("§a这是一个示例命令！")
    }
}
```

### 带参数的命令
```kotlin
@CommandBody
object TeleportCommand {
    
    @CommandHeader(
        name = "tp",
        description = "传送命令"
    )
    fun main(
        sender: ProxyCommandSender,
        @CommandArgument("target", "目标玩家") target: Player
    ) {
        if (sender !is Player) {
            sender.sendMessage("§c只有玩家才能使用此命令！")
            return
        }
        
        sender.teleport(target.location)
        sender.sendMessage("§a已传送到 ${target.name}！")
    }
}
```

### 多级子命令
```kotlin
@CommandBody
object AdminCommand {
    
    @CommandHeader(
        name = "admin",
        description = "管理员命令",
        permission = "myplugin.admin"
    )
    fun main(sender: ProxyCommandSender) {
        sender.sendMessage("§c请使用子命令！")
    }
    
    @SubCommand("reload")
    @CommandDescription("重载配置")
    fun reload(sender: ProxyCommandSender) {
        // 重载逻辑
        sender.sendMessage("§a配置已重载！")
    }
    
    @SubCommand("give")
    @CommandDescription("给予物品")
    fun give(
        sender: ProxyCommandSender,
        @CommandArgument("player") player: Player,
        @CommandArgument("item") item: String,
        @CommandArgument("amount", optional = true) amount: Int = 1
    ) {
        // 给予物品逻辑
        sender.sendMessage("§a已给予 ${player.name} ${amount} 个 ${item}！")
    }
}
```

### 命令补全
```kotlin
@CommandBody
object GameModeCommand {
    
    @CommandHeader(name = "gamemode")
    fun main(
        sender: ProxyCommandSender,
        @CommandArgument("mode") mode: String,
        @CommandArgument("player", optional = true) player: Player?
    ) {
        val targetPlayer = player ?: (sender as? Player)
        if (targetPlayer == null) {
            sender.sendMessage("§c请指定玩家！")
            return
        }
        
        val gameMode = when(mode.lowercase()) {
            "0", "survival", "s" -> GameMode.SURVIVAL
            "1", "creative", "c" -> GameMode.CREATIVE
            "2", "adventure", "a" -> GameMode.ADVENTURE
            "3", "spectator", "sp" -> GameMode.SPECTATOR
            else -> {
                sender.sendMessage("§c无效的游戏模式！")
                return
            }
        }
        
        targetPlayer.gameMode = gameMode
        sender.sendMessage("§a已设置 ${targetPlayer.name} 的游戏模式为 ${gameMode.name}！")
    }
    
    @CommandCompleter("mode")
    fun modeCompleter(sender: ProxyCommandSender, argument: String): List<String> {
        return listOf("survival", "creative", "adventure", "spectator")
            .filter { it.startsWith(argument.lowercase()) }
    }
}
```

## 高级功能

### 权限检查
```kotlin
@CommandBody
object VipCommand {
    
    @CommandHeader(
        name = "vip",
        permission = "myplugin.vip"
    )
    fun main(sender: ProxyCommandSender) {
        sender.sendMessage("§a您是 VIP 用户！")
    }
    
    @SubCommand("exclusive")
    @CommandPermission("myplugin.vip.exclusive")
    fun exclusive(sender: ProxyCommandSender) {
        sender.sendMessage("§e这是 VIP 专属功能！")
    }
}
```

### 参数验证
```kotlin
@CommandBody
object EcoCommand {
    
    @CommandHeader(name = "eco")
    @SubCommand("give")
    fun give(
        sender: ProxyCommandSender,
        @CommandArgument("player") player: Player,
        @CommandArgument("amount") amount: Double
    ) {
        if (amount <= 0) {
            sender.sendMessage("§c金额必须大于 0！")
            return
        }
        
        if (amount > 1000000) {
            sender.sendMessage("§c金额不能超过 1,000,000！")
            return
        }
        
        // 给予金币逻辑
        sender.sendMessage("§a已给予 ${player.name} $amount 金币！")
    }
}
```

### 异步命令处理
```kotlin
@CommandBody
object DatabaseCommand {
    
    @CommandHeader(name = "dbquery")
    @SubCommand("player")
    fun queryPlayer(
        sender: ProxyCommandSender,
        @CommandArgument("name") playerName: String
    ) {
        sender.sendMessage("§e正在查询玩家数据...")
        
        // 异步查询数据库
        runTaskAsync {
            val playerData = queryPlayerFromDatabase(playerName)
            
            // 回到主线程发送结果
            runTask {
                if (playerData != null) {
                    sender.sendMessage("§a玩家数据: $playerData")
                } else {
                    sender.sendMessage("§c未找到玩家数据！")
                }
            }
        }
    }
}
```

## 注意事项
- 命令类必须使用 `@CommandBody` 注解
- 主命令方法必须使用 `@CommandHeader` 注解
- 子命令使用 `@SubCommand` 注解
- 可选参数使用 `optional = true` 并提供默认值
- 权限检查自动进行，无权限时自动返回错误消息
- 异步操作要注意线程安全