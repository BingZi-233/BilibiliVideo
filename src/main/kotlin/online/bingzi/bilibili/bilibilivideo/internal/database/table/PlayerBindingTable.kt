package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.Host
import taboolib.module.database.Table
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnOptionSQL

/**
 * 玩家-MID绑定表定义
 * 管理Minecraft玩家与Bilibili MID的一对一绑定关系
 */
object PlayerBindingTable {
    
    /**
     * 创建 player_binding 表
     */
    fun createTable(host: Host<*>): Table<*, *> {
        return Table("${DatabaseConfig.getTableName("player_binding")}", host) {
            add("player_uuid") {
                type(ColumnTypeSQL.VARCHAR, 36) {
                    options(ColumnOptionSQL.PRIMARY_KEY)
                }
            }
            add("mid") {
                type(ColumnTypeSQL.BIGINT) {
                    options(ColumnOptionSQL.UNIQUE_KEY)
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
}