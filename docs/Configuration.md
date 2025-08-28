# Configuration 模块

TabooLib 的 Configuration 模块提供了强大而简便的配置文件管理功能，支持多种格式的配置文件，并具备自动重载、类型安全等特性。

## 核心特性

- 支持 YAML、JSON、TOML、HOCON 等多种格式
- `@Config` 注解自动加载配置文件
- 自动文件监听和重载功能
- 类型安全的配置读写 API
- 注释管理和保存
- 配置节点转换和缓存机制

## 基础使用

### 1. 添加模块依赖

Configuration 模块通常包含在 Basic 模块中：

```kotlin
dependencies {
    // Basic 模块包含 Configuration
    taboo("module-basic")
}
```

### 2. @Config 注解详解

`@Config` 注解用于标记一个字段，使其在 TabooLib 启动时自动加载配置文件并赋值给该字段。

#### @Config 注解的属性

```kotlin
@Config(
    value = "config.yml",        // 配置文件名称，默认 config.yml
    target = "",                 // 目标路径，为空使用 value 值
    autoReload = false,          // 是否启用自动重载，默认 false
    concurrent = true            // 是否并发模式加载，默认 true
)
```

#### 基础用法

```kotlin
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object MyPlugin {
    
    // 基础用法 - 加载 config.yml
    @Config
    lateinit var config: Configuration
    
    // 指定文件名
    @Config("database.yml")
    lateinit var database: Configuration
    
    // 启用自动重载
    @Config("settings.yml", autoReload = true)
    lateinit var settings: Configuration
    
    // 指定目标路径和自动重载
    @Config(value = "lang.yml", target = "lang/zh_CN.yml", autoReload = true)
    lateinit var language: Configuration
}
```

### 3. Configuration 类详解

`Configuration` 类继承自 `ConfigurationSection`，提供了完整的配置文件操作 API。

#### 核心 API 方法

```kotlin
object ConfigExample {
    
    @Config("app.yml")
    lateinit var appConfig: Configuration
    
    fun demonstrateConfigAPI() {
        // 检查路径是否存在
        val hasPath = appConfig.contains("server.name")
        val isSet = appConfig.isSet("server.name")
        
        // 获取所有键
        val allKeys = appConfig.getKeys(false)  // 不包含子键
        val deepKeys = appConfig.getKeys(true)  // 包含所有子键
        
        // 基本类型读取
        val serverName = appConfig.getString("server.name")
        val maxPlayers = appConfig.getInt("server.max-players", 100)
        val enableFeature = appConfig.getBoolean("features.enable", false)
        val serverPort = appConfig.getLong("server.port", 25565L)
        val tickRate = appConfig.getDouble("server.tick-rate", 20.0)
        
        // 集合类型读取
        val adminList = appConfig.getStringList("admins")
        val itemList = appConfig.getList("items")
        val mapList = appConfig.getMapList("complex-data")
        
        // 枚举类型读取
        val difficulty = appConfig.getEnum("difficulty", Difficulty::class.java)
        
        // 获取子配置节
        val databaseSection = appConfig.getConfigurationSection("database")
        databaseSection?.let { section ->
            val host = section.getString("host", "localhost")
            val port = section.getInt("port", 3306)
        }
    }
}

enum class Difficulty {
    PEACEFUL, EASY, NORMAL, HARD
}
```

## 配置文件操作

### 1. 读取配置

```kotlin
object ConfigReader {
    
    @Config("example.yml")
    lateinit var config: Configuration
    
    fun readConfigurations() {
        // 基本类型读取（带默认值）
        val serverName = config.getString("server.name", "默认服务器")
        val maxPlayers = config.getInt("server.max-players", 100)
        val enablePvp = config.getBoolean("server.pvp", true)
        val motd = config.getString("server.motd", "欢迎来到服务器！")
        
        // 列表数据读取
        val adminList = config.getStringList("admins")
        val whiteList = config.getStringList("whitelist")
        val bannedItems = config.getList("banned-items")
        
        // 复杂对象读取
        val worldSettings = config.getConfigurationSection("world-settings")
        worldSettings?.let { settings ->
            val spawnProtection = settings.getInt("spawn-protection", 16)
            val allowNether = settings.getBoolean("allow-nether", true)
            val allowEnd = settings.getBoolean("allow-end", true)
        }
        
        // 直接获取原始值
        val rawValue = config.get("custom.setting")
        val specificValue = config.get("another.setting", "default")
        
        // 转换为 Map
        val configMap = config.toMap()
    }
}
```

### 2. 写入配置

```kotlin
object ConfigWriter {
    
    @Config("example.yml")
    lateinit var config: Configuration
    
    fun writeConfigurations() {
        // 设置基本类型
        config.set("server.name", "我的服务器")
        config.set("server.max-players", 200)
        config.set("server.pvp", false)
        config.set("server.difficulty", "NORMAL")
        
        // 使用索引操作符设置值
        config["server.motd"] = "新的服务器欢迎信息"
        config["server.port"] = 25565
        
        // 设置列表数据
        config.set("admins", listOf("admin1", "admin2", "admin3"))
        config.set("banned-items", listOf("minecraft:tnt", "minecraft:lava_bucket"))
        
        // 设置复杂对象
        config.set("database.host", "localhost")
        config.set("database.port", 3306)
        config.set("database.user", "root")
        config.set("database.password", "")
        
        // 创建子配置节
        val newSection = config.createSection("new-feature")
        newSection.set("enabled", true)
        newSection.set("max-uses", 10)
        
        // 清除特定路径
        config.set("old-setting", null)
        
        // 保存到文件
        config.saveToFile()
    }
}
```

### 3. 注释管理

```kotlin
object CommentManager {
    
    @Config("documented.yml")
    lateinit var config: Configuration
    
    fun manageComments() {
        // 设置单行注释
        config.setComment("server.name", "服务器显示名称")
        config.setComment("server.max-players", "最大玩家数量")
        
        // 获取注释
        val nameComment = config.getComment("server.name")
        val playerComment = config.getComment("server.max-players")
        
        // 设置多行注释
        config.setComments("database", listOf(
            "数据库配置区域",
            "请确保数据库服务器正在运行",
            "支持 MySQL 和 SQLite"
        ))
        
        // 获取多行注释
        val databaseComments = config.getComments("database")
        
        // 添加注释（不覆盖已有注释）
        config.addComments("server", listOf(
            "服务器基础配置",
            "修改后需要重启服务器"
        ))
        
        // 保存（包含注释）
        config.saveToFile()
    }
}
```

## 自动重载功能

### 1. 启用自动重载

```kotlin
object AutoReloadExample {
    
    @Config("dynamic.yml", autoReload = true)
    lateinit var dynamicConfig: Configuration
    
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        // 配置文件会自动监听文件变化并重载
        info("动态配置已加载，支持热重载")
    }
    
    fun getCurrentSettings() {
        // 总是获取最新的配置值
        val currentMessage = dynamicConfig.getString("welcome-message", "欢迎")
        val maxConnections = dynamicConfig.getInt("max-connections", 100)
        
        // 当外部修改文件时，这里会自动获取新值
        info("当前欢迎消息: $currentMessage")
        info("最大连接数: $maxConnections")
    }
    
    fun startDynamicTask() {
        // 使用动态配置的定时任务
        submitAsync(period = dynamicConfig.getLong("task-interval", 1200L)) {
            val message = dynamicConfig.getString("broadcast-message", "定时广播")
            broadcast(message)
        }
    }
}
```

### 2. 文件监听器

```kotlin
import taboolib.module.configuration.util.FileWatcher

object FileWatcherExample {
    
    fun setupCustomWatcher() {
        val configFile = File(getDataFolder(), "watched-config.yml")
        
        // 添加文件监听器
        FileWatcher.INSTANCE.addSimpleListener(configFile) { file ->
            info("配置文件已更新: ${file.name}")
            
            // 自定义重载逻辑
            reloadCustomSettings()
        }
    }
    
    private fun reloadCustomSettings() {
        // 重新加载自定义设置
        info("正在重新加载自定义设置...")
    }
}
```

## 手动配置管理

### 1. 手动加载配置文件

```kotlin
import taboolib.common.platform.function.getDataFolder
import taboolib.module.configuration.Configuration
import taboolib.module.configuration.Type
import java.io.File

object ManualConfigLoader {
    
    fun loadDifferentFormats() {
        val dataFolder = getDataFolder()
        
        // 加载 YAML 配置
        val yamlFile = File(dataFolder, "config.yml")
        val yamlConfig = Configuration.loadFromFile(yamlFile, Type.YAML)
        
        // 加载 JSON 配置
        val jsonFile = File(dataFolder, "config.json")
        val jsonConfig = Configuration.loadFromFile(jsonFile, Type.JSON)
        
        // 加载 TOML 配置
        val tomlFile = File(dataFolder, "config.toml")
        val tomlConfig = Configuration.loadFromFile(tomlFile, Type.TOML)
        
        // 加载 HOCON 配置
        val hoconFile = File(dataFolder, "config.conf")
        val hoconConfig = Configuration.loadFromFile(hoconFile, Type.HOCON)
        
        // 自动检测格式
        val autoConfig = Configuration.loadFromFile(yamlFile) // 根据扩展名自动检测
    }
    
    fun loadFromString() {
        val yamlString = """
            server:
              name: "测试服务器"
              port: 25565
            features:
              enable-pvp: true
        """.trimIndent()
        
        val config = Configuration.loadFromString(yamlString, Type.YAML)
        val serverName = config.getString("server.name")
    }
}
```

### 2. 创建配置文件

```kotlin
object ConfigCreator {
    
    fun createDefaultConfig() {
        // 创建空配置
        val config = Configuration.empty(Type.YAML)
        
        // 设置默认值
        config.set("version", "1.0.0")
        config.set("created", System.currentTimeMillis())
        
        // 服务器配置
        config.set("server.name", "我的 Minecraft 服务器")
        config.set("server.max-players", 100)
        config.set("server.difficulty", "NORMAL")
        config.set("server.pvp", true)
        
        // 数据库配置
        config.set("database.enable", false)
        config.set("database.host", "localhost")
        config.set("database.port", 3306)
        config.set("database.name", "minecraft")
        config.set("database.user", "root")
        config.set("database.password", "")
        
        // 功能配置
        config.set("features.auto-save", true)
        config.set("features.backup-interval", 3600)
        config.set("features.max-players-per-ip", 3)
        
        // 添加注释
        config.setComment("version", "配置文件版本号")
        config.setComments("server", listOf(
            "服务器基础配置",
            "修改后需要重启服务器生效"
        ))
        config.setComments("database", listOf(
            "数据库连接配置",
            "enable: false 时使用 SQLite"
        ))
        
        // 保存到文件
        val configFile = File(getDataFolder(), "config.yml")
        config.saveToFile(configFile)
        
        info("已创建默认配置文件")
    }
}
```

## 高级特性

### 1. 配置验证和升级

```kotlin
object ConfigValidator {
    
    @Config("versioned.yml")
    lateinit var config: Configuration
    
    @Awake(LifeCycle.ENABLE)
    fun validateAndUpgrade() {
        val currentVersion = config.getString("version", "1.0.0")
        val requiredVersion = "2.0.0"
        
        if (currentVersion != requiredVersion) {
            info("配置文件版本过旧 ($currentVersion)，正在升级到 $requiredVersion")
            upgradeConfig(currentVersion, requiredVersion)
        }
        
        validateRequiredSettings()
    }
    
    private fun upgradeConfig(from: String, to: String) {
        when (from) {
            "1.0.0" -> {
                // 1.0.0 -> 2.0.0 升级
                config.set("version", "2.0.0")
                
                // 添加新配置项
                if (!config.contains("new-features")) {
                    config.set("new-features.enabled", true)
                    config.set("new-features.max-level", 10)
                    config.setComment("new-features", "2.0.0 版本新增功能")
                }
                
                // 迁移旧配置
                val oldSetting = config.get("old-setting")
                if (oldSetting != null) {
                    config.set("new-setting", oldSetting)
                    config.set("old-setting", null) // 删除旧配置
                }
                
                config.saveToFile()
                info("配置文件已升级到版本 $to")
            }
        }
    }
    
    private fun validateRequiredSettings() {
        var needSave = false
        
        // 检查必需配置
        val requiredSettings = mapOf(
            "server.name" to "默认服务器",
            "server.max-players" to 100,
            "features.auto-save" to true
        )
        
        requiredSettings.forEach { (path, defaultValue) ->
            if (!config.contains(path)) {
                config.set(path, defaultValue)
                needSave = true
                warning("添加缺失的配置项: $path = $defaultValue")
            }
        }
        
        if (needSave) {
            config.saveToFile()
            info("已补充缺失的配置项")
        }
    }
}
```

### 2. 格式转换

```kotlin
object FormatConverter {
    
    fun convertFormats() {
        // 加载 YAML 配置
        val yamlFile = File(getDataFolder(), "config.yml")
        val config = Configuration.loadFromFile(yamlFile, Type.YAML)
        
        // 转换为不同格式并保存
        config.saveToFile(File(getDataFolder(), "config.json"), Type.JSON)
        config.saveToFile(File(getDataFolder(), "config.toml"), Type.TOML)
        config.saveToFile(File(getDataFolder(), "config.conf"), Type.HOCON)
        
        info("配置文件已转换为多种格式")
    }
}
```

### 3. 配置模板系统

```kotlin
object ConfigTemplate {
    
    fun createFromTemplate(templateName: String) {
        val templates = mapOf(
            "basic" to ::createBasicTemplate,
            "advanced" to ::createAdvancedTemplate,
            "database" to ::createDatabaseTemplate
        )
        
        val creator = templates[templateName]
        if (creator != null) {
            creator()
            info("已创建 $templateName 配置模板")
        } else {
            warning("未找到配置模板: $templateName")
        }
    }
    
    private fun createBasicTemplate() {
        val config = Configuration.empty()
        
        config.set("server.name", "我的服务器")
        config.set("server.max-players", 20)
        config.setComment("server", "基础服务器配置")
        
        config.saveToFile(File(getDataFolder(), "basic-config.yml"))
    }
    
    private fun createAdvancedTemplate() {
        val config = Configuration.empty()
        
        // 高级配置模板
        config.set("server.name", "高级服务器")
        config.set("server.max-players", 100)
        config.set("performance.chunk-loading-threads", 4)
        config.set("performance.entity-tracking-range", 64)
        
        config.setComments("performance", listOf(
            "性能优化配置",
            "请根据服务器硬件调整"
        ))
        
        config.saveToFile(File(getDataFolder(), "advanced-config.yml"))
    }
    
    private fun createDatabaseTemplate() {
        val config = Configuration.empty()
        
        config.set("database.type", "mysql")
        config.set("database.host", "localhost")
        config.set("database.port", 3306)
        config.set("database.name", "minecraft")
        config.set("database.user", "root")
        config.set("database.password", "")
        config.set("database.pool.max-size", 10)
        config.set("database.pool.min-idle", 2)
        
        config.setComment("database", "数据库连接配置")
        
        config.saveToFile(File(getDataFolder(), "database-config.yml"))
    }
}
```

## 配置节点管理

### @ConfigNode 注解

根据最新的 TabooLib 源码，`@ConfigNode` 注解确实存在，用于标记配置文件中特定路径或值的细粒度管理。它与 `@Config` 注解配合使用，允许在配置文件重载时进行更精确的节点更新。

```kotlin
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object ConfigNodeExample {
    
    @Config("app.yml", autoReload = true)
    lateinit var config: Configuration
    
    // 使用 @ConfigNode 标记特定配置节点
    @ConfigNode("server.name")
    var serverName: String = "默认服务器"
    
    @ConfigNode("server.max-players") 
    var maxPlayers: Int = 100
    
    @ConfigNode("features.enabled")
    var featuresEnabled: Boolean = true
    
    @ConfigNode("database.connection-timeout")
    var connectionTimeout: Long = 30000L
    
    // 复杂类型的配置节点
    @ConfigNode("admin-list")
    var adminList: List<String> = listOf()
    
    @ConfigNode("server-settings")
    var serverSettings: Map<String, Any> = mapOf()
}
```

### @ConfigNode 工作机制

`@ConfigNode` 注解的工作流程如下：

1. **节点注册**: 在配置文件加载时，`ConfigNodeLoader` 会扫描所有被 `@ConfigNode` 注解标记的字段，并将其注册到 `ConfigNodeFile` 中
2. **值同步**: 当配置文件重载时，系统会自动更新这些标记的字段值
3. **细粒度控制**: 相比 `@Config` 注解加载整个配置文件，`@ConfigNode` 允许更精确地控制特定配置项

### ConfigNodeTransfer 转换机制

除了 `@ConfigNode` 注解外，TabooLib 还提供了 `ConfigNodeTransfer` 类用于配置值的转换和缓存：

```kotlin
import taboolib.module.configuration.conversion

object NodeTransferExample {
    
    @Config("transform.yml")
    lateinit var config: Configuration
    
    // 使用 conversion 进行值转换
    val serverPort by conversion<String, Int> {
        config.getString("server.port-string", "25565").toInt()
    }
    
    val enabledFeatures by conversion<List<String>, Set<String>> {
        config.getStringList("features").toSet()
    }
    
    val connectionTimeout by conversion<String, Long> {
        val timeoutStr = config.getString("timeout", "30s")
        parseTimeToMillis(timeoutStr)
    }
    
    private fun parseTimeToMillis(time: String): Long {
        return when {
            time.endsWith("s") -> time.removeSuffix("s").toLong() * 1000
            time.endsWith("m") -> time.removeSuffix("m").toLong() * 60000
            time.endsWith("h") -> time.removeSuffix("h").toLong() * 3600000
            else -> time.toLong()
        }
    }
}
```

## 完整示例

```kotlin
package online.bingzi.bilibili.bilibilivideo

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

object BilibiliVideoConfig {
    
    @Config("config.yml", autoReload = true)
    lateinit var config: Configuration
    
    @Config("database.yml")
    lateinit var databaseConfig: Configuration
    
    @Config("messages.yml", autoReload = true)
    lateinit var messages: Configuration
    
    @Awake(LifeCycle.ENABLE)
    fun initialize() {
        // 验证和升级配置
        validateAndUpgradeConfigs()
        
        // 加载配置设置
        loadSettings()
        
        info("配置系统初始化完成")
    }
    
    private fun validateAndUpgradeConfigs() {
        // 检查主配置版本
        val version = config.getString("version", "1.0.0")
        if (version == "1.0.0") {
            upgradeMainConfig()
        }
        
        // 确保必需配置存在
        ensureRequiredConfigs()
    }
    
    private fun upgradeMainConfig() {
        info("正在升级主配置文件...")
        
        config.set("version", "2.0.0")
        
        // 添加新功能配置
        if (!config.contains("features")) {
            config.set("features.max-bilibili-accounts", 3)
            config.set("features.cookie-refresh-days", 30)
            config.set("features.auto-clean-expired", true)
            config.setComments("features", listOf(
                "插件功能配置",
                "max-bilibili-accounts: 每个玩家最多绑定的B站账户数",
                "cookie-refresh-days: Cookie自动刷新间隔（天）",
                "auto-clean-expired: 是否自动清理过期数据"
            ))
        }
        
        config.saveToFile()
        info("主配置文件已升级到版本 2.0.0")
    }
    
    private fun ensureRequiredConfigs() {
        var needSave = false
        
        // 确保安全配置存在
        if (!config.contains("security.encryption-key")) {
            config.set("security.encryption-key", "")
            config.setComments("security.encryption-key", listOf(
                "自动生成的加密密钥，请勿手动修改！",
                "如果丢失此密钥，所有加密数据将无法解密",
                "强烈建议备份此配置文件到安全位置"
            ))
            needSave = true
        }
        
        // 确保数据库配置存在
        if (!databaseConfig.contains("enable")) {
            databaseConfig.set("enable", false)
            databaseConfig.set("host", "localhost")
            databaseConfig.set("port", 3306)
            databaseConfig.set("database", "bilibili_video")
            databaseConfig.set("user", "root")
            databaseConfig.set("password", "")
            databaseConfig.set("table_prefix", "bv_")
            
            databaseConfig.setComment("enable", "true 使用 MySQL，false 使用 SQLite")
            databaseConfig.setComments("host", listOf("数据库服务器地址"))
            
            databaseConfig.saveToFile()
            needSave = false
        }
        
        if (needSave) {
            config.saveToFile()
        }
    }
    
    private fun loadSettings() {
        // 加载主配置设置
        val maxAccounts = config.getInt("features.max-bilibili-accounts", 3)
        val refreshDays = config.getInt("features.cookie-refresh-days", 30)
        val autoClean = config.getBoolean("features.auto-clean-expired", true)
        
        info("功能配置 - 最大账户数: $maxAccounts, 刷新间隔: ${refreshDays}天, 自动清理: $autoClean")
        
        // 加载数据库配置
        val dbEnabled = databaseConfig.getBoolean("enable", false)
        if (dbEnabled) {
            val host = databaseConfig.getString("host", "localhost")
            val port = databaseConfig.getInt("port", 3306)
            val database = databaseConfig.getString("database", "bilibili_video")
            info("数据库配置 - 使用 MySQL: $host:$port/$database")
        } else {
            info("数据库配置 - 使用 SQLite")
        }
    }
    
    // 获取本地化消息
    fun getMessage(key: String, vararg args: Any): String {
        val message = messages.getString(key, key)
        return if (args.isNotEmpty()) {
            String.format(message, *args)
        } else {
            message
        }
    }
    
    // 获取配置值的便捷方法
    fun getMaxBilibiliAccounts(): Int = config.getInt("features.max-bilibili-accounts", 3)
    fun getCookieRefreshDays(): Int = config.getInt("features.cookie-refresh-days", 30)
    fun isAutoCleanEnabled(): Boolean = config.getBoolean("features.auto-clean-expired", true)
    fun getEncryptionKey(): String = config.getString("security.encryption-key", "")
    
    // 设置配置值的便捷方法
    fun setEncryptionKey(key: String) {
        config.set("security.encryption-key", key)
        config.saveToFile()
    }
}
```

## 重要说明

**关于 @ConfigNode**：
- TabooLib 中**存在 @ConfigNode 注解**，用于标记配置文件中的特定节点
- `@ConfigNode` 注解与 `@Config` 注解配合使用，实现细粒度的配置管理
- `@ConfigNode` 允许在配置文件重载时精确更新特定的配置字段
- 使用 `@Config` 注解可以自动加载整个配置文件
- 使用 `ConfigNodeTransfer` 和 `conversion` 函数可以实现配置值的转换

Configuration 模块为 TabooLib 插件提供了完整的配置文件管理解决方案，通过 `@Config` 注解和丰富的 API，可以轻松实现复杂的配置管理需求。