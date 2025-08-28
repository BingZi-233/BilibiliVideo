# DatabasePlayer 模块

## 概述
DatabasePlayer 模块提供玩家数据的数据库存储和管理功能，支持多种数据库后端，实现玩家数据的持久化存储。

## 功能特性
- 多数据库支持 (SQLite, MySQL, PostgreSQL)
- 玩家数据自动同步
- 异步数据操作
- 数据缓存机制
- 跨服务器数据共享
- 自动数据迁移

## 安装配置
在 `build.gradle.kts` 中配置：
```kotlin
taboolib {
    env {
        install(DatabasePlayer)
        install(Database) // 需要数据库模块支持
    }
}
```

## 基本用法

### 数据库配置
在 `config.yml` 中配置数据库连接：

```yaml
database:
  # 数据库类型: sqlite, mysql, postgresql
  type: sqlite
  
  # SQLite 配置
  sqlite:
    file: "players.db"
    
  # MySQL 配置  
  mysql:
    host: "localhost"
    port: 3306
    database: "minecraft"
    username: "root"
    password: "password"
    ssl: false
    
  # 连接池配置
  pool:
    maximum-pool-size: 10
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

### 玩家数据模型定义
```kotlin
import taboolib.module.database.ColumnType
import taboolib.module.database.Table

@Table("player_data")
data class PlayerData(
    @ColumnType(type = "VARCHAR(36)", primaryKey = true)
    val uuid: String,
    
    @ColumnType(type = "VARCHAR(16)")
    val name: String,
    
    @ColumnType(type = "BIGINT", def = "0")
    val money: Long = 0,
    
    @ColumnType(type = "INT", def = "1") 
    val level: Int = 1,
    
    @ColumnType(type = "BIGINT", def = "0")
    val exp: Long = 0,
    
    @ColumnType(type = "TIMESTAMP", def = "CURRENT_TIMESTAMP")
    val lastLogin: String = "",
    
    @ColumnType(type = "BIGINT", def = "0")
    val playtime: Long = 0,
    
    @ColumnType(type = "TEXT")
    val settings: String = "{}"
)
```

### 数据操作服务
```kotlin
import taboolib.module.database.*
import taboolib.module.database.database.DatabaseTask
import org.bukkit.entity.Player

object PlayerDataService {
    
    // 获取数据库实例
    private val database by lazy { Database.getDataSource() }
    
    // 创建表
    fun createTables() {
        database.createTable<PlayerData>()
    }
    
    // 异步获取玩家数据
    fun getPlayerData(player: Player, callback: (PlayerData?) -> Unit) {
        DatabaseTask.run(async = true) {
            val data = database.selectOne<PlayerData> {
                where { "uuid" eq player.uniqueId.toString() }
            }
            
            // 回到主线程执行回调
            DatabaseTask.run(async = false) {
                callback(data)
            }
        }
    }
    
    // 同步获取玩家数据 (缓存优先)
    fun getPlayerDataSync(player: Player): PlayerData? {
        return playerCache[player.uniqueId] ?: run {
            val data = database.selectOne<PlayerData> {
                where { "uuid" eq player.uniqueId.toString() }
            }
            data?.let { playerCache[player.uniqueId] = it }
            data
        }
    }
    
    // 保存玩家数据
    fun savePlayerData(data: PlayerData) {
        DatabaseTask.run(async = true) {
            database.replace<PlayerData>(data)
            
            // 更新缓存
            DatabaseTask.run(async = false) {
                playerCache[UUID.fromString(data.uuid)] = data
            }
        }
    }
    
    // 删除玩家数据
    fun deletePlayerData(uuid: UUID) {
        DatabaseTask.run(async = true) {
            database.delete<PlayerData> {
                where { "uuid" eq uuid.toString() }
            }
            
            // 从缓存中移除
            DatabaseTask.run(async = false) {
                playerCache.remove(uuid)
            }
        }
    }
    
    // 更新特定字段
    fun updatePlayerMoney(player: Player, newAmount: Long) {
        DatabaseTask.run(async = true) {
            database.update<PlayerData> {
                set("money", newAmount)
                where { "uuid" eq player.uniqueId.toString() }
            }
            
            // 更新缓存
            DatabaseTask.run(async = false) {
                playerCache[player.uniqueId]?.let {
                    playerCache[player.uniqueId] = it.copy(money = newAmount)
                }
            }
        }
    }
}
```

### 缓存系统
```kotlin
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PlayerDataCache {
    
    // 玩家数据缓存
    private val playerCache = ConcurrentHashMap<UUID, PlayerData>()
    
    // 获取缓存的玩家数据
    fun getCachedData(uuid: UUID): PlayerData? {
        return playerCache[uuid]
    }
    
    // 缓存玩家数据
    fun cachePlayerData(uuid: UUID, data: PlayerData) {
        playerCache[uuid] = data
    }
    
    // 移除缓存
    fun removeCachedData(uuid: UUID) {
        playerCache.remove(uuid)
    }
    
    // 清理所有缓存
    fun clearCache() {
        playerCache.clear()
    }
    
    // 获取缓存大小
    fun getCacheSize(): Int {
        return playerCache.size
    }
    
    // 预加载在线玩家数据
    fun preloadOnlinePlayersData() {
        server.onlinePlayers.forEach { player ->
            if (!playerCache.containsKey(player.uniqueId)) {
                PlayerDataService.getPlayerData(player) { data ->
                    data?.let { cachePlayerData(player.uniqueId, it) }
                }
            }
        }
    }
}
```

## 高级功能

### 自动数据同步
```kotlin
import taboolib.common.platform.event.SubscribeEvent

object PlayerDataSync {
    
    @SubscribeEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        
        // 异步加载玩家数据
        PlayerDataService.getPlayerData(player) { data ->
            if (data != null) {
                // 数据存在，更新缓存和最后登录时间
                val updatedData = data.copy(
                    name = player.name, // 更新玩家名
                    lastLogin = System.currentTimeMillis().toString()
                )
                
                PlayerDataCache.cachePlayerData(player.uniqueId, updatedData)
                PlayerDataService.savePlayerData(updatedData)
                
                info("已加载玩家 ${player.name} 的数据")
            } else {
                // 新玩家，创建默认数据
                val newData = PlayerData(
                    uuid = player.uniqueId.toString(),
                    name = player.name,
                    lastLogin = System.currentTimeMillis().toString()
                )
                
                PlayerDataCache.cachePlayerData(player.uniqueId, newData)
                PlayerDataService.savePlayerData(newData)
                
                info("为新玩家 ${player.name} 创建数据")
            }
        }
    }
    
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        
        // 保存玩家数据
        PlayerDataCache.getCachedData(player.uniqueId)?.let { data ->
            val updatedData = data.copy(
                playtime = data.playtime + getSessionPlaytime(player)
            )
            
            PlayerDataService.savePlayerData(updatedData)
        }
        
        // 延迟清理缓存 (防止立即重连)
        submitTask(delay = 1200) { // 1分钟后清理
            if (!player.isOnline) {
                PlayerDataCache.removeCachedData(player.uniqueId)
            }
        }
    }
}
```

### 数据查询和统计
```kotlin
object PlayerDataQueries {
    
    // 获取富豪排行榜
    fun getTopRichPlayers(limit: Int = 10): List<PlayerData> {
        return database.select<PlayerData> {
            orderBy("money", Order.DESC)
            limit(limit)
        }
    }
    
    // 获取等级排行榜
    fun getTopLevelPlayers(limit: Int = 10): List<PlayerData> {
        return database.select<PlayerData> {
            orderBy("level", Order.DESC, "exp", Order.DESC)
            limit(limit)
        }
    }
    
    // 获取在线时间排行榜
    fun getTopPlaytimePlayers(limit: Int = 10): List<PlayerData> {
        return database.select<PlayerData> {
            orderBy("playtime", Order.DESC)
            limit(limit)
        }
    }
    
    // 搜索玩家
    fun searchPlayers(namePattern: String): List<PlayerData> {
        return database.select<PlayerData> {
            where { "name" like "%$namePattern%" }
            orderBy("name", Order.ASC)
        }
    }
    
    // 获取服务器统计
    fun getServerStats(): ServerPlayerStats {
        val totalPlayers = database.selectOne<Int>("SELECT COUNT(*) FROM player_data") ?: 0
        val totalMoney = database.selectOne<Long>("SELECT SUM(money) FROM player_data") ?: 0
        val avgLevel = database.selectOne<Double>("SELECT AVG(level) FROM player_data") ?: 0.0
        val totalPlaytime = database.selectOne<Long>("SELECT SUM(playtime) FROM player_data") ?: 0
        
        return ServerPlayerStats(totalPlayers, totalMoney, avgLevel, totalPlaytime)
    }
}

data class ServerPlayerStats(
    val totalPlayers: Int,
    val totalMoney: Long,
    val averageLevel: Double,
    val totalPlaytime: Long
)
```

### 数据迁移工具
```kotlin
object DataMigration {
    
    // 从文件迁移到数据库
    fun migrateFromFiles(dataFolder: File) {
        val playerFiles = dataFolder.listFiles { file ->
            file.extension == "yml" && file.nameWithoutExtension.matches(
                Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            )
        }
        
        playerFiles?.forEach { file ->
            try {
                val uuid = file.nameWithoutExtension
                val config = YamlConfiguration.loadConfiguration(file)
                
                val playerData = PlayerData(
                    uuid = uuid,
                    name = config.getString("name", "Unknown"),
                    money = config.getLong("money", 0),
                    level = config.getInt("level", 1),
                    exp = config.getLong("exp", 0),
                    lastLogin = config.getString("lastLogin", ""),
                    playtime = config.getLong("playtime", 0),
                    settings = config.getString("settings", "{}")
                )
                
                PlayerDataService.savePlayerData(playerData)
                info("已迁移玩家数据: ${playerData.name}")
                
            } catch (e: Exception) {
                severe("迁移文件 ${file.name} 时出错: ${e.message}")
            }
        }
    }
    
    // 备份数据到文件
    fun backupToFiles(backupFolder: File) {
        backupFolder.mkdirs()
        
        val allPlayers = database.select<PlayerData>()
        
        allPlayers.forEach { playerData ->
            val file = File(backupFolder, "${playerData.uuid}.yml")
            val config = YamlConfiguration()
            
            config.set("name", playerData.name)
            config.set("money", playerData.money)
            config.set("level", playerData.level) 
            config.set("exp", playerData.exp)
            config.set("lastLogin", playerData.lastLogin)
            config.set("playtime", playerData.playtime)
            config.set("settings", playerData.settings)
            
            try {
                config.save(file)
            } catch (e: Exception) {
                severe("备份玩家 ${playerData.name} 数据时出错: ${e.message}")
            }
        }
        
        info("数据备份完成，共备份 ${allPlayers.size} 个玩家")
    }
}
```

## 实用示例

### 经济系统集成
```kotlin
object DatabaseEconomy {
    
    fun getBalance(player: Player): Double {
        val data = PlayerDataCache.getCachedData(player.uniqueId)
        return data?.money?.toDouble() ?: 0.0
    }
    
    fun setBalance(player: Player, amount: Double) {
        val longAmount = amount.toLong()
        PlayerDataService.updatePlayerMoney(player, longAmount)
    }
    
    fun addBalance(player: Player, amount: Double): Boolean {
        val current = getBalance(player)
        setBalance(player, current + amount)
        return true
    }
    
    fun removeBalance(player: Player, amount: Double): Boolean {
        val current = getBalance(player)
        if (current < amount) return false
        
        setBalance(player, current - amount)
        return true
    }
}
```

## 注意事项
- 数据库操作应在异步线程中进行
- 缓存系统可以提高性能但要注意内存使用
- 定期备份重要的玩家数据
- 数据库连接失败时要有合适的错误处理
- 大量数据查询时考虑分页
- 注意数据库字段类型和长度限制