package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.*

/**
 * 玩家-MID绑定表统一实现
 * 管理Minecraft玩家与Bilibili MID的一对一绑定关系
 * 支持MySQL和SQLite双数据库
 */
object PlayerBindingTable {

    /**
     * 创建 player_binding 表
     * 根据Host类型自动适配MySQL或SQLite
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("player_binding")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    // MySQL实现
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
                }
            }

            is HostSQLite -> {
                Table(tableName, host) {
                    // SQLite实现
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
                    // SQLite中使用UNIQUE约束而不是UNIQUE_KEY
                    table.index("unique_mid", listOf("mid"), unique = true)
                }
            }

            else -> {
                throw IllegalArgumentException("unknown database type")
            }
        }
    }
}