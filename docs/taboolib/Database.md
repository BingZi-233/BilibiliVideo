# Database 模块

TabooLib 的 Database 模块提供了统一的数据库操作接口，支持 MySQL 和 SQLite 数据库，是构建插件数据持久化的核心模块。

## 核心特性

- 支持 MySQL 和 SQLite 双数据库
- 自动数据表创建和管理  
- 跨数据库兼容的表结构定义
- 联合索引和约束支持
- 基于Host类型的自动适配

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
  tablePrefix: plugin_    # 表名前缀
```

## 跨数据库表结构定义

### 统一表定义模式

使用 `object` 单例模式定义表结构，根据 `Host` 类型自动适配不同数据库：

```kotlin
object CustomTable {
    
    /**
     * 创建表
     * 根据Host类型自动适配MySQL或SQLite
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("custom_table")

        return when (host) {
            is HostSQL -> {
                // MySQL实现
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.AUTO_INCREMENT)
                        }
                    }
                    add("player_uuid") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("data") {
                        type(ColumnTypeSQL.TEXT)
                    }
                    add("create_time") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            
            is HostSQLite -> {
                // SQLite实现
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.PRIMARY_KEY, ColumnOptionSQLite.AUTOINCREMENT)
                        }
                    }
                    add("player_uuid") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("data") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("create_time") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }.also { table ->
                    // SQLite中使用索引实现唯一约束
                    table.index("unique_player_uuid", listOf("player_uuid"), unique = true)
                }
            }
            
            else -> {
                throw IllegalArgumentException("unknown database type")
            }
        }
    }
}
```

### 类型映射对照

#### 字符串类型
- MySQL: `ColumnTypeSQL.VARCHAR(length)`、`ColumnTypeSQL.TEXT`
- SQLite: `ColumnTypeSQLite.TEXT`

#### 数值类型  
- MySQL: `ColumnTypeSQL.INT`、`ColumnTypeSQL.BIGINT`
- SQLite: `ColumnTypeSQLite.INTEGER`

#### 布尔类型
- MySQL: `ColumnTypeSQL.BOOLEAN`（原生布尔支持）
- SQLite: `ColumnTypeSQLite.INTEGER`（使用0/1表示）

#### 时间戳
- 推荐使用 `BIGINT`/`INTEGER` 存储毫秒时间戳
- 使用 `System.currentTimeMillis()` 获取

### 约束和索引

#### MySQL约束
```kotlin
// 主键
options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.AUTO_INCREMENT)

// 唯一约束
options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL)

// 非空约束
options(ColumnOptionSQL.NOTNULL)

// 默认值
def(false)  // 布尔默认值
def(0)      // 数值默认值
```

#### SQLite约束
```kotlin
// 主键
options(ColumnOptionSQLite.PRIMARY_KEY, ColumnOptionSQLite.AUTOINCREMENT)

// 非空约束
options(ColumnOptionSQLite.NOTNULL)

// 唯一约束通过索引实现
table.index("unique_column", listOf("column"), unique = true)

// 默认值  
def(0)      // 布尔值用0/1表示
```

#### 联合索引
```kotlin
// MySQL和SQLite通用
table.index("composite_index", listOf("column1", "column2", "column3"), unique = true)
```

## 表操作API

### 创建和管理表
```kotlin
// 在DatabaseManager中初始化表
val table = CustomTable.createTable(host)
table.createTable(dataSource)
```

### 数据库CRUD操作

```kotlin
fun databaseOperations() {
    val table = CustomTable.createTable(host)
    val dataSource = DatabaseManager.dataSource
    
    // 查询操作
    table.select(dataSource) {
        where("player_uuid", playerUuid)
    }.forEach { row ->
        val data = row["data"].asString()
        val createTime = row["create_time"].asLong()
    }
    
    // 插入操作
    table.insert(dataSource) {
        value("player_uuid", playerUuid)
        value("data", "some data")
        value("create_time", System.currentTimeMillis())
    }
    
    // 更新操作
    table.update(dataSource) {
        set("data", "updated data")
        set("update_time", System.currentTimeMillis())
        where("player_uuid", playerUuid)
    }
    
    // 删除操作
    table.delete(dataSource) {
        where("player_uuid", playerUuid)
    }
}
```

## 事务管理

### 批量操作事务

```kotlin
fun batchOperations(players: List<Player>) {
    val table = CustomTable.createTable(host)
    
    // 开启事务
    table.transaction(dataSource) {
        players.forEach { player ->
            // 批量插入
            insert {
                value("player_uuid", player.uniqueId.toString())
                value("data", "batch data")
                value("create_time", System.currentTimeMillis())
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
        val table = CustomTable.createTable(host)
        
        table.insert(dataSource) {
            value("player_uuid", player.uniqueId.toString())
            value("data", "async_data")
            value("create_time", System.currentTimeMillis())
        }
        
        // 切换回主线程处理结果
        submit {
            player.sendMessage("数据已异步保存")
        }
    }
}
```

## 实际应用示例

### Bilibili 数据表实现

#### 玩家-MID绑定表
```kotlin
object PlayerBindingTable {
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("player_binding")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("player_uuid") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.PRIMARY_KEY)
                        }
                    }
                    add("mid") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.UNIQUE_KEY, ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("player_uuid") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.PRIMARY_KEY)
                        }
                    }
                    add("mid") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }.also { table ->
                    table.index("unique_mid", listOf("mid"), unique = true)
                }
            }
            else -> throw IllegalArgumentException("unknown database type")
        }
    }
}
```

#### 视频三连状态表
```kotlin
object VideoTripleStatusTable {
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("video_triple_status")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.AUTO_INCREMENT)
                        }
                    }
                    add("bvid") {
                        type(ColumnTypeSQL.VARCHAR, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("is_liked") {
                        type(ColumnTypeSQL.BOOLEAN) {
                            options(ColumnOptionSQL.NOTNULL)
                            def(false)
                        }
                    }
                }.also { table ->
                    table.index("unique_video_triple", listOf("bvid", "mid", "player_uuid"), unique = true)
                }
            }
            is HostSQLite -> {
                Table(tableName, host) {
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.PRIMARY_KEY, ColumnOptionSQLite.AUTOINCREMENT)
                        }
                    }
                    add("bvid") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("is_liked") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def(0)
                        }
                    }
                }.also { table ->
                    table.index("unique_video_triple", listOf("bvid", "mid", "player_uuid"), unique = true)
                }
            }
            else -> throw IllegalArgumentException("unknown database type")
        }
    }
}
```

## 最佳实践

### 1. 表结构设计原则
- 使用 `object` 单例模式定义表结构
- 在 `createTable` 方法中根据 `Host` 类型适配不同数据库
- 为每个表添加必要的时间戳字段（create_time、update_time）
- 使用联合索引优化查询性能

### 2. 数据类型选择
- 时间戳：使用 `BIGINT`/`INTEGER` 存储毫秒时间戳
- 布尔值：MySQL使用 `BOOLEAN`，SQLite使用 `INTEGER(0/1)`
- UUID：统一使用36位 `VARCHAR`/`TEXT`
- 大整数：Bilibili MID等使用 `BIGINT`/`INTEGER`

### 3. 性能优化建议
- 异步操作：将数据库操作放在 `submitAsync` 中执行
- 批量处理：使用事务进行批量数据操作  
- 索引优化：为经常查询的字段组合添加联合索引
- 连接池管理：合理配置数据库连接池大小

### 4. 错误处理

```kotlin
fun safeDataOperation(player: Player) {
    try {
        submitAsync {
            val table = CustomTable.createTable(host)
            table.insert(dataSource) {
                value("player_uuid", player.uniqueId.toString())
                value("data", "value")
                value("create_time", System.currentTimeMillis())
            }
        }
    } catch (e: Exception) {
        plugin.logger.warning("数据库操作失败: ${e.message}")
        // 降级处理或重试逻辑
    }
}
```

## 统一数据库服务层

在 BilibiliVideo 项目中，我们通过 `DatabaseService` 对象提供统一的数据库操作 API，封装了所有异步数据库操作的复杂性。

### DatabaseService 设计模式

#### 核心特性
- **object 单例模式**：确保全局唯一的数据库服务实例
- **回调式异步 API**：所有操作使用 `callback: (Result) -> Unit` 模式
- **自动异步处理**：内部使用 `submitAsync` 执行数据库操作
- **统一错误处理**：捕获异常并使用 `severe()` 记录错误日志
- **主线程回调**：使用 `submit` 确保回调在主线程执行

#### API 设计模式示例

```kotlin
object DatabaseService {
    
    /**
     * 标准的异步数据库操作模式
     */
    fun bindPlayer(playerUuid: String, mid: Long, playerName: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 查询是否已存在记录
                val existingRecords = table.select(dataSource) {
                    where { "player_uuid" eq playerUuid }
                }
                
                val result = if (existingRecords.find()) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("mid", mid)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where { "player_uuid" eq playerUuid }
                    } > 0
                } else {
                    // 插入新记录
                    table.insert(dataSource) {
                        value("player_uuid", playerUuid)
                        value("mid", mid)
                        value("create_time", currentTime)
                        value("update_time", currentTime)
                        value("create_player", playerName)
                        value("update_player", playerName)
                    } > 0
                }
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("绑定玩家失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
}
```

### 实体对象构建模式

在 `DatabaseService` 中，复杂实体对象通过 `firstOrNull` 扩展函数构建：

```kotlin
val account = results.firstOrNull {
    BilibiliAccount(
        mid = getLong("mid"),
        nickname = getString("nickname"),
        sessdata = getString("sessdata"),
        buvid3 = getString("buvid3"),
        biliJct = getString("bili_jct"),
        refreshToken = getString("refresh_token"),
        createTime = getLong("create_time"),
        updateTime = getLong("update_time"),
        createPlayer = getString("create_player"),
        updatePlayer = getString("update_player")
    )
}
```

### 联合查询条件

支持多字段联合查询条件：

```kotlin
val existingRecords = table.select(dataSource) {
    where { "bvid" eq bvid; "mid" eq mid; "player_uuid" eq playerUuid }
}
```

### 完整的 CRUD 操作集

#### 1. 玩家绑定管理
```kotlin
// 绑定/更新玩家MID
DatabaseService.bindPlayer(playerUuid, mid, playerName) { success ->
    if (success) sendMessage("绑定成功")
}

// 查询玩家MID
DatabaseService.getPlayerMid(playerUuid) { mid ->
    mid?.let { sendMessage("玩家MID: $it") }
}

// 根据MID查询玩家
DatabaseService.getPlayerByMid(mid) { playerUuid ->
    playerUuid?.let { sendMessage("绑定玩家: $it") }
}

// 解除绑定
DatabaseService.unbindPlayer(playerUuid) { success ->
    if (success) sendMessage("已解除绑定")
}
```

#### 2. Bilibili账户信息管理
```kotlin
// 保存Cookie信息
DatabaseService.saveBilibiliAccount(
    mid, nickname, sessdata, buvid3, biliJct, refreshToken, playerName
) { success ->
    if (success) sendMessage("账户信息已保存")
}

// 获取账户信息
DatabaseService.getBilibiliAccount(mid) { account ->
    account?.let { 
        sendMessage("账户: ${it.nickname}")
        // 使用完整的Cookie信息进行API调用
    }
}

// 更新Cookie
DatabaseService.updateCookies(
    mid, sessdata, buvid3, biliJct, refreshToken, playerName
) { success ->
    if (success) sendMessage("Cookie已更新")
}
```

#### 3. 视频三连状态管理
```kotlin
// 保存视频状态
DatabaseService.saveVideoTripleStatus(
    bvid, mid, playerUuid, isLiked, isCoined, isFavorited, playerName
) { success ->
    if (success) sendMessage("视频状态已保存")
}

// 查询视频状态
DatabaseService.getVideoTripleStatus(bvid, mid, playerUuid) { status ->
    status?.let {
        sendMessage("点赞: ${it.isLiked}, 投币: ${it.isCoined}, 收藏: ${it.isFavorited}")
    }
}
```

#### 4. UP主关注状态管理
```kotlin
// 保存关注状态
DatabaseService.saveUpFollowStatus(
    upMid, followerMid, playerUuid, isFollowing, playerName
) { success ->
    if (success) sendMessage("关注状态已更新")
}

// 查询关注状态
DatabaseService.getUpFollowStatus(upMid, followerMid, playerUuid) { status ->
    status?.let {
        sendMessage("关注状态: ${it.isFollowing}")
    }
}
```

#### 5. 便捷查询方法
```kotlin
// 检查绑定状态
DatabaseService.isPlayerBound(playerUuid) { isBound ->
    if (!isBound) sendMessage("请先绑定Bilibili账户")
}

DatabaseService.isMidBound(mid) { isBound ->
    if (isBound) sendMessage("该账户已被绑定")
}
```

### 数据库服务层优势

1. **简化使用复杂度**：调用方无需关心异步处理细节
2. **统一错误处理**：所有数据库异常都有统一的日志记录
3. **线程安全保障**：回调确保在正确的线程中执行
4. **代码复用性**：避免重复的异步处理逻辑
5. **易于测试维护**：集中的数据库操作便于单元测试

Database 模块为 TabooLib 插件提供了完整的数据持久化解决方案，通过 DatabaseService 统一服务层，实现了简单易用的数据库操作 API，是开发功能丰富插件的重要基础。