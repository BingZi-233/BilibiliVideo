package online.bingzi.bilibili.video.internal.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

/**
 * 数据库类型。
 */
enum class DatabaseType {
    SQLITE,
    MYSQL
}

data class SqliteSection(
    val file: String
)

data class MysqlSection(
    val host: String,
    val port: Int,
    val database: String,
    val username: String,
    val password: String,
    val params: String
)

data class HikariSection(
    val poolName: String,
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeout: Long,
    val idleTimeout: Long,
    val maxLifetime: Long,
    val autoCommit: Boolean,
    val readOnly: Boolean
)

data class DatabaseOptionsSection(
    val tablePrefix: String,
    val showSql: Boolean,
    val slowSqlThresholdMs: Long
)

/**
 * 从配置文件解析后的数据库整体配置。
 */
data class DatabaseConfig(
    val enabled: Boolean,
    val type: DatabaseType,
    val sqlite: SqliteSection,
    val mysql: MysqlSection,
    val hikari: HikariSection,
    val options: DatabaseOptionsSection
)

/**
 * 数据库配置管理器，负责从 `database.yml` 中解析配置。
 */
object DatabaseConfigManager {

    @Config("database.yml")
    lateinit var file: ConfigFile
        private set

    fun load(): DatabaseConfig {
        val enabled = file.getBoolean("database.enabled", true)
        val type = when (file.getString("database.type")?.lowercase()) {
            "mysql" -> DatabaseType.MYSQL
            else -> DatabaseType.SQLITE
        }

        val sqlite = SqliteSection(
            file = file.getString("database.sqlite.file") ?: "bilibili_video.db"
        )

        val mysql = MysqlSection(
            host = file.getString("database.mysql.host") ?: "127.0.0.1",
            port = file.getInt("database.mysql.port"),
            database = file.getString("database.mysql.database") ?: "bilibili_video",
            username = file.getString("database.mysql.username") ?: "root",
            password = file.getString("database.mysql.password") ?: "password",
            params = file.getString("database.mysql.params")
                ?: "useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
        )

        val hikari = HikariSection(
            poolName = file.getString("database.hikari.pool-name") ?: "BilibiliVideoPool",
            maximumPoolSize = file.getInt("database.hikari.maximum-pool-size").takeIf { it > 0 } ?: 10,
            minimumIdle = file.getInt("database.hikari.minimum-idle").takeIf { it >= 0 } ?: 2,
            connectionTimeout = file.getLong("database.hikari.connection-timeout").takeIf { it > 0 } ?: 30_000L,
            idleTimeout = file.getLong("database.hikari.idle-timeout").takeIf { it >= 0 } ?: 600_000L,
            maxLifetime = file.getLong("database.hikari.max-lifetime").takeIf { it > 0 } ?: 1_800_000L,
            autoCommit = file.getBoolean("database.hikari.auto-commit", true),
            readOnly = file.getBoolean("database.hikari.read-only", false)
        )

        val options = DatabaseOptionsSection(
            tablePrefix = file.getString("database.options.table-prefix") ?: "bv_",
            showSql = file.getBoolean("database.options.show-sql", false),
            slowSqlThresholdMs = file.getLong("database.options.slow-sql-threshold-ms").takeIf { it > 0 } ?: 500L
        )

        return DatabaseConfig(
            enabled = enabled,
            type = type,
            sqlite = sqlite,
            mysql = mysql,
            hikari = hikari,
            options = options
        )
    }
}

