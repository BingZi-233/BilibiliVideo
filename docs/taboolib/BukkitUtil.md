# BukkitUtil 模块

## 概述
BukkitUtil 模块提供 Bukkit 平台的实用工具类和扩展功能，简化常见的 Bukkit 开发任务。

## 功能特性
- 物品构建器 (ItemBuilder)
- 库存管理工具
- 位置和世界操作
- 玩家实用工具
- 任务调度器封装

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(BukkitUtil)
        install(Bukkit) // 需要与 Bukkit 模块一起使用
    }
}
```

## 基本用法

### ItemBuilder 物品构建器
```kotlin
import taboolib.module.bukkit.item.ItemBuilder
import org.bukkit.Material

// 创建自定义物品
val customItem = ItemBuilder(Material.DIAMOND_SWORD)
    .name("§c传说之剑")
    .lore("§7这是一把传说级武器", "§7攻击力: §c+50")
    .enchant(Enchantment.SHARPNESS, 5)
    .unbreakable()
    .flags(ItemFlag.HIDE_ENCHANTS)
    .build()

// 给玩家物品
player.inventory.addItem(customItem)
```

### 库存操作
```kotlin
import taboolib.module.bukkit.util.inventory.*

// 检查玩家背包是否有足够空间
if (player.inventory.hasSpace(5)) {
    info("玩家背包有足够空间")
}

// 移除指定数量的物品
player.inventory.removeItem(Material.COAL, 64)

// 获取玩家背包中指定物品的数量
val coalAmount = player.inventory.getItemAmount(Material.COAL)
```

### 位置和世界工具
```kotlin
import taboolib.module.bukkit.util.location.*

// 字符串转换为位置
val location = "world,0,64,0,0,0".parseToLocation()

// 位置转换为字符串
val locationString = location.parseToString()

// 安全传送玩家
player.teleportSafely(location)
```

### 玩家工具
```kotlin
import taboolib.module.bukkit.util.player.*

// 发送 Action Bar 消息
player.sendActionBar("§a欢迎来到服务器！")

// 发送标题
player.sendTitle("§c警告", "§7请注意安全", fadeIn = 20, stay = 60, fadeOut = 20)

// 播放音效
player.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f)

// 检查玩家是否在线
if (player.isOnlinePlayer()) {
    info("玩家在线")
}
```

### 任务调度
```kotlin
import taboolib.module.bukkit.util.task.*

// 延迟执行任务
runTaskLater(20) { // 20 tick = 1 秒
    player.sendMessage("§a1秒后执行的消息")
}

// 循环执行任务
runTaskTimer(0, 20) { // 立即开始，每秒执行一次
    player.sendActionBar("§e当前时间: ${System.currentTimeMillis()}")
}

// 异步任务
runTaskAsync {
    // 执行耗时操作
    val data = fetchDataFromDatabase()
    
    // 回到主线程更新UI
    runTask {
        player.sendMessage("§a数据加载完成：$data")
    }
}
```

## 高级用法

### 物品匹配
```kotlin
// 检查物品是否匹配
val targetItem = ItemBuilder(Material.DIAMOND).name("§c特殊钻石").build()
if (player.itemInHand.isSimilar(targetItem)) {
    info("玩家手持目标物品")
}
```

### 批量操作
```kotlin
import taboolib.module.bukkit.util.world.*

// 获取附近的实体
val nearbyEntities = player.location.getNearbyEntities(10.0)
nearbyEntities.forEach { entity ->
    if (entity is Monster) {
        entity.remove()
    }
}
```

## 注意事项
- 所有操作都在主线程中执行，避免线程安全问题
- 大量操作时考虑使用异步任务
- ItemBuilder 支持链式调用，提高代码可读性