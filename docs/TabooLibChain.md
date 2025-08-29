# TabooLib Chain 任务链使用指南

## 概述

TabooLib Chain 是对 Kotlin `Coroutine API` 的封装，提供了优雅的异步任务链处理方式。通过 `chain {}` DSL，可以轻松组合同步、异步、等待等操作，避免回调地狱，让异步代码更加清晰易读。

## 安装配置

Chain 功能需要安装 `expansion-submit-chain` 扩展：

```kotlin
taboolib {
    env {
        install(Basic)
        install(Bukkit)
        // Chain 扩展会自动包含在某些模块中
    }
}
```

## 基本用法

### 导入

```kotlin
import taboolib.expansion.chain
```

### 简单示例

```kotlin
chain {
    // 等待 10 ticks
    wait(10)
    
    // 同步执行代码
    sync {
        player.sendMessage("Hello from Sync!")
    }
    
    wait(20)
    
    // 异步执行代码
    async {
        player.sendMessage("Hello from Async!")
    }
    
    wait(5)
    
    // 允许求值的异步操作
    val value = async {
        1 + 2 + 3
    }
    
    sync {
        player.sendMessage("Value: $value")
    }
}
```

## 核心方法详解

### 1. wait(ticks)

等待指定的游戏刻数（ticks）：

```kotlin
chain {
    sync { player.sendMessage("开始等待...") }
    wait(20)  // 等待 1 秒（20 ticks）
    sync { player.sendMessage("等待结束！") }
}
```

### 2. sync {}

在主线程（游戏主循环）中同步执行代码，可以安全访问 Bukkit API：

```kotlin
chain {
    sync {
        // 可以安全访问 Bukkit API
        player.teleport(location)
        player.sendMessage("传送完成")
        
        // 修改方块
        block.type = Material.DIAMOND_BLOCK
    }
}
```

### 3. async {}

在异步线程中执行代码，适合耗时操作：

```kotlin
chain {
    // 异步执行耗时操作
    val data = async {
        // 数据库查询
        database.query("SELECT * FROM players")
    }
    
    // 回到主线程处理结果
    sync {
        processData(data)
    }
}
```

### 4. 重复任务 sync(period, delay)

创建重复执行的同步任务：

```kotlin
chain {
    var index = 0
    
    // 重复执行的同步代码，每 20 ticks 执行一次
    val result = sync(period = 20L, delay = 0L) {
        index += 1
        player.sendMessage("执行次数: $index")
        
        if (index == 10) {
            player.sendMessage("&c任务已取消".colored())
            cancel()  // 取消重复执行
            "任务结束"
        } else {
            // 由于任务没有被 cancel，所以这里的返回值不会应用在 result 上
            "继续执行"
        }
    }
    
    sync {
        player.sendMessage("最终结果: $result")
        // 输出: "最终结果: 任务结束"
    }
}
```

## 实际应用示例

### 1. 玩家登录流程

```kotlin
fun handlePlayerLogin(player: Player) {
    chain {
        sync {
            player.sendMessage("§a正在验证登录信息...")
        }
        
        // 异步验证用户凭据
        val isValid = async {
            validatePlayerCredentials(player)
        }
        
        if (!isValid) {
            sync {
                player.sendMessage("§c登录失败！")
                player.kickPlayer("验证失败")
            }
            return@chain
        }
        
        // 异步加载玩家数据
        val playerData = async {
            loadPlayerDataFromDatabase(player.uniqueId)
        }
        
        // 等待一些时间让玩家看到消息
        wait(10)
        
        sync {
            // 应用玩家数据
            applyPlayerData(player, playerData)
            player.sendMessage("§a登录成功！欢迎回来!")
        }
    }
}
```

### 2. 定时清理任务

```kotlin
fun startCleanupTask() {
    chain {
        var cleanupCount = 0
        
        // 每 5 分钟执行一次清理
        sync(period = 6000L, delay = 0L) {
            cleanupCount++
            
            // 异步执行清理操作
            val cleanedItems = async {
                cleanupExpiredItems()
            }
            
            // 回到主线程广播结果
            sync {
                Bukkit.broadcastMessage("§7[系统] 清理了 $cleanedItems 个过期物品")
            }
            
            if (cleanupCount >= 100) {
                cancel()
                "清理任务结束"
            } else {
                "继续清理"
            }
        }
    }
}
```

### 3. 复杂的数据处理工作流

```kotlin
fun processPlayerStatistics(player: Player) {
    chain {
        sync {
            player.sendMessage("§e开始统计数据处理...")
        }
        
        // 第一阶段：收集原始数据
        val rawData = async {
            collectRawPlayerData(player.uniqueId)
        }
        
        wait(20)
        sync {
            player.sendMessage("§7数据收集完成，正在分析...")
        }
        
        // 第二阶段：分析数据
        val analysisResult = async {
            analyzePlayerData(rawData)
        }
        
        wait(40)
        sync {
            player.sendMessage("§7数据分析完成，正在生成报告...")
        }
        
        // 第三阶段：生成报告
        val report = async {
            generatePlayerReport(analysisResult)
        }
        
        wait(60)
        
        // 第四阶段：展示结果
        sync {
            displayReportToPlayer(player, report)
            player.sendMessage("§a统计报告生成完成！")
        }
    }
}
```

### 4. 服务器备份流程

```kotlin
fun performServerBackup() {
    chain {
        sync {
            Bukkit.broadcastMessage("§e[备份] 服务器备份即将开始...")
        }
        
        wait(100) // 给玩家 5 秒时间看到消息
        
        sync {
            Bukkit.broadcastMessage("§c[备份] 备份开始，可能会有短暂卡顿")
        }
        
        // 异步执行备份操作
        val backupResult = async {
            performBackupOperation()
        }
        
        sync {
            if (backupResult.success) {
                Bukkit.broadcastMessage("§a[备份] 备份完成！文件大小: ${backupResult.size}")
            } else {
                Bukkit.broadcastMessage("§c[备份] 备份失败: ${backupResult.error}")
            }
        }
    }
}
```

### 5. 玩家传送倒计时

```kotlin
fun teleportWithCountdown(player: Player, location: Location, seconds: Int = 5) {
    chain {
        var countdown = seconds
        
        // 倒计时循环
        sync(period = 20L, delay = 0L) {
            if (countdown > 0) {
                player.sendMessage("§e传送倒计时: §c$countdown §e秒")
                player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
                countdown--
                "继续倒计时"
            } else {
                cancel()
                "倒计时结束"
            }
        }
        
        // 执行传送
        sync {
            player.teleport(location)
            player.sendMessage("§a传送成功！")
            player.playSound(player.location, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f)
        }
    }
}
```

## 与命令系统结合使用

### 在命令中使用 Chain

```kotlin
@CommandHeader(name = "process")
object ProcessCommand {
    
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, context, argument ->
            if (sender !is Player) {
                sender.sendMessage("§c只有玩家可以使用此命令！")
                return@execute
            }
            
            chain {
                sync {
                    sender.sendMessage("§a开始处理...")
                }
                
                // 模拟长时间处理
                val result = async {
                    Thread.sleep(3000)
                    "处理完成的数据"
                }
                
                sync {
                    sender.sendMessage("§a处理结果: $result")
                }
            }
        }
    }
}
```

### 复杂命令处理

```kotlin
@CommandBody(aliases = ["backup"])
val backup = subCommand {
    execute<ProxyCommandSender> { sender, context, argument ->
        chain {
            sync {
                sender.sendMessage("§e开始备份流程...")
            }
            
            // 检查权限
            val hasPermission = async {
                checkBackupPermission(sender)
            }
            
            if (!hasPermission) {
                sync {
                    sender.sendMessage("§c权限不足！")
                }
                return@chain
            }
            
            // 创建备份
            val backupFile = async {
                createBackup()
            }
            
            wait(20)
            
            sync {
                sender.sendMessage("§a备份完成: ${backupFile.name}")
            }
        }
    }
}
```

## 错误处理

### 异常捕获

```kotlin
chain {
    try {
        val data = async {
            riskyOperation()
        }
        
        sync {
            processData(data)
        }
    } catch (e: Exception) {
        sync {
            player.sendMessage("§c操作失败: ${e.message}")
        }
    }
}
```

### 安全的异步操作

```kotlin
chain {
    val result = async {
        try {
            performNetworkRequest()
        } catch (e: Exception) {
            null
        }
    }
    
    sync {
        if (result != null) {
            player.sendMessage("§a请求成功: $result")
        } else {
            player.sendMessage("§c网络请求失败")
        }
    }
}
```

## 最佳实践

### 1. 合理使用 sync 和 async

```kotlin
// ✅ 正确：Bukkit API 在 sync 中调用
chain {
    val data = async {
        // 耗时的数据库查询
        database.queryPlayerData(uuid)
    }
    
    sync {
        // 安全地使用 Bukkit API
        player.inventory.addItem(createItemFromData(data))
    }
}

// ❌ 错误：在 async 中调用 Bukkit API
chain {
    async {
        player.sendMessage("这可能会导致线程安全问题")  // 危险！
    }
}
```

### 2. 避免过度嵌套

```kotlin
// ✅ 正确：平铺的 chain 结构
chain {
    val step1 = async { performStep1() }
    val step2 = async { performStep2(step1) }
    val step3 = async { performStep3(step2) }
    
    sync {
        displayResult(step3)
    }
}

// ❌ 错误：过度嵌套
chain {
    async {
        val step1 = performStep1()
        chain {
            val step2 = async { performStep2(step1) }
            // 嵌套过深，难以维护
        }
    }
}
```

### 3. 适当的等待时间

```kotlin
chain {
    sync { player.sendMessage("§e准备执行操作...") }
    
    wait(20)  // 给玩家 1 秒时间看到消息
    
    val result = async { performLongOperation() }
    
    sync { 
        player.sendMessage("§a操作完成: $result")
    }
}
```

### 4. 资源清理

```kotlin
chain {
    val resource = async {
        openDatabaseConnection()
    }
    
    try {
        val data = async {
            resource.queryData()
        }
        
        sync {
            processData(data)
        }
    } finally {
        async {
            resource.close()  // 确保资源被清理
        }
    }
}
```

## 与传统异步方法对比

### 传统 submit 方式

```kotlin
// 传统方式：回调地狱
submit(async = true) {
    val data = fetchData()
    submit(async = false) {
        player.sendMessage("数据: $data")
        submit(async = true) {
            val processed = processData(data)
            submit(async = false) {
                player.sendMessage("处理结果: $processed")
            }
        }
    }
}
```

### Chain 方式

```kotlin
// Chain 方式：清晰优雅
chain {
    val data = async { fetchData() }
    sync { player.sendMessage("数据: $data") }
    
    val processed = async { processData(data) }
    sync { player.sendMessage("处理结果: $processed") }
}
```

## 性能注意事项

1. **避免频繁的线程切换**：合理组织 sync 和 async 的调用顺序
2. **控制并发数量**：不要在短时间内创建过多的 chain
3. **及时取消无用任务**：使用 `cancel()` 取消不需要的重复任务
4. **异常处理**：确保异步操作中的异常得到妥善处理

TabooLib 的 Chain API 提供了强大而优雅的异步处理能力，通过合理使用可以大大提升代码的可读性和维护性。