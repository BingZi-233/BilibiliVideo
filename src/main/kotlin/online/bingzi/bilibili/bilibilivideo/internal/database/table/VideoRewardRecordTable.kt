package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.*

/**
 * 视频奖励记录表定义
 * 
 * 用于存储玩家对视频的奖励领取记录。
 * 防止重复领取奖励，记录奖励发放历史。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object VideoRewardRecordTable {
    
    /** 表名 */
    const val TABLE_NAME = "video_reward_record"
    
    /** 完整表名（带前缀） */
    val FULL_TABLE_NAME: String
        get() = DatabaseConfig.getTableName(TABLE_NAME)
    
    /**
     * 创建表定义
     * 
     * @param host 数据库主机对象
     * @return Table<*, *> TabooLib表定义对象
     */
    fun createTable(host: Host<*>): Table<*, *> {
        val tableName = DatabaseConfig.getTableName(TABLE_NAME)
        
        return when (host) {
            is HostSQL -> {
                Table(tableName, host) {
                    // MySQL实现
                    add("id") {
                        type(ColumnTypeSQL.BIGINT, 20) {
                            options(ColumnOptionSQL.PRIMARY_KEY)
                            options(ColumnOptionSQL.AUTO_INCREMENT)
                        }
                    }
                    
                    add("bvid") {
                        type(ColumnTypeSQL.VARCHAR, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("mid") {
                        type(ColumnTypeSQL.BIGINT, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("player_uuid") {
                        type(ColumnTypeSQL.VARCHAR, 36) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("reward_type") {
                        type(ColumnTypeSQL.VARCHAR, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                            def("default")
                        }
                    }
                    
                    add("reward_data") {
                        type(ColumnTypeSQL.TEXT)
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
                        type(ColumnTypeSQL.BIGINT, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("update_time") {
                        type(ColumnTypeSQL.BIGINT, 20) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("create_player") {
                        type(ColumnTypeSQL.VARCHAR, 64) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                    
                    add("update_player") {
                        type(ColumnTypeSQL.VARCHAR, 64) {
                            options(ColumnOptionSQL.NOTNULL)
                        }
                    }
                }
            }
            
            is HostSQLite -> {
                Table(tableName, host) {
                    // SQLite实现
                    add("id") {
                        type(ColumnTypeSQLite.INTEGER) {
                            options(ColumnOptionSQLite.PRIMARY_KEY)
                            options(ColumnOptionSQLite.AUTOINCREMENT)
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
                    
                    add("reward_type") {
                        type(ColumnTypeSQLite.TEXT) {
                            options(ColumnOptionSQLite.NOTNULL)
                            def("default")
                        }
                    }
                    
                    add("reward_data") {
                        type(ColumnTypeSQLite.TEXT)
                    }
                    
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
                }
            }
            
            else -> throw IllegalArgumentException("不支持的数据库类型: ${host::class}")
        }
    }
}