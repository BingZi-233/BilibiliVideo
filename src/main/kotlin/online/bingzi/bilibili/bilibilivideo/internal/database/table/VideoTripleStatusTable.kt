package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.Host
import taboolib.module.database.Table
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnOptionSQL

/**
 * 视频三连状态表定义
 * 记录玩家对某个视频的点赞、投币、收藏状态
 */
object VideoTripleStatusTable {
    
    /**
     * 创建 video_triple_status 表
     */
    fun createTable(host: Host<*>): Table<*, *> {
        return Table("${DatabaseConfig.getTableName("video_triple_status")}", host) {
            add("id") {
                type(ColumnTypeSQL.INT) {
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
            // 添加联合唯一索引
            index("unique_video_triple") {
                columns("bvid", "mid", "player_uuid")
                type = "UNIQUE"
            }
        }
    }
}