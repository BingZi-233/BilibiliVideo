# Bukkit 模块

## 概述
Bukkit 模块是 TabooLib 的核心平台实现模块，提供 Bukkit/Spigot/Paper 服务器的基础功能支持和平台适配。

## 功能特性
- Bukkit 平台适配器
- 事件监听系统
- 插件生命周期管理
- 服务器 API 封装
- 跨版本兼容性

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(Bukkit)
        install(Basic) // 通常需要与 Basic 模块一起使用
    }
}
```

## 基本用法

### 事件监听
```kotlin
import taboolib.common.platform.event.SubscribeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object PlayerEvents {
    
    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 自定义加入消息
        event.joinMessage = "§a欢迎 ${player.name} 来到服务器！"
        
        // 给予新手礼包
        if (!player.hasPlayedBefore()) {
            giveStarterKit(player)
        }
        
        // 记录登录时间
        recordLoginTime(player)
    }
    
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // 自定义离开消息
        event.quitMessage = "§c${player.name} 离开了服务器"
        
        // 保存玩家数据
        savePlayerData(player)
    }
}
```

### 服务器信息获取
```kotlin
import taboolib.common.platform.function.*

// 获取在线玩家数
val onlineCount = onlinePlayers().size
info("当前在线玩家数: $onlineCount")

// 获取服务器版本信息
val version = serverVersion()
info("服务器版本: $version")

// 广播消息
broadcast("§a服务器维护将在5分钟后开始！")

// 执行控制台命令
console().performCommand("say 这是来自控制台的消息")
```

### 任务调度
```kotlin
import taboolib.common.platform.schedule.SubmitTask
import taboolib.common.platform.schedule.TaskScope

object ServerTasks {
    
    // 同步任务
    @SubmitTask(period = 20) // 每秒执行一次
    fun heartbeat() {
        // 心跳任务，检查服务器状态
        checkServerStatus()
    }
    
    // 异步任务
    @SubmitTask(async = true, period = 1200) // 每分钟执行一次
    fun autoSave() {
        // 异步保存数据
        saveAllPlayerData()
        info("自动保存完成")
    }
    
    // 延迟任务
    @SubmitTask(delay = 100) // 5秒后执行一次
    fun delayedTask() {
        info("延迟任务执行")
    }
}

// 手动创建任务
fun createCustomTask() {
    submitTask {
        info("这是一个手动创建的同步任务")
    }
    
    submitTask(async = true) {
        // 异步任务
        val data = fetchDataFromDatabase()
        
        // 回到主线程更新
        submitTask {
            updatePlayerData(data)
        }
    }
}
```

## 高级功能

### 跨版本兼容性
```kotlin
import taboolib.module.bukkit.ServerVersion

object VersionCompatibility {
    
    fun handleVersionSpecificFeature() {
        when {
            ServerVersion.isUniversal -> {
                // 通用实现
                info("使用通用实现")
            }
            
            ServerVersion.isHigherOrEqual("1.19") -> {
                // 1.19+ 特有功能
                handleModernFeatures()
            }
            
            ServerVersion.isHigherOrEqual("1.16") -> {
                // 1.16+ RGB 颜色支持
                handleRGBColors()
            }
            
            else -> {
                // 旧版本兼容
                handleLegacyFeatures()
            }
        }
    }
    
    private fun handleModernFeatures() {
        // 现代版本的功能实现
    }
    
    private fun handleRGBColors() {
        // RGB 颜色相关功能
    }
    
    private fun handleLegacyFeatures() {
        // 旧版本兼容代码
    }
}
```

### 自定义监听器
```kotlin
import taboolib.common.platform.event.EventPriority

object CustomEventHandlers {
    
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPlayerDamage(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        
        val player = event.entity as Player
        
        // 检查玩家是否有保护状态
        if (player.hasMetadata("protected")) {
            event.isCancelled = true
            player.sendMessage("§c你处于保护状态，无法受到伤害！")
            return
        }
        
        // 记录伤害事件
        recordDamageEvent(player, event.damage, event.cause)
    }
    
    @SubscribeEvent(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block
        
        // 检查区域权限
        if (!hasBlockPermission(player, block.location)) {
            event.isCancelled = true
            player.sendMessage("§c你没有权限在此区域破坏方块！")
            return
        }
        
        // 给予经验奖励
        giveBreakingExperience(player, block.type)
    }
}
```

### 服务器管理工具
```kotlin
object ServerManager {
    
    // 踢出所有玩家
    fun kickAllPlayers(reason: String = "服务器维护") {
        onlinePlayers().forEach { player ->
            player.kickPlayer("§c$reason")
        }
    }
    
    // 服务器重启倒计时
    fun startRestartCountdown(seconds: Int) {
        var remaining = seconds
        
        val task = submitTask(period = 20) {
            when (remaining) {
                in listOf(300, 240, 180, 120, 60, 30, 10, 5, 4, 3, 2, 1) -> {
                    broadcast("§c服务器将在 $remaining 秒后重启！")
                }
                0 -> {
                    broadcast("§c服务器正在重启...")
                    kickAllPlayers("服务器重启")
                    // 执行重启命令
                    console().performCommand("restart")
                    return@submitTask // 停止任务
                }
            }
            remaining--
        }
    }
    
    // 获取服务器统计信息
    fun getServerStats(): ServerStats {
        val runtime = Runtime.getRuntime()
        return ServerStats(
            onlinePlayers = onlinePlayers().size,
            maxPlayers = server.maxPlayers,
            usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
            maxMemory = runtime.maxMemory() / 1024 / 1024,
            tps = server.getTPS(),
            uptime = getServerUptime()
        )
    }
}

data class ServerStats(
    val onlinePlayers: Int,
    val maxPlayers: Int,
    val usedMemory: Long,
    val maxMemory: Long,
    val tps: DoubleArray,
    val uptime: Long
)
```

### 插件间通信
```kotlin
import taboolib.common.platform.service.PlatformService

// 定义服务接口
interface EconomyService : PlatformService {
    fun getBalance(player: Player): Double
    fun deposit(player: Player, amount: Double): Boolean
    fun withdraw(player: Player, amount: Double): Boolean
}

// 实现服务
class MyEconomyService : EconomyService {
    override fun getBalance(player: Player): Double {
        // 获取余额实现
        return playerData[player.uniqueId]?.balance ?: 0.0
    }
    
    override fun deposit(player: Player, amount: Double): Boolean {
        // 存款实现
        val currentBalance = getBalance(player)
        setBalance(player, currentBalance + amount)
        return true
    }
    
    override fun withdraw(player: Player, amount: Double): Boolean {
        // 取款实现
        val currentBalance = getBalance(player)
        if (currentBalance < amount) return false
        
        setBalance(player, currentBalance - amount)
        return true
    }
}

// 在其他插件中使用服务
object OtherPluginUsage {
    
    fun buyItem(player: Player, itemCost: Double) {
        val economyService = PlatformService.getService<EconomyService>()
        
        if (economyService?.withdraw(player, itemCost) == true) {
            player.sendMessage("§a购买成功！")
            giveItem(player)
        } else {
            player.sendMessage("§c余额不足！")
        }
    }
}
```

## 注意事项
- Bukkit 模块是平台特定的，只在 Bukkit 系服务器上工作
- 事件监听器会自动注册和注销
- 任务调度器在插件禁用时会自动清理
- 跨版本兼容需要仔细测试
- 异步任务中不能直接操作 Bukkit API