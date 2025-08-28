# Database 模块

TabooLib 的 Database 模块提供了统一的数据库操作接口，支持 MySQL 和 SQLite 数据库，是构建插件数据持久化的核心模块。

## 核心特性

- 支持 MySQL 和 SQLite 双数据库
- 自动数据表创建和管理
- 玩家数据容器系统
- 事务管理和批量操作
- 简化的数据操作 API

## 基础配置

### 1. 添加模块依赖

在 `build.gradle.kts` 中添加 Database 模块：

```kotlin
dependencies {
    // Database 模块
    taboo("module-database")
}
```

### 2. 配置文件设置

在 `config.yml` 中配置数据库连接信息：

```yaml
database:
  enable: true              # true 使用 MySQL，false 使用 SQLite
  host: localhost          # MySQL 主机地址
  port: 3306              # MySQL 端口
  user: root              # MySQL 用户名
  password: password      # MySQL 密码
  database: plugin_db     # 数据库名
  table: player_data      # 表名前缀
```

## 数据库初始化

### 创建数据库类

继承 `PlayerDatabase` 创建自定义数据库类：

```kotlin
object PluginDatabase : PlayerDatabase() {
    
    @Awake(LifeCycle.ENABLE)
    fun enable() {
        // 初始化数据库
        // 参数：配置节点, 表名, SQLite文件路径
        init(plugin.config.getConfigurationSection("database")!!, "plugin_data", "data/database.db")
    }
}
```

### 数据库连接原理

- 当 `enable: true` 时，使用 MySQL 连接
- 当 `enable: false` 时，使用 SQLite 文件数据库
- 初始化时会自动创建必要的数据表

## 数据操作 API

### 1. 获取数据容器

```kotlin
fun handlePlayerData(player: Player) {
    // 获取玩家数据容器
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    
    // 数据容器提供了键值对操作接口
}
```

### 2. 数据查询

```kotlin
fun queryData(player: Player) {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    
    // 查询字符串数据
    val playerName = container["name"] ?: "Unknown"
    
    // 查询数字数据
    val level = container["level"]?.toIntOrNull() ?: 1
    
    // 查询布尔数据
    val isVip = container["vip"]?.toBooleanStrictOrNull() ?: false
}
```

### 3. 数据插入/更新

```kotlin
fun updateData(player: Player) {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    
    // 设置/更新数据
    container["name"] = player.name
    container["level"] = "10"
    container["vip"] = "true"
    container["last_login"] = System.currentTimeMillis().toString()
}
```

### 4. 数据删除

```kotlin
fun deleteData(player: Player) {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    
    // 删除特定键值
    container["temp_data"] = ""
    
    // 或使用 remove 方法
    container.remove("temp_data")
}
```

## 高级数据表操作

### 1. 自定义数据表

```kotlin
// 创建自定义表结构
val customTable = Table("custom_table", host) {
    add("id") {
        type(SQLColumnType.INT, 11) {
            options(SQLColumnOption.PRIMARY_KEY, SQLColumnOption.AUTO_INCREMENT)
        }
    }
    add("player_uuid") {
        type(SQLColumnType.VARCHAR, 36) {
            options(SQLColumnOption.UNIQUE_KEY)
        }
    }
    add("data") {
        type(SQLColumnType.TEXT)
    }
    add("created_time") {
        type(SQLColumnType.TIMESTAMP) {
            def("CURRENT_TIMESTAMP")
        }
    }
}

// 创建表
customTable.createTable(dataSource)
```

### 2. 直接 SQL 操作

```kotlin
fun customQuery() {
    val dataSource = PluginDatabase.dataSource
    
    // 查询操作
    customTable.select(dataSource) {
        where("player_uuid", player.uniqueId.toString())
    }.forEach { row ->
        val data = row["data"].asString()
        val createTime = row["created_time"].asLong()
    }
    
    // 插入操作
    customTable.insert(dataSource) {
        value("player_uuid", player.uniqueId.toString())
        value("data", "some data")
    }
    
    // 更新操作
    customTable.update(dataSource) {
        set("data", "updated data")
        where("player_uuid", player.uniqueId.toString())
    }
    
    // 删除操作
    customTable.delete(dataSource) {
        where("player_uuid", player.uniqueId.toString())
    }
}
```

## 事务管理

### 批量操作事务

```kotlin
fun batchOperations(players: List<Player>) {
    // 开启事务
    customTable.transaction(dataSource) {
        players.forEach { player ->
            // 批量插入
            insert {
                value("player_uuid", player.uniqueId.toString())
                value("data", "batch data")
            }
        }
        // 自动提交或回滚
    }
}
```

## 异步操作

### 结合 TabooLib 任务系统

```kotlin
fun asyncDatabaseOperation(player: Player) {
    // 异步执行数据库操作
    submitAsync {
        val container = PluginDatabase.getDataContainer(adaptPlayer(player))
        container["async_data"] = "processed"
        
        // 切换回主线程处理结果
        submit {
            player.sendMessage("数据已异步保存")
        }
    }
}
```

## 数据迁移和备份

### 数据导出

```kotlin
fun exportPlayerData(player: Player): Map<String, String> {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    val exportData = mutableMapOf<String, String>()
    
    // 遍历所有数据
    container.keys.forEach { key ->
        exportData[key] = container[key] ?: ""
    }
    
    return exportData
}
```

### 数据导入

```kotlin
fun importPlayerData(player: Player, data: Map<String, String>) {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    
    data.forEach { (key, value) ->
        container[key] = value
    }
}
```

## 最佳实践

### 1. 数据结构设计

```kotlin
object PlayerDataKeys {
    const val LEVEL = "level"
    const val EXP = "exp"
    const val VIP = "vip"
    const val LAST_LOGIN = "last_login"
    const val SETTINGS = "settings"
}

fun setPlayerLevel(player: Player, level: Int) {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    container[PlayerDataKeys.LEVEL] = level.toString()
}
```

### 2. 数据验证

```kotlin
fun safeGetPlayerLevel(player: Player): Int {
    val container = PluginDatabase.getDataContainer(adaptPlayer(player))
    return container[PlayerDataKeys.LEVEL]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
}
```

### 3. 批量数据处理

```kotlin
fun batchUpdatePlayerData(updates: Map<Player, Map<String, String>>) {
    submitAsync {
        updates.forEach { (player, data) ->
            val container = PluginDatabase.getDataContainer(adaptPlayer(player))
            data.forEach { (key, value) ->
                container[key] = value
            }
        }
    }
}
```

## 性能优化建议

1. **异步操作**：将数据库操作放在异步任务中执行
2. **批量处理**：使用事务进行批量数据操作
3. **连接池管理**：合理配置数据库连接池大小
4. **索引优化**：为经常查询的字段添加索引
5. **数据缓存**：对频繁访问的数据进行内存缓存

## 错误处理

### 异常捕获

```kotlin
fun safeDataOperation(player: Player) {
    try {
        val container = PluginDatabase.getDataContainer(adaptPlayer(player))
        container["data"] = "value"
    } catch (e: Exception) {
        plugin.logger.warning("数据库操作失败: ${e.message}")
        // 降级处理或重试逻辑
    }
}
```

Database 模块为 TabooLib 插件提供了完整的数据持久化解决方案，通过简单的 API 即可实现复杂的数据库操作，是开发功能丰富插件的重要基础。