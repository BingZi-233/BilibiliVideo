# Metrics 模块

## 概述
Metrics 模块提供插件使用统计和数据收集功能，集成 bStats 服务，帮助开发者了解插件的使用情况。

## 功能特性
- 自动数据收集
- 用户统计信息
- 服务器版本统计
- 插件版本分布
- 自定义统计图表
- 隐私保护

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(Metrics)
    }
}
```

## 基本用法

### 启用基础统计
```kotlin
import taboolib.module.metrics.Metrics
import taboolib.common.platform.Plugin

object MyPlugin : Plugin() {
    
    override fun onEnable() {
        // 启用基础统计 (使用 bStats 服务 ID)
        Metrics.enable(12345) // 替换为你的插件 ID
        
        info("插件统计已启用")
    }
}
```

### 获取 bStats 服务 ID
1. 访问 https://bstats.org/
2. 注册账户并添加你的插件
3. 获取分配的服务 ID
4. 在代码中使用该 ID

## 高级功能

### 自定义图表数据
```kotlin
import taboolib.module.metrics.Metrics

// 简单饼状图 - 显示玩家使用的功能分布
Metrics.addCustomChart { serverId ->
    val data = mutableMapOf<String, Int>()
    
    // 统计各功能使用次数
    data["传送功能"] = getTeleportUsageCount()
    data["经济功能"] = getEconomyUsageCount()
    data["聊天功能"] = getChatUsageCount()
    
    SimplePie("feature_usage", data)
}

// 高级饼状图 - 显示服务器规模分布
Metrics.addCustomChart { serverId ->
    AdvancedPie("server_size") { 
        val playerCount = server.onlinePlayers.size
        val category = when {
            playerCount <= 10 -> "小型服务器"
            playerCount <= 50 -> "中型服务器" 
            playerCount <= 100 -> "大型服务器"
            else -> "超大型服务器"
        }
        
        mapOf(category to 1)
    }
}
```

### 条形图统计
```kotlin
// 单线条形图 - 显示平均在线玩家数
Metrics.addCustomChart { serverId ->
    SingleLineChart("average_players") {
        getAverageOnlinePlayers()
    }
}

// 多线条形图 - 显示不同时间段的活跃度
Metrics.addCustomChart { serverId ->
    MultiLineChart("activity_by_hour") {
        mapOf(
            "早晨 (6-12)" to getMorningActivity(),
            "下午 (12-18)" to getAfternoonActivity(), 
            "晚上 (18-24)" to getEveningActivity(),
            "深夜 (0-6)" to getNightActivity()
        )
    }
}
```

### 简单条形图
```kotlin
// 简单条形图 - 显示插件配置选项使用情况
Metrics.addCustomChart { serverId ->
    SimpleBarChart("config_options") {
        mapOf(
            "启用经济" to if (config.getBoolean("economy.enabled")) 1 else 0,
            "启用传送" to if (config.getBoolean("teleport.enabled")) 1 else 0,
            "启用聊天" to if (config.getBoolean("chat.enabled")) 1 else 0
        )
    }
}

// 高级条形图 - 显示详细的功能使用统计
Metrics.addCustomChart { serverId ->
    AdvancedBarChart("detailed_usage") {
        mapOf(
            "每日活跃用户" to getDailyActiveUsers(),
            "每周活跃用户" to getWeeklyActiveUsers(),
            "每月活跃用户" to getMonthlyActiveUsers()
        )
    }
}
```

## 实用统计示例

### 玩家行为统计
```kotlin
object PlayerActivityMetrics {
    
    private val loginCount = mutableMapOf<String, Int>()
    private val commandUsage = mutableMapOf<String, Int>()
    
    fun recordLogin(player: Player) {
        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        loginCount[date] = loginCount.getOrDefault(date, 0) + 1
    }
    
    fun recordCommand(command: String) {
        commandUsage[command] = commandUsage.getOrDefault(command, 0) + 1
    }
    
    fun setupMetrics() {
        // 每日登录统计
        Metrics.addCustomChart { serverId ->
            SingleLineChart("daily_logins") {
                val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
                loginCount.getOrDefault(today, 0)
            }
        }
        
        // 热门命令统计
        Metrics.addCustomChart { serverId ->
            SimplePie("popular_commands") { 
                commandUsage.toList()
                    .sortedByDescending { it.second }
                    .take(5)
                    .toMap()
            }
        }
    }
}
```

### 服务器性能统计
```kotlin
object PerformanceMetrics {
    
    fun setupMetrics() {
        // TPS 统计
        Metrics.addCustomChart { serverId ->
            val tps = server.getTPS()
            val category = when {
                tps >= 19.5 -> "优秀 (19.5+)"
                tps >= 18.0 -> "良好 (18.0+)"
                tps >= 15.0 -> "一般 (15.0+)"
                else -> "较差 (<15.0)"
            }
            
            SimplePie("server_tps", mapOf(category to 1))
        }
        
        // 内存使用统计
        Metrics.addCustomChart { serverId ->
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory() / 1024 / 1024 // MB
            val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val usagePercent = (usedMemory.toDouble() / maxMemory * 100).toInt()
            
            SingleLineChart("memory_usage_percent") { usagePercent }
        }
    }
}
```

### 插件配置统计
```kotlin
object ConfigMetrics {
    
    fun setupMetrics() {
        // 语言分布统计
        Metrics.addCustomChart { serverId ->
            val languages = mutableMapOf<String, Int>()
            
            server.onlinePlayers.forEach { player ->
                val lang = getPlayerLanguage(player)
                languages[lang] = languages.getOrDefault(lang, 0) + 1
            }
            
            SimplePie("language_distribution", languages)
        }
        
        // 功能启用统计
        Metrics.addCustomChart { serverId ->
            mapOf(
                "economy" to if (config.getBoolean("modules.economy")) 1 else 0,
                "teleport" to if (config.getBoolean("modules.teleport")) 1 else 0,
                "chat" to if (config.getBoolean("modules.chat")) 1 else 0,
                "shop" to if (config.getBoolean("modules.shop")) 1 else 0
            )
        }
    }
}
```

## 隐私和配置

### 检查统计状态
```kotlin
// 检查统计是否启用
if (Metrics.isEnabled()) {
    info("插件统计已启用")
} else {
    info("插件统计已禁用")
}

// 获取服务器 ID
val serverId = Metrics.getServerId()
info("服务器 ID: $serverId")
```

### 用户隐私
- bStats 只收集匿名统计信息
- 不收集玩家个人信息
- 服务器管理员可以在 `plugins/bStats/config.yml` 中禁用统计
- 统计数据帮助开发者改进插件

## 注意事项
- 需要在 bStats 网站注册获取服务 ID
- 统计数据每 30 分钟提交一次
- 自定义图表数据应该是有意义的统计信息
- 不要收集敏感或个人身份信息
- 过多的自定义图表可能影响性能