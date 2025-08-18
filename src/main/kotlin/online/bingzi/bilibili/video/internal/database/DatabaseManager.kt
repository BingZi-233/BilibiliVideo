package online.bingzi.bilibili.video.internal.database

import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.support.ConnectionSource
import online.bingzi.bilibili.video.api.event.database.DatabaseConnectionEvent
import online.bingzi.bilibili.video.api.event.database.DatabaseInitializeEvent
import online.bingzi.bilibili.video.internal.config.ConfigManager
import online.bingzi.bilibili.video.internal.database.entity.*
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.io.File

/**
 * OrmLite数据库连接管理器
 * 负责数据库连接的创建、配置和管理
 */
object DatabaseManager {

    /**
     * 数据库连接源
     */
    private var connectionSource: ConnectionSource? = null

    /**
     * 数据库配置
     */
    private var databaseConfig: DatabaseConfig? = null

    /**
     * 支持的实体类列表
     */
    val entityClasses = listOf(
        Player::class.java,
        QQBinding::class.java,
        BilibiliBinding::class.java,
        BilibiliCookie::class.java,
        UploaderVideo::class.java,
        UploaderConfig::class.java,
        VideoRewardRecord::class.java,
        RewardConfig::class.java,
        PlayerRewardStats::class.java
    )

    /**
     * 初始化数据库连接
     *
     * @return 是否初始化成功
     */
    fun initialize(): Boolean {
        return try {
            console().sendInfo("databaseInitializing")

            val config = loadDatabaseConfig()
            databaseConfig = config
            
            // 触发初始化事件
            val databaseType = when(config) {
                is DatabaseConfig.MySQL -> "MySQL"
                is DatabaseConfig.SQLite -> "SQLite"
            }
            val initEvent = DatabaseInitializeEvent(
                databaseType = databaseType,
                success = true
            )
            initEvent.call()
            
            connectionSource = createConnectionSource(config)

            console().sendInfo("databaseInitialized")
            true
        } catch (e: Exception) {
            console().sendWarn("databaseInitFailed", e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 获取数据库连接源
     *
     * @return ConnectionSource 连接源，如果未初始化则抛出异常
     */
    fun getConnectionSource(): ConnectionSource {
        return connectionSource ?: throw IllegalStateException("数据库连接未初始化，请先调用initialize()")
    }

    /**
     * 获取数据库配置
     *
     * @return DatabaseConfig 数据库配置，如果未初始化则抛出异常
     */
    fun getDatabaseConfig(): DatabaseConfig {
        return databaseConfig ?: throw IllegalStateException("数据库配置未加载，请先调用initialize()")
    }

    /**
     * 关闭数据库连接
     */
    fun close() {
        try {
            connectionSource?.close()
            connectionSource = null
            databaseConfig = null
        } catch (e: Exception) {
            console().sendWarn("databaseConnectFailed", e.message ?: "Unknown error")
        }
    }

    /**
     * 加载数据库配置
     */
    private fun loadDatabaseConfig(): DatabaseConfig {
        val config = ConfigManager.databaseConfig
        val connectionSection = config.getConfigurationSection("connection")

        val tablePrefix = ConfigManager.getTablePrefix()

        return if (connectionSection?.getBoolean("enable") == true) {
            // MySQL配置
            val mysqlSection = connectionSection.getConfigurationSection("mysql")!!
            DatabaseConfig.MySQL(
                host = mysqlSection.getString("host", "localhost")!!,
                port = mysqlSection.getInt("port", 3306),
                database = mysqlSection.getString("database", "bilibili_video")!!,
                username = mysqlSection.getString("username", "root")!!,
                password = mysqlSection.getString("password", "root")!!,
                tablePrefix = tablePrefix,
                parameters = loadMysqlParameters(mysqlSection),
                poolConfig = loadPoolConfig(mysqlSection)
            )
        } else {
            // SQLite配置
            val sqliteSection = connectionSection?.getConfigurationSection("sqlite")
            val sqliteFilePath = sqliteSection?.getString("file-path") ?: "database/bilibili.db"
            val sqliteFile = File(getDataFolder(), sqliteFilePath)

            // 确保父目录存在
            sqliteFile.parentFile?.mkdirs()

            DatabaseConfig.SQLite(
                filePath = sqliteFile.absolutePath,
                tablePrefix = tablePrefix,
                parameters = loadSqliteParameters(sqliteSection)
            )
        }
    }

    /**
     * 加载MySQL连接参数
     */
    private fun loadMysqlParameters(mysqlSection: ConfigurationSection): Map<String, String> {
        val parametersSection = mysqlSection.getConfigurationSection("parameters")
        val params = mutableMapOf<String, String>()

        parametersSection?.getKeys(false)?.forEach { key ->
            params[key] = parametersSection.getString(key, "")!!
        }

        return params.ifEmpty {
            mapOf(
                "useSSL" to "false",
                "allowPublicKeyRetrieval" to "true",
                "serverTimezone" to "UTC",
                "characterEncoding" to "utf8",
                "autoReconnect" to "true",
                "failOverReadOnly" to "false",
                "maxReconnects" to "3"
            )
        }
    }

    /**
     * 加载SQLite连接参数
     */
    private fun loadSqliteParameters(sqliteSection: ConfigurationSection?): Map<String, String> {
        val parametersSection = sqliteSection?.getConfigurationSection("parameters")
        val params = mutableMapOf<String, String>()

        parametersSection?.getKeys(false)?.forEach { key ->
            params[key] = parametersSection.getString(key, "")!!
        }

        return params.ifEmpty {
            mapOf(
                "journal_mode" to "WAL",
                "synchronous" to "NORMAL",
                "cache_size" to "10000",
                "temp_store" to "MEMORY",
                "foreign_keys" to "true"
            )
        }
    }

    /**
     * 加载连接池配置
     */
    private fun loadPoolConfig(mysqlSection: ConfigurationSection): PoolConfig {
        val poolSection = mysqlSection.getConfigurationSection("pool")
        return PoolConfig(
            minimumIdle = poolSection?.getInt("minimum-idle") ?: 2,
            maximumPoolSize = poolSection?.getInt("maximum-pool-size") ?: 10,
            connectionTimeout = poolSection?.getLong("connection-timeout") ?: 30000L,
            idleTimeout = poolSection?.getLong("idle-timeout") ?: 600000L,
            maxLifetime = poolSection?.getLong("max-lifetime") ?: 1800000L,
            leakDetectionThreshold = poolSection?.getLong("leak-detection-threshold") ?: 60000L
        )
    }

    /**
     * 创建数据库连接源
     */
    private fun createConnectionSource(config: DatabaseConfig): ConnectionSource {
        return when (config) {
            is DatabaseConfig.MySQL -> {
                val paramStr = config.parameters.entries.joinToString("&") { "${it.key}=${it.value}" }
                val url = "jdbc:mysql://${config.host}:${config.port}/${config.database}?$paramStr"
                console().sendInfo("databaseConnected", "MySQL (${config.host}:${config.port}/${config.database})")
                val source = JdbcConnectionSource(url, config.username, config.password)
                
                // 触发连接事件
                val connectionEvent = DatabaseConnectionEvent(
                    connected = true,
                    databaseInfo = "MySQL (${config.host}:${config.port}/${config.database})"
                )
                connectionEvent.call()
                
                source
            }

            is DatabaseConfig.SQLite -> {
                val paramStr = config.parameters.entries.joinToString("&") { "${it.key}=${it.value}" }
                val url = "jdbc:sqlite:${config.filePath}?$paramStr"
                console().sendInfo("databaseConnected", "SQLite (${config.filePath})")
                val source = JdbcConnectionSource(url)
                
                // 触发连接事件
                val connectionEvent = DatabaseConnectionEvent(
                    connected = true,
                    databaseInfo = "SQLite (${config.filePath})"
                )
                connectionEvent.call()
                
                source
            }
        }
    }

    /**
     * 数据库配置密封类
     */
    sealed class DatabaseConfig {
        abstract val tablePrefix: String

        /**
         * MySQL数据库配置
         */
        data class MySQL(
            val host: String,
            val port: Int,
            val database: String,
            val username: String,
            val password: String,
            override val tablePrefix: String,
            val parameters: Map<String, String>,
            val poolConfig: PoolConfig
        ) : DatabaseConfig()

        /**
         * SQLite数据库配置
         */
        data class SQLite(
            val filePath: String,
            override val tablePrefix: String,
            val parameters: Map<String, String>
        ) : DatabaseConfig()
    }

    /**
     * 连接池配置数据类
     */
    data class PoolConfig(
        val minimumIdle: Int,
        val maximumPoolSize: Int,
        val connectionTimeout: Long,
        val idleTimeout: Long,
        val maxLifetime: Long,
        val leakDetectionThreshold: Long
    )
}