# BukkitHook 模块

## 概述
BukkitHook 模块提供与 Bukkit 生态系统中其他插件的集成功能，支持各种插件钩子和依赖管理。

## 功能特性
- 其他插件的软依赖检测
- PlaceholderAPI 集成
- Vault 经济系统集成
- ProtocolLib 支持
- 权限系统集成

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(BukkitHook)
        install(Bukkit) // 通常与 Bukkit 模块一起使用
    }
}
```

## 基本用法

### 检测插件依赖
```kotlin
import taboolib.module.bukkit.hookPlugin

// 检测插件是否存在
if (hookPlugin("Vault") != null) {
    info("检测到 Vault 插件")
}

// 获取插件实例
val placeholderAPI = hookPlugin("PlaceholderAPI")
if (placeholderAPI != null) {
    info("PlaceholderAPI 版本: ${placeholderAPI.description.version}")
}
```

### PlaceholderAPI 集成
```kotlin
import taboolib.module.bukkit.placeholderapi.PlaceholderHook
import taboolib.module.bukkit.placeholderapi.registerPlaceholder

// 注册自定义变量
registerPlaceholder("myplugin") { player, args ->
    when (args) {
        "name" -> player.name
        "level" -> player.level.toString()
        else -> null
    }
}
```

### Vault 经济系统
```kotlin
import taboolib.module.bukkit.vault.getEconomy

// 获取经济系统
val economy = getEconomy()
if (economy != null) {
    // 检查余额
    val balance = economy.getBalance(player)
    
    // 扣除金币
    economy.withdrawPlayer(player, 100.0)
    
    // 增加金币
    economy.depositPlayer(player, 50.0)
}
```

## 常用钩子

### 权限系统
```kotlin
import taboolib.module.bukkit.vault.getPermission

val permission = getPermission()
permission?.let {
    // 检查权限
    val hasPermission = it.has(player, "myplugin.use")
    
    // 添加权限
    it.playerAdd(player, "myplugin.vip")
}
```

## 注意事项
- 需要与相关插件配合使用
- 建议在插件启动时检测依赖插件
- 软依赖失效时要有备用方案