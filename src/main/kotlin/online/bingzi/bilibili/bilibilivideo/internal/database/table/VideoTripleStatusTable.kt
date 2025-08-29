package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.*

/**
 * 视频三连状态表统一实现
 * 记录玩家对某个视频的点赞、投币、收藏状态
 * 支持MySQL和SQLite双数据库
 */
object VideoTripleStatusTable {

    /**
     * 创建 video_triple_status 表
     * 根据Host类型自动适配MySQL或SQLite
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName("video_triple_status")

        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    // MySQL实现
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
                    add("mid") {
                        type(ColumnTypeSQL.BIGINT) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("player_uuid") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    add("is_liked") {
                        type(ColumnTypeSQL.BOOLEAN) {
                            options(ColumnOptionSQL.NOTNULL)
                            def(false)
                        }
                    }
                    add("is_coined") {
                        type(ColumnTypeSQL.BOOLEAN) {
                            options(ColumnOptionSQL.NOTNULL)
                            def(false)
                        }
                    }
                    add("is_favorited") {
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
                    table.index("unique_video_triple", listOf("bvid", "mid", "player_uuid"), unique = true)
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
                    add("bvid") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                        }
                    }
                    add("mid") {
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
                    add("is_liked") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def(0)
                        }
                    }
                    add("is_coined") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def(0)
                        }
                    }
                    add("is_favorited") {
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
                    table.index("unique_video_triple", listOf("bvid", "mid", "player_uuid"), unique = true)
                }
            }

            else -> {
                throw IllegalArgumentException("unknown database type")
            }
        }
    }
}