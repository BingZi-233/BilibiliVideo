package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.config.DatabaseConfig
import online.bingzi.bilibili.video.internal.config.DatabaseType
import org.ktorm.database.Database
import taboolib.common.platform.function.info
import java.sql.Connection

/**
 * 负责在首次连接数据库时，确保所有业务表已经存在。
 */
internal object DatabaseSchemaInitializer {

    fun ensureSchema(database: Database, config: DatabaseConfig) {
        val statements = when (config.type) {
            DatabaseType.SQLITE -> buildSqliteStatements(config.options.tablePrefix)
            DatabaseType.MYSQL -> buildMysqlStatements(config.options.tablePrefix)
        }

        database.useConnection { connection ->
            executeStatements(connection, statements, config.options.showSql)
        }

        // 执行迁移语句
        val migrations = when (config.type) {
            DatabaseType.SQLITE -> buildSqliteMigrations(config.options.tablePrefix)
            DatabaseType.MYSQL -> buildMysqlMigrations(config.options.tablePrefix)
        }
        database.useConnection { connection ->
            executeMigrations(connection, migrations, config.options.showSql)
        }

        info("[Database] Schema 校验完成（执行 ${statements.size} 条语句，${migrations.size} 条迁移）。")
    }

    private fun executeStatements(connection: Connection, statements: List<String>, showSql: Boolean) {
        statements.forEach { sql ->
            connection.createStatement().use { statement ->
                if (showSql) {
                    val oneLineSql = sql.lines().joinToString(" ") { it.trim() }
                    info("[Database][Schema] $oneLineSql")
                }
                statement.execute(sql)
            }
        }
    }

    /**
     * 执行迁移语句，忽略已存在列等错误。
     */
    private fun executeMigrations(connection: Connection, migrations: List<String>, showSql: Boolean) {
        migrations.forEach { sql ->
            try {
                connection.createStatement().use { statement ->
                    if (showSql) {
                        val oneLineSql = sql.lines().joinToString(" ") { it.trim() }
                        info("[Database][Migration] $oneLineSql")
                    }
                    statement.execute(sql)
                }
            } catch (e: Exception) {
                // 忽略列/索引已存在等错误（SQLite: duplicate column name, MySQL: Duplicate column name / Duplicate key name）
                val msg = e.message?.lowercase() ?: ""
                if (!msg.contains("duplicate column") && !msg.contains("duplicate column name") && !msg.contains("duplicate key name")) {
                    throw e
                }
            }
        }
    }

    private fun buildSqliteStatements(prefix: String): List<String> {
        val sanitizedPrefix = prefix.ifBlank { "" }
        val table = { name: String -> sanitizedPrefix + name }
        val statements = listOf(
            // 玩家绑定关系表
            """
                CREATE TABLE IF NOT EXISTS ${table("bound_account")} (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL UNIQUE,
                    player_name TEXT NOT NULL,
                    bilibili_mid INTEGER NOT NULL UNIQUE,
                    bilibili_name TEXT NOT NULL,
                    status INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                );
            """.trimIndent(),
            // 登录凭证表
            """
                CREATE TABLE IF NOT EXISTS ${table("credential")} (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    label TEXT NOT NULL UNIQUE,
                    sessdata TEXT NOT NULL,
                    bili_jct TEXT NOT NULL,
                    bilibili_mid INTEGER NULL,
                    buvid3 TEXT NULL,
                    access_key TEXT NULL,
                    refresh_token TEXT NULL,
                    status INTEGER NOT NULL DEFAULT 1,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    expired_at INTEGER NULL,
                    last_used_at INTEGER NULL
                );
            """.trimIndent(),
            // 三连状态缓存表
            """
                CREATE TABLE IF NOT EXISTS ${table("triple_status")} (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    bilibili_mid INTEGER NOT NULL,
                    target_key TEXT NOT NULL,
                    target_bvid TEXT NOT NULL,
                    last_status INTEGER NOT NULL DEFAULT 0,
                    last_triple_time INTEGER NULL,
                    last_check_time INTEGER NULL,
                    last_error_code INTEGER NULL,
                    last_error_message TEXT NULL,
                    UNIQUE(player_uuid, target_key)
                );
            """.trimIndent(),
            // 奖励记录表
            """
                CREATE TABLE IF NOT EXISTS ${table("reward_record")} (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT NOT NULL,
                    player_name TEXT NOT NULL,
                    target_key TEXT NOT NULL,
                    reward_key TEXT NOT NULL,
                    status INTEGER NOT NULL DEFAULT 0,
                    issued_at INTEGER NOT NULL,
                    context TEXT NULL,
                    fail_reason TEXT NULL
                );
            """.trimIndent(),
            // 帮助凭证按 mid 查询的索引
            """
                CREATE INDEX IF NOT EXISTS ${table("credential_mid_idx")} ON ${table("credential")} (bilibili_mid);
            """.trimIndent(),
            // 奖励记录查询索引
            """
                CREATE INDEX IF NOT EXISTS ${table("reward_player_target_idx")} ON ${table("reward_record")} (player_uuid, target_key);
            """.trimIndent()
        )
        return statements
    }

    private fun buildMysqlStatements(prefix: String): List<String> {
        val sanitizedPrefix = prefix.ifBlank { "" }
        val table = { name: String -> "`${sanitizedPrefix + name}`" }
        val idx = { name: String -> "`${sanitizedPrefix + name}`" }
        val tableOptions = "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        return listOf(
            // 玩家绑定关系表
            """
                CREATE TABLE IF NOT EXISTS ${table("bound_account")} (
                    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `player_name` VARCHAR(64) NOT NULL,
                    `bilibili_mid` BIGINT UNSIGNED NOT NULL,
                    `bilibili_name` VARCHAR(64) NOT NULL,
                    `status` INT NOT NULL DEFAULT 1,
                    `created_at` BIGINT NOT NULL,
                    `updated_at` BIGINT NOT NULL,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY ${idx("bound_player_uuid_uq")} (`player_uuid`),
                    UNIQUE KEY ${idx("bound_mid_uq")} (`bilibili_mid`)
                ) $tableOptions;
            """.trimIndent(),
            // 登录凭证表
            """
                CREATE TABLE IF NOT EXISTS ${table("credential")} (
                    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `label` VARCHAR(64) NOT NULL,
                    `sessdata` TEXT NOT NULL,
                    `bili_jct` VARCHAR(64) NOT NULL,
                    `bilibili_mid` BIGINT UNSIGNED NULL,
                    `buvid3` VARCHAR(64) NULL,
                    `access_key` VARCHAR(128) NULL,
                    `refresh_token` VARCHAR(256) NULL,
                    `status` INT NOT NULL DEFAULT 1,
                    `created_at` BIGINT NOT NULL,
                    `updated_at` BIGINT NOT NULL,
                    `expired_at` BIGINT NULL,
                    `last_used_at` BIGINT NULL,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY ${idx("credential_label_uq")} (`label`),
                    KEY ${idx("credential_mid_idx")} (`bilibili_mid`)
                ) $tableOptions;
            """.trimIndent(),
            // 三连状态缓存表
            """
                CREATE TABLE IF NOT EXISTS ${table("triple_status")} (
                    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `bilibili_mid` BIGINT UNSIGNED NOT NULL,
                    `target_key` VARCHAR(128) NOT NULL,
                    `target_bvid` VARCHAR(32) NOT NULL,
                    `last_status` INT NOT NULL DEFAULT 0,
                    `last_triple_time` BIGINT NULL,
                    `last_check_time` BIGINT NULL,
                    `last_error_code` INT NULL,
                    `last_error_message` VARCHAR(255) NULL,
                    PRIMARY KEY (`id`),
                    UNIQUE KEY ${idx("triple_player_target_uq")} (`player_uuid`, `target_key`)
                ) $tableOptions;
            """.trimIndent(),
            // 奖励记录表
            """
                CREATE TABLE IF NOT EXISTS ${table("reward_record")} (
                    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                    `player_uuid` VARCHAR(36) NOT NULL,
                    `player_name` VARCHAR(64) NOT NULL,
                    `target_key` VARCHAR(128) NOT NULL,
                    `reward_key` VARCHAR(64) NOT NULL,
                    `status` INT NOT NULL DEFAULT 0,
                    `issued_at` BIGINT NOT NULL,
                    `context` TEXT NULL,
                    `fail_reason` VARCHAR(255) NULL,
                    PRIMARY KEY (`id`),
                    KEY ${idx("reward_player_target_idx")} (`player_uuid`, `target_key`)
                ) $tableOptions;
            """.trimIndent()
        )
    }

    /**
     * SQLite 迁移语句：为现有表添加新列。
     */
    private fun buildSqliteMigrations(prefix: String): List<String> {
        val sanitizedPrefix = prefix.ifBlank { "" }
        val table = { name: String -> sanitizedPrefix + name }
        return listOf(
            // 为 reward_record 表添加 bilibili_mid 列
            "ALTER TABLE ${table("reward_record")} ADD COLUMN bilibili_mid INTEGER NULL;",
            // 为 reward_record 表添加按 bilibili_mid + target_key 查询的索引
            "CREATE INDEX IF NOT EXISTS ${table("reward_mid_target_idx")} ON ${table("reward_record")} (bilibili_mid, target_key);"
        )
    }

    /**
     * MySQL 迁移语句：为现有表添加新列。
     */
    private fun buildMysqlMigrations(prefix: String): List<String> {
        val sanitizedPrefix = prefix.ifBlank { "" }
        val table = { name: String -> "`${sanitizedPrefix + name}`" }
        val idx = { name: String -> "`${sanitizedPrefix + name}`" }
        return listOf(
            // 为 reward_record 表添加 bilibili_mid 列
            "ALTER TABLE ${table("reward_record")} ADD COLUMN `bilibili_mid` BIGINT UNSIGNED NULL;",
            // 为 reward_record 表添加按 bilibili_mid + target_key 查询的索引
            "ALTER TABLE ${table("reward_record")} ADD INDEX ${idx("reward_mid_target_idx")} (`bilibili_mid`, `target_key`);"
        )
    }
}

