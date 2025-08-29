package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.*

/**
 * UP主关注状态表统一实现
 * 记录玩家对UP主的关注状态
 * 支持MySQL和SQLite双数据库
 */
object UpFollowStatusTable {

    /**
     * 创建 up_follow_status 表
     * 根据Host类型自动适配MySQL或SQLite
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("up_follow_status")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    // MySQL实现
                    add("id") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.PRIMARY_KEY, ColumnOptionSQL.AUTO_INCREMENT)
                        }
                    }
                    add("up_mid") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("follower_mid") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("player_uuid") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("is_following") {
                        type(ColumnTypeSQL.BOOLEAN) {
                            options(ColumnOptionSQL.NOTNULL)
                            def(false)
                        }
                    }
                    add("create_time") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("update_time") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("create_player") {
                        type(ColumnTypeSQL.VARCHAR, 16) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("update_player") {
                        type(ColumnTypeSQL.VARCHAR, 16) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }.also { table ->
                    // 添加联合唯一索引
                    table.index("unique_up_follow", listOf("up_mid", "follower_mid", "player_uuid"), unique = true)
                }
            }

            is HostSQLite -> {
                Table(tableName, host) {
                    // SQLite实现
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.PRIMARY_KEY, ColumnOptionSQLite.AUTOINCREMENT)
                        }
                    }
                    add("up_mid") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("follower_mid") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("player_uuid") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    // SQLite中BOOLEAN使用INTEGER类型 (0/1)
                    add("is_following") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def(0)
                        }
                    }
                    add("create_time") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("update_time") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("create_player") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("update_player") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                }.also { table ->
                    // 添加联合唯一索引
                    table.index("unique_up_follow", listOf("up_mid", "follower_mid", "player_uuid"), unique = true)
                }
            }

            else -> {
                throw IllegalArgumentException("unknown database type")
            }
        }
    }
}