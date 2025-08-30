# Kether 模块

## 概述
Kether 是 TabooLib 的脚本引擎模块，提供强大的脚本解析和执行功能，支持动态配置、条件判断、数据处理等高级功能。

## 功能特性
- 动态脚本执行
- 条件判断和循环
- 变量和函数支持
- 数据操作和计算
- 与插件深度集成
- 热重载支持

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(Kether)
        install(Bukkit) // 通常与 Bukkit 模块一起使用
    }
}
```

## 基本用法

### 执行简单脚本
```kotlin
import taboolib.module.kether.ScriptService
import taboolib.module.kether.run

// 执行简单脚本
val result = ScriptService.run("print 'Hello Kether!'") {
    sender = player // 设置执行者
}

// 带变量的脚本
val script = """
    set money to 1000
    print "玩家金币: " + money
    if money > 500 then {
        print "你很富有！"
    } else {
        print "需要更多金币"
    }
"""

ScriptService.run(script) {
    sender = player
}
```

### 配置文件中的脚本
```yaml
# config.yml
welcome-script: |
  print "欢迎来到服务器, " + player.name
  if player.hasPlayedBefore then {
    print "欢迎回来！"
  } else {
    print "这是你的第一次访问"
    give player "diamond" 1
  }

reward-calculation: |
  set base to 100
  set level to player.level
  set bonus to level * 10
  return base + bonus
```

```kotlin
// 执行配置中的脚本
fun executeWelcomeScript(player: Player) {
    val script = config.getString("welcome-script") ?: return
    
    ScriptService.run(script) {
        sender = player
        namespace = listOf("minecraft", "myplugin") // 可用的命名空间
    }
}

// 计算奖励
fun calculateReward(player: Player): Int {
    val script = config.getString("reward-calculation") ?: "return 0"
    
    val result = ScriptService.run(script) {
        sender = player
    }
    
    return (result as? Number)?.toInt() ?: 0
}
```

## 高级功能

### 自定义函数注册
```kotlin
import taboolib.module.kether.action.ActionProperty
import taboolib.module.kether.KetherParser

object CustomKetherActions {
    
    fun registerActions() {
        // 注册获取玩家余额的函数
        KetherParser.registerAction("balance") { parser ->
            ActionProperty<Double> { 
                val economy = getEconomy()
                if (economy != null && sender is Player) {
                    economy.getBalance(sender as Player)
                } else {
                    0.0
                }
            }
        }
        
        // 注册设置余额的函数
        KetherParser.registerAction("setBalance") { parser ->
            val amount = parser.expectToken("amount")
            ActionProperty<Boolean> {
                val economy = getEconomy()
                if (economy != null && sender is Player) {
                    economy.withdrawPlayer(sender as Player, economy.getBalance(sender as Player))
                    economy.depositPlayer(sender as Player, amount.get())
                    true
                } else {
                    false
                }
            }
        }
        
        // 注册传送函数
        KetherParser.registerAction("teleport") { parser ->
            val x = parser.expectToken("x")
            val y = parser.expectToken("y") 
            val z = parser.expectToken("z")
            val world = parser.expectToken("world", optional = true)
            
            ActionProperty<Boolean> {
                if (sender !is Player) return@ActionProperty false
                
                val player = sender as Player
                val targetWorld = world?.get()?.let { server.getWorld(it) } ?: player.world
                val location = Location(targetWorld, x.get(), y.get(), z.get())
                
                player.teleport(location)
                true
            }
        }
    }
}
```

### 复杂脚本示例
```kotlin
// 商店系统脚本
val shopScript = """
    # 检查玩家金币
    set money to balance
    set item_cost to 500
    
    if money >= item_cost then {
        # 扣除金币并给予物品
        setBalance (money - item_cost)
        give player "diamond_sword" 1 with name "传说之剑"
        print "§a购买成功！获得传说之剑"
        sound player "entity.experience_orb.pickup" 1.0 1.0
    } else {
        print "§c金币不足！需要 " + item_cost + " 金币"
        sound player "entity.villager.no" 1.0 1.0
    }
"""

// 每日签到脚本
val checkinScript = """
    # 检查今日是否已签到
    set today to date("yyyy-MM-dd")
    set last_checkin to data("last_checkin")
    
    if today == last_checkin then {
        print "§c今日已签到！"
    } else {
        # 执行签到
        setData "last_checkin" today
        
        # 计算连续签到天数
        set streak to data("checkin_streak") default 0
        setData "checkin_streak" (streak + 1)
        
        # 签到奖励
        set reward to 100 + (streak * 10)
        setBalance (balance + reward)
        
        print "§a签到成功！获得 " + reward + " 金币"
        print "§e连续签到 " + (streak + 1) + " 天"
        
        # 特殊奖励
        if (streak + 1) % 7 == 0 then {
            give player "diamond" 1
            print "§d连续签到7天奖励：钻石 x1"
        }
    }
"""
```

### 条件和循环
```kotlin
val complexLogicScript = """
    # 循环示例
    set players to server.onlinePlayers
    for player in players {
        if player.level > 50 then {
            tell player "§a你是高级玩家！"
        }
    }
    
    # 计数循环
    set count to 0
    while count < 5 {
        print "计数: " + count
        set count to count + 1
    }
    
    # 条件判断
    set time to server.time
    if time >= 0 && time < 6000 then {
        print "现在是早晨"
    } else if time >= 6000 && time < 12000 then {
        print "现在是上午"
    } else if time >= 12000 && time < 18000 then {
        print "现在是下午"
    } else {
        print "现在是夜晚"
    }
"""
```

### 数据持久化
```kotlin
// 在脚本中使用数据存储
val dataScript = """
    # 读取玩家数据
    set kills to data("kills") default 0
    set deaths to data("deaths") default 0
    
    # 计算KD比
    set kd_ratio to if deaths > 0 then kills / deaths else kills
    
    # 保存统计信息
    setData "last_login" now()
    setData "total_playtime" (data("total_playtime") default 0 + 1)
    
    # 显示统计
    print "§e=== 玩家统计 ==="
    print "§f击杀: §a" + kills
    print "§f死亡: §c" + deaths
    print "§fK/D比: §b" + format(kd_ratio, "0.00")
"""

// Kether 数据操作扩展
object KetherDataExtensions {
    
    fun registerDataActions() {
        // 注册获取玩家数据
        KetherParser.registerAction("data") { parser ->
            val key = parser.expectToken("key")
            val defaultValue = parser.expectToken("default", optional = true)
            
            ActionProperty<Any?> {
                if (sender !is Player) return@ActionProperty null
                
                val player = sender as Player
                val data = getPlayerData(player, key.get())
                data ?: defaultValue?.get()
            }
        }
        
        // 注册设置玩家数据
        KetherParser.registerAction("setData") { parser ->
            val key = parser.expectToken("key")
            val value = parser.expectToken("value")
            
            ActionProperty<Unit> {
                if (sender is Player) {
                    setPlayerData(sender as Player, key.get(), value.get())
                }
            }
        }
    }
}
```

## 实际应用场景

### 任务系统
```kotlin
val questScript = """
    # 检查任务状态
    set quest_id to "kill_monsters"
    set progress to data("quest_" + quest_id + "_progress") default 0
    set target to 10
    
    # 更新进度
    set progress to progress + 1
    setData ("quest_" + quest_id + "_progress") progress
    
    print "§e任务进度: " + progress + "/" + target
    
    # 检查完成
    if progress >= target then {
        # 完成任务
        setData ("quest_" + quest_id + "_completed") true
        
        # 给予奖励
        setBalance (balance + 1000)
        give player "diamond" 3
        
        print "§a任务完成！获得奖励：1000金币 + 3钻石"
        sound player "ui.toast.challenge_complete" 1.0 1.0
        
        # 解锁下一个任务
        setData "quest_explore_world_unlocked" true
    }
"""

// 在怪物死亡事件中执行
@SubscribeEvent
fun onMonsterKill(event: EntityDeathEvent) {
    val killer = event.entity.killer ?: return
    
    ScriptService.run(questScript) {
        sender = killer
    }
}
```

### 技能系统
```kotlin
val skillScript = """
    # 技能升级系统
    set skill to args[0]  # 技能名称
    set exp_gained to args[1]  # 获得的经验
    
    # 获取当前技能数据
    set current_level to data("skill_" + skill + "_level") default 1
    set current_exp to data("skill_" + skill + "_exp") default 0
    
    # 添加经验
    set new_exp to current_exp + exp_gained
    
    # 计算所需经验
    set required_exp to current_level * 100
    
    # 检查是否升级
    if new_exp >= required_exp then {
        set new_level to current_level + 1
        set remaining_exp to new_exp - required_exp
        
        # 保存新数据
        setData ("skill_" + skill + "_level") new_level
        setData ("skill_" + skill + "_exp") remaining_exp
        
        print "§a" + skill + " 技能升级！"
        print "§e等级: " + current_level + " → " + new_level
        
        # 升级奖励
        give player "experience_bottle" new_level
        
    } else {
        # 保存经验
        setData ("skill_" + skill + "_exp") new_exp
        print "§b" + skill + " 获得经验: " + exp_gained
    }
    
    # 显示进度条
    set progress to (new_exp * 20) / required_exp
    set bar to ""
    repeat progress times {
        set bar to bar + "§a▌"
    }
    repeat (20 - progress) times {
        set bar to bar + "§7▌"
    }
    
    print "§f[" + bar + "§f] " + new_exp + "/" + required_exp
"""
```

## 注意事项
- Kether 脚本在主线程中执行，避免长时间运行的脚本
- 复杂逻辑可能影响服务器性能
- 脚本中的错误会抛出异常，需要适当处理
- 变量作用域仅限于脚本执行期间
- 建议对频繁执行的脚本进行性能测试