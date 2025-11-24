package online.bingzi.bilibili.video.internal.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import online.bingzi.bilibili.video.internal.config.DatabaseConfig
import online.bingzi.bilibili.video.internal.config.DatabaseConfigManager
import online.bingzi.bilibili.video.internal.config.DatabaseType
import org.ktorm.database.Database
import taboolib.common.platform.function.getDataFolder
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import java.io.File

/**
 * 数据库工厂，负责根据配置初始化 Hikari 数据源与 Ktorm Database 实例。
 */
internal object DatabaseFactory {

    @Volatile
    private var dataSource: HikariDataSource? = null

    @Volatile
    private var database: Database? = null

    /**
     * 使用配置文件初始化数据库。
     */
    fun initFromConfig() {
        val config = DatabaseConfigManager.load()
        init(config)
    }

    /**
     * 使用给定配置初始化数据库。
     */
    fun init(config: DatabaseConfig) {
        if (!config.enabled) {
            info("[Database] 已禁用，跳过初始化。")
            return
        }
        if (database != null) {
            // 已初始化，避免重复创建连接池
            return
        }

        // 覆盖表前缀，需在首次访问实体前完成
        DATABASE_TABLE_PREFIX = config.options.tablePrefix

        try {
            val jdbcUrlValue = when (config.type) {
                DatabaseType.SQLITE -> buildSqliteJdbcUrl(config)
                DatabaseType.MYSQL -> buildMysqlJdbcUrl(config)
            }

            val hikariConfig = HikariConfig().apply {
                poolName = config.hikari.poolName
                jdbcUrl = jdbcUrlValue
                maximumPoolSize = config.hikari.maximumPoolSize
                minimumIdle = config.hikari.minimumIdle
                connectionTimeout = config.hikari.connectionTimeout
                idleTimeout = config.hikari.idleTimeout
                maxLifetime = config.hikari.maxLifetime
                isAutoCommit = config.hikari.autoCommit
                isReadOnly = config.hikari.readOnly

                when (config.type) {
                    DatabaseType.SQLITE -> {
                        driverClassName = "org.sqlite.JDBC"
                    }

                    DatabaseType.MYSQL -> {
                        driverClassName = "com.mysql.cj.jdbc.Driver"
                        username = config.mysql.username
                        password = config.mysql.password
                    }
                }
            }

            val ds = HikariDataSource(hikariConfig)
            val db = Database.connect(ds)

            try {
                DatabaseSchemaInitializer.ensureSchema(db, config)
            } catch (t: Throwable) {
                ds.close()
                throw t
            }

            dataSource = ds
            database = db

            info("[Database] 初始化完成,类型: ${config.type}, URL: $jdbcUrlValue")
        } catch (t: Throwable) {
            warning("[Database] 初始化失败: ${t.message}", t)
        }
    }

    private fun buildSqliteJdbcUrl(config: DatabaseConfig): String {
        val dataFolder = getDataFolder()
        val fileName = config.sqlite.file.ifBlank { "bilibili_video.db" }
        val dbFile = File(dataFolder, fileName)
        val path = dbFile.absolutePath.replace("\\", "/")
        return "jdbc:sqlite:$path"
    }

    private fun buildMysqlJdbcUrl(config: DatabaseConfig): String {
        val host = config.mysql.host
        val port = config.mysql.port
        val databaseName = config.mysql.database
        val params = config.mysql.params
        val base = "jdbc:mysql://$host:$port/$databaseName"
        return if (params.isBlank()) base else "$base?$params"
    }

    /**
     * 获取 Ktorm Database 实例。
     *
     * 如果尚未初始化，将抛出异常，调用方应在插件启动流程中显式初始化。
     */
    fun database(): Database {
        return database ?: error("Database is not initialized, please call DatabaseFactory.initFromConfig() first.")
    }

    /**
     * 关闭连接池。
     */
    fun shutdown() {
        database = null
        dataSource?.close()
        dataSource = null
    }
}
