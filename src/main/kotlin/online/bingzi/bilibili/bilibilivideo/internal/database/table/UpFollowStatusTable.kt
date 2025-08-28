package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.Host
import taboolib.module.database.Table
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnOptionSQL

/**
 * UP主关注状态表定义
 * 记录玩家对UP主的关注状态
 */
object UpFollowStatusTable {
    
    /**
     * 创建 up_follow_status 表
     */
    fun createTable(host: Host<*>): Table<*, *> {
        return Table("${DatabaseConfig.getTableName("up_follow_status")}", host) {
            add("id") {
                type(ColumnTypeSQL.INT) {
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
            // 添加联合唯一索引
            index("unique_up_follow") {
                columns("up_mid", "follower_mid", "player_uuid")
                type = "UNIQUE"
            }
        }
    }
}