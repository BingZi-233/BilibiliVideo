# TabooLib Configuration 模块详解

TabooLib Configuration 模块提供了强大的配置文件管理功能，支持自动加载、类型转换、热重载等特性。本文档详细介绍该模块的核心功能和使用方法。

## 目录
- [核心注解](#核心注解)
  - [@Config 注解](#config-注解)
  - [@ConfigNode 注解](#confignode-注解)
- [核心类](#核心类)
  - [Configuration 类](#configuration-类)
  - [ConfigNodeTransfer 类](#confignodetransfer-类)
- [高级用法](#高级用法)
- [最佳实践](#最佳实践)
- [常见问题](#常见问题)

## 核心注解

### @Config 注解

`@Config` 注解用于将类中的字段与配置文件进行绑定，实现配置文件的自动加载和管理。

#### 注解定义

```kotlin
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val value: String = "config.yml",
    val target: String = "",
    val migrate: Boolean = false,
    val autoReload: Boolean = false,
    val concurrent: Boolean = true,
)
```

#### 基本用法

```kotlin
class MyPlugin : Plugin() {
    @Config("config.yml")
    lateinit var config: Configuration
        private set
    
    @Config("messages.yml")
    lateinit var messages: Configuration
        private set
}
```

#### 注解属性

- **value** (默认: `"config.yml"`): 指定配置文件名
- **target** (默认: `""`): 指定资源文件在插件数据文件夹中的目标路径，为空时使用 value 的值
- **migrate** (默认: `false`): 是否启用配置文件迁移功能
- **autoReload** (默认: `false`): 是否启用文件变更自动重载
- **concurrent** (默认: `true`): 是否支持并发操作

#### 高级配置示例

```kotlin
class AdvancedConfig {
    // 基础配置文件
    @Config("config.yml")
    lateinit var mainConfig: Configuration
        private set
    
    // 自定义目标路径
    @Config("default-settings.yml", target = "configs/custom.yml")
    lateinit var customConfig: Configuration
        private set
    
    // 启用自动重载
    @Config("settings.yml", autoReload = true)
    lateinit var settings: Configuration
        private set
    
    // 禁用并发访问
    @Config("data.yml", concurrent = false)
    lateinit var dataConfig: Configuration
        private set
    
    // 启用配置迁移
    @Config("player-data.yml", migrate = true)
    lateinit var playerData: Configuration
        private set
}
```

#### 属性详解

##### target 属性
`target` 属性允许您指定资源文件在插件数据文件夹中的具体存储路径：

```kotlin
// 将 jar 包内的 default-settings.yml 释放到 plugins/YourPlugin/configs/custom.yml
@Config("default-settings.yml", target = "configs/custom.yml")
lateinit var customConfig: Configuration
```

##### migrate 属性
`migrate` 属性用于启用配置文件迁移功能，当配置文件版本更新时自动迁移旧版本的配置：

```kotlin
// 启用配置迁移，自动处理配置文件版本升级
@Config("player-data.yml", migrate = true)
lateinit var playerData: Configuration
```

配置迁移的工作流程：
1. 检测配置文件版本
2. 如果发现新版本的配置模板，自动备份旧配置
3. 迁移可兼容的配置项到新版本
4. 保留用户的自定义配置值

##### autoReload 属性
启用后，TabooLib 会监听配置文件的变化并自动重载：

```kotlin
@Config("dynamic-config.yml", autoReload = true)
lateinit var dynamicConfig: Configuration
```

##### concurrent 属性
控制 Configuration 实例是否支持多线程并发访问：

```kotlin
// 适用于高频读写的配置
@Config("cache.yml", concurrent = true)  // 默认值

// 适用于单线程访问的配置，性能更好
@Config("static-data.yml", concurrent = false)
```

#### 工作原理

1. **初始化阶段**: `ConfigLoader` 在 `LifeCycle.INIT` 阶段扫描所有带有 `@Config` 注解的字段
2. **文件处理**: 自动释放资源文件到插件数据文件夹，如果文件不存在则创建默认文件
3. **实例注入**: 加载 `Configuration` 实例并注入到对应字段
4. **监听机制**: 如果启用了 `autoReload`，会注册 `FileWatcher` 监听文件变更

### @ConfigNode 注解

`@ConfigNode` 注解用于将配置文件中的特定节点值直接映射到类的字段上，支持类型转换和缓存机制。

#### 注解定义

```kotlin
@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.FILE)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConfigNode(val value: String = "", val bind: String = "config.yml")
```

#### 基本用法

```kotlin
class PlayerSettings {
    @ConfigNode("player.max-health")
    var maxHealth: Double = 20.0
    
    @ConfigNode("player.speed", bind = "settings.yml")
    var speed: Float = 0.1f
    
    @ConfigNode("messages.welcome")
    var welcomeMessage: String = "Welcome!"
}
```

#### 注解属性

- **value** (默认: `""`): 配置节点路径，如果为空则使用字段名转换为配置路径
- **bind** (默认: `"config.yml"`): 绑定的配置文件名，指定从哪个配置文件读取数据

#### 作用目标

@ConfigNode 注解可以应用于：
- **FIELD**: 类的字段/属性
- **CLASS**: 类级别（为该类的所有字段设置默认配置文件）
- **FILE**: 文件级别

#### 类级别和文件级别使用

```kotlin
// 类级别：为整个类设置默认配置文件
@ConfigNode(bind = "player-settings.yml")
class PlayerConfig {
    @ConfigNode("max-health")  // 从 player-settings.yml 读取
    var maxHealth: Double = 20.0
    
    @ConfigNode("speed")       // 从 player-settings.yml 读取
    var speed: Float = 0.1f
    
    @ConfigNode("mana", bind = "magic.yml")  // 覆盖类级别设置，从 magic.yml 读取
    var mana: Int = 100
}

// 文件级别：影响整个文件中的类
@file:ConfigNode(bind = "global-settings.yml")
package com.example.plugin.config
```

#### 自动路径转换

如果不指定 `value` 属性，字段名会自动转换为配置路径：

```kotlin
class AutoPathExample {
    // 自动映射到配置路径 "max-health"
    @ConfigNode
    var maxHealth: Int = 100
    
    // 自动映射到配置路径 "player-limit"
    @ConfigNode
    var playerLimit: Int = 50
}
```

## 核心类

### Configuration 接口

`Configuration` 接口是 TabooLib 对配置文件的核心封装，继承自 `ConfigurationSection`，提供了丰富的 API 来操作配置数据。

#### 接口定义

```kotlin
interface Configuration : ConfigurationSection {
    /**
     * 文件
     */
    var file: File?

    /**
     * 保存为字符串
     */
    fun saveToString(): String

    /**
     * 保存到文件
     */
    fun saveToFile(file: File? = null)

    /**
     * 从文件加载
     */
    fun loadFromFile(file: File)

    /**
     * 从字符串加载
     */
    fun loadFromString(contents: String)

    /**
     * 从 Reader 加载
     */
    fun loadFromReader(reader: Reader)

    /**
     * 从 InputStream 加载
     */
    fun loadFromInputStream(inputStream: InputStream)

    /**
     * 重载
     */
    fun reload()

    /**
     * 注册重载回调
     */
    fun onReload(runnable: Runnable)

    /**
     * 变更类型
     */
    fun changeType(type: Type)
}
```

#### 创建 Configuration 实例

```kotlin
// 创建空配置
val config = Configuration.empty(Type.YAML, concurrent = true)

// 从文件加载
val config = Configuration.loadFromFile(File("config.yml"))

// 从字符串加载
val config = Configuration.loadFromString("""
    server:
      port: 25565
      host: localhost
""", Type.YAML)

// 从 Map 创建
val configData = mapOf("key" to "value", "number" to 42)
val config = Configuration.fromMap(configData)
```

#### 支持的配置类型

TabooLib 支持多种配置文件格式：

```kotlin
enum class Type {
    YAML,    // .yml, .yaml
    JSON,    // .json  
    TOML,    // .toml, .tml
    HOCON    // .conf
}

// 根据文件扩展名自动识别类型
val type = Configuration.getTypeFromFile(File("config.yml"))  // Type.YAML
val type = Configuration.getTypeFromExtension("json")          // Type.JSON
```

#### 基本操作

```kotlin
// 获取值
val message = config.getString("messages.welcome", "Default message")
val count = config.getInt("settings.max-players", 20)
val enabled = config.getBoolean("features.pvp", true)

// 设置值
config.set("settings.max-players", 50)
config.set("messages.goodbye", "See you later!")

// 保存配置
config.saveToFile()

// 重载配置
config.reload()

// 注册重载回调
config.onReload {
    println("配置已重新加载！")
}
```

#### 对象序列化和反序列化

TabooLib Configuration 支持将 Kotlin 对象直接序列化到配置文件，或从配置文件反序列化为对象：

```kotlin
// 定义数据类
data class PlayerData(
    val name: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
    val items: List<String> = emptyList()
)

data class ServerSettings(
    val maxPlayers: Int = 20,
    val motd: String = "Welcome",
    val worldSettings: Map<String, Any> = emptyMap()
)

// 序列化对象到配置
val playerData = PlayerData("Steve", 50, 1250.5, listOf("sword", "pickaxe"))
config.setObject("players.steve", playerData)

// 反序列化配置到对象
val loadedPlayer = config.getObject<PlayerData>("players.steve")

// 将整个配置节转换为对象
val settings = config.toObject<ServerSettings>()

// 使用自定义对象作为基础进行反序列化（保留现有值）
val existingPlayer = PlayerData("Alex", 25)
val updatedPlayer = config.getObject("players.alex", existingPlayer)

// 序列化对象为独立的 ConfigurationSection
val serializedData = Configuration.serialize(playerData, Type.YAML)
```

#### 序列化控制选项

```kotlin
// 忽略构造函数进行反序列化（适用于没有默认构造函数的类）
val player = config.getObject<PlayerData>("players.steve", ignoreConstructor = true)

// 创建新的 Configuration 实例从其他配置对象
val newConfig = Configuration.loadFromOther(bukkitConfig, Type.YAML)
```

#### 列表和复杂类型

```kotlin
// 获取列表
val worlds = config.getStringList("allowed-worlds")
val coordinates = config.getIntegerList("spawn.coordinates")

// 获取配置节
val playerSection = config.getConfigurationSection("player")
playerSection?.let { section ->
    val health = section.getDouble("health")
    val mana = section.getInt("mana")
}
```

### ConfigNodeTransfer 类

`ConfigNodeTransfer` 提供了配置值的类型转换和缓存机制，支持懒加载模式。

#### 基本使用

```kotlin
class ItemConfig {
    @ConfigNode("items.sword")
    val swordConfig: ConfigNodeTransfer<Map<String, Any>, ItemStack> = conversion { configMap ->
        // 将配置 Map 转换为 ItemStack
        ItemStack(Material.valueOf(configMap["material"] as String)).apply {
            itemMeta = itemMeta?.apply {
                displayName = configMap["name"] as String
                lore = configMap["lore"] as List<String>
            }
        }
    }
    
    // 使用懒加载模式
    @ConfigNode("custom-recipes")
    val recipes: ConfigNodeTransfer<List<Map<String, Any>>, List<Recipe>> = lazyConversion { recipeList ->
        recipeList.map { recipeMap ->
            // 转换逻辑
            createRecipeFromMap(recipeMap)
        }
    }
}
```

#### 创建 ConfigNodeTransfer

```kotlin
// 普通模式 - 立即转换
val immediateTransfer = conversion<String, ItemStack> { materialName ->
    ItemStack(Material.valueOf(materialName))
}

// 懒加载模式 - 首次访问时才转换
val lazyTransfer = lazyConversion<List<String>, List<Player>> { playerNames ->
    playerNames.mapNotNull { Bukkit.getPlayer(it) }
}

// 手动创建
val customTransfer = ConfigNodeTransfer<String, UUID>(isLazyMode = true) { playerName ->
    Bukkit.getOfflinePlayer(playerName).uniqueId
}
```

#### 核心方法

```kotlin
// 获取转换后的值
val result = transfer.get()

// 重置缓存并更新原始值
transfer.reset(newConfigValue)

// 作为属性委托使用
class MyConfig {
    @ConfigNode("player-data")
    val playerData by lazyConversion<Map<String, Any>, PlayerData> { dataMap ->
        PlayerData.fromMap(dataMap)
    }
}
```

## 高级用法

### 多文件配置管理

```kotlin
object ConfigManager {
    @Config("config.yml")
    lateinit var mainConfig: Configuration
    
    @Config("database.yml")
    lateinit var dbConfig: Configuration
    
    @Config("messages.yml", autoReload = true)
    lateinit var messages: Configuration
    
    // 集中管理配置节点
    @ConfigNode("server.port", bind = "config.yml")
    var serverPort: Int = 25565
    
    @ConfigNode("database.url", bind = "database.yml")
    var databaseUrl: String = "jdbc:mysql://localhost:3306/test"
    
    @ConfigNode("messages.prefix", bind = "messages.yml")
    var messagePrefix: String = "&7[&aServer&7] "
}
```

### 自定义类型转换

```kotlin
class PlayerConfig {
    @ConfigNode("spawn.location")
    val spawnLocation: ConfigNodeTransfer<Map<String, Any>, Location> = conversion { locMap ->
        Location(
            Bukkit.getWorld(locMap["world"] as String),
            (locMap["x"] as Number).toDouble(),
            (locMap["y"] as Number).toDouble(),
            (locMap["z"] as Number).toDouble(),
            (locMap["yaw"] as Number).toFloat(),
            (locMap["pitch"] as Number).toFloat()
        )
    }
    
    @ConfigNode("allowed-items")
    val allowedItems: ConfigNodeTransfer<List<String>, Set<Material>> = lazyConversion { itemNames ->
        itemNames.mapNotNull { 
            try { Material.valueOf(it.uppercase()) } 
            catch (e: Exception) { null }
        }.toSet()
    }
}
```

### 配置文件热重载

```kotlin
class ReloadableConfig {
    @Config("dynamic.yml", autoReload = true)
    lateinit var dynamicConfig: Configuration
        private set
    
    @ConfigNode("settings.debug-mode", bind = "dynamic.yml")
    var debugMode: Boolean = false
    
    // 当文件变更时，ConfigNodeTransfer 会自动重置缓存
    @ConfigNode("features", bind = "dynamic.yml")
    val enabledFeatures: ConfigNodeTransfer<List<String>, Set<Feature>> = lazyConversion { featureNames ->
        featureNames.mapNotNull { Feature.valueOf(it) }.toSet()
    }
}
```

## 最佳实践

### 1. 配置类结构

```kotlin
object PluginConfig {
    // 使用 lateinit var 和 private set
    @Config("config.yml")
    lateinit var config: Configuration
        private set
    
    // 统一管理相关配置
    @ConfigNode("server.max-players")
    var maxPlayers: Int = 100
    
    @ConfigNode("server.motd")
    var motd: String = "Welcome to the server!"
    
    // 使用 ConfigNodeTransfer 处理复杂类型
    @ConfigNode("spawn.location")
    val spawnLocation by lazyConversion<Map<String, Any>, Location> { locData ->
        LocationHelper.fromMap(locData)
    }
}
```

### 2. 配置验证

```kotlin
class ValidatedConfig {
    @ConfigNode("settings.port")
    private var _port: Int = 25565
    
    val port: Int
        get() = _port.coerceIn(1024, 65535)
    
    @ConfigNode("settings.max-players")
    private var _maxPlayers: Int = 20
    
    val maxPlayers: Int
        get() = _maxPlayers.coerceAtLeast(1)
}
```

### 3. 国际化配置

```kotlin
class I18nConfig {
    @Config("messages_en.yml")
    lateinit var englishMessages: Configuration
    
    @Config("messages_zh.yml")  
    lateinit var chineseMessages: Configuration
    
    fun getMessage(key: String, locale: String = "en"): String {
        val config = when(locale) {
            "zh" -> chineseMessages
            else -> englishMessages
        }
        return config.getString(key, key)
    }
}
```

### 4. 错误处理

```kotlin
class SafeConfig {
    @ConfigNode("database.url")
    val databaseUrl: ConfigNodeTransfer<String, String> = conversion { url ->
        if (url.isBlank()) {
            throw IllegalArgumentException("Database URL cannot be empty")
        }
        url
    }
    
    @ConfigNode("items.list")
    val items: ConfigNodeTransfer<List<String>, List<Material>> = lazyConversion { itemNames ->
        itemNames.mapNotNull { name ->
            try {
                Material.valueOf(name.uppercase())
            } catch (e: IllegalArgumentException) {
                plugin.logger.warning("Invalid material name: $name")
                null
            }
        }
    }
}
```

## 常见问题

### Q: 配置文件没有自动创建怎么办？

A: 确保资源文件放在 `src/main/resources` 目录下，文件名与 `@Config` 注解中指定的名称一致。

### Q: @ConfigNode 字段始终是默认值？

A: 检查以下几点：
1. 配置文件是否存在对应节点
2. `bind` 属性是否正确指定了配置文件名
3. 节点路径是否正确（注意大小写和分隔符）

### Q: ConfigNodeTransfer 什么时候使用懒加载？

A: 在以下情况建议使用懒加载模式：
- 转换操作比较耗时
- 转换后的对象占用内存较大
- 转换后的值可能不会立即被使用

### Q: 如何调试配置加载问题？

A: 可以在插件启动时添加日志：

```kotlin
object DebugConfig {
    @Config("config.yml")
    lateinit var config: Configuration
        private set
    
    fun init() {
        plugin.logger.info("Config loaded: ${::config.isInitialized}")
        plugin.logger.info("Config file exists: ${config.file.exists()}")
        plugin.logger.info("Config keys: ${config.getKeys(false)}")
    }
}
```

### Q: 多个插件使用相同配置文件名会冲突吗？

A: 不会冲突。每个插件的配置文件都存储在各自的插件数据文件夹中（`plugins/插件名/`）。

## 示例项目

以下是一个完整的数据库配置管理示例，来自 BilibiliVideo 插件的实际实现：

```kotlin
/**
 * 数据库配置管理类
 * 负责为 TabooLib Database 模块提供连接对象
 */
object DatabaseConfig {
    @Config("database.yml")
    lateinit var config: Configuration
        private set

    // 基本配置
    @ConfigNode(value = "database.enable", bind = "database.yml")
    var enable: Boolean = false

    // MySQL 配置
    @ConfigNode(value = "database.mysql.host", bind = "database.yml")
    var mysqlHost: String = "localhost"

    @ConfigNode(value = "database.mysql.port", bind = "database.yml")
    var mysqlPort: Int = 3306

    @ConfigNode(value = "database.mysql.database", bind = "database.yml")
    var mysqlDatabase: String = "bilibili_video"

    @ConfigNode(value = "database.mysql.username", bind = "database.yml")
    var mysqlUsername: String = "root"

    @ConfigNode(value = "database.mysql.password", bind = "database.yml")
    var mysqlPassword: String = ""

    @ConfigNode(value = "database.mysql.use-ssl", bind = "database.yml")
    var mysqlUseSsl: Boolean = false

    @ConfigNode(value = "database.mysql.charset", bind = "database.yml")
    var mysqlCharset: String = "utf8mb4"

    // SQLite 配置
    @ConfigNode(value = "database.sqlite.file", bind = "database.yml")
    var sqliteFile: String = "data/database.db"

    // 数据表配置
    @ConfigNode(value = "database.table.prefix", bind = "database.yml")
    var tablePrefix: String = "bv_"

    // 高级配置
    @ConfigNode(value = "database.advanced.auto-reconnect", bind = "database.yml")
    var advancedAutoReconnect: Boolean = true

    /**
     * 创建 TabooLib Database 模块所需的 Host 对象
     * @return Host<*> 对象，根据配置返回 HostSQL 或 HostSQLite
     */
    fun createHost(): Host<*> {
        return if (enable) {
            // 创建 MySQL Host
            val host = HostSQL(
                host = mysqlHost,
                port = mysqlPort.toString(),
                user = mysqlUsername,
                password = mysqlPassword,
                database = mysqlDatabase
            )

            // 配置连接参数
            host.flags.clear()
            host.flags.add("characterEncoding=$mysqlCharset")
            host.flags.add("useSSL=$mysqlUseSsl")
            host.flags.add("allowPublicKeyRetrieval=true") // 针对 MySQL8
            if (advancedAutoReconnect) {
                host.flags.add("autoReconnect=true")
            }

            host
        } else {
            // 创建 SQLite Host
            HostSQLite(getSqliteFile())
        }
    }

    /**
     * 创建 DataSource 对象
     * @param autoRelease 是否自动释放，默认为 true
     * @param withoutConfig 是否不使用配置，默认为 false
     * @return DataSource 对象
     */
    fun createDataSource(autoRelease: Boolean = true, withoutConfig: Boolean = false): DataSource {
        return createHost().createDataSource(autoRelease, withoutConfig)
    }

    /**
     * 获取带前缀的表名
     * @param tableName 原表名
     * @return 带前缀的完整表名
     */
    fun getTableName(tableName: String): String {
        return tablePrefix + tableName
    }

    /**
     * 获取 SQLite 数据库文件
     */
    private fun getSqliteFile(): File {
        val file = if (sqliteFile.startsWith("/")) {
            File(sqliteFile)
        } else {
            newFile(getDataFolder(), sqliteFile)
        }

        // 确保父目录存在
        file.parentFile?.mkdirs()

        return file
    }
}
```

### 该示例的配置文件结构

对应的 `database.yml` 配置文件结构：

```yaml
database:
  enable: false  # true: MySQL, false: SQLite
  
  mysql:
    host: localhost
    port: 3306
    database: bilibili_video
    username: root
    password: ""
    use-ssl: false
    charset: utf8mb4
  
  sqlite:
    file: "data/database.db"
  
  table:
    prefix: "bv_"
  
  advanced:
    auto-reconnect: true
```

### 示例特点说明

这个数据库配置管理类展示了以下 TabooLib Configuration 模块的核心特性：

1. **单一配置文件绑定**: 所有字段都使用 `bind = "database.yml"` 绑定到同一个配置文件
2. **层次化配置路径**: 使用点号分隔的路径如 `database.mysql.host` 映射到嵌套的 YAML 结构
3. **类型自动转换**: 支持 String、Int、Boolean 等基本类型的自动转换
4. **默认值设置**: 每个配置项都提供了合理的默认值
5. **业务逻辑集成**: `createHost()` 方法展示了如何将配置数据转换为业务对象
6. **文件路径处理**: `getSqliteFile()` 方法展示了相对路径和绝对路径的处理
7. **TabooLib 集成**: 与 TabooLib Database 模块无缝集成，支持 MySQL/SQLite 双数据库

这个配置系统为 BilibiliVideo 插件提供了完整的数据库配置管理功能，支持生产环境的 MySQL 和开发环境的 SQLite 无缝切换，确保插件配置的灵活性和可维护性。