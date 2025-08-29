package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.*

/**
 * Bilibili账户信息表统一实现
 * 存储B站用户的认证信息，包括Cookie和刷新令牌
 * 支持MySQL和SQLite双数据库
 */
object BilibiliAccountTable {

    /**
     * 创建 bilibili_account 表
     * 根据Host类型自动适配MySQL或SQLite
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("bilibili_account")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    // MySQL实现
                    add("mid") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.PRIMARY_KEY)
                        }
                    }
                    add("nickname") {
                        type(ColumnTypeSQL.VARCHAR, 32) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("sessdata") {
                        type(ColumnTypeSQL.TEXT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("buvid3") {
                        type(ColumnTypeSQL.VARCHAR, 255)
                    }
                    add("bili_jct") {
                        type(ColumnTypeSQL.VARCHAR, 255)
                    }
                    add("refresh_token") {
                        type(ColumnTypeSQL.TEXT)
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
                    add("mid") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.PRIMARY_KEY)
                        }
                    }
                    add("nickname") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("sessdata") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("buvid3") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("bili_jct") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    add("refresh_token") {
                        type(ColumnTypeSQLite.TEXT)
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
                }
            }

            else -> {
                throw IllegalArgumentException("unknown database type")
            }
        }
    }
}
