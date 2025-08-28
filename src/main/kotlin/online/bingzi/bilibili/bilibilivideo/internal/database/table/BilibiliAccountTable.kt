package online.bingzi.bilibili.bilibilivideo.internal.database.table

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import taboolib.module.database.Host
import taboolib.module.database.Table
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.ColumnOptionSQL

/**
 * Bilibili账户信息表定义
 * 存储B站用户的认证信息，包括Cookie和刷新令牌
 */
object BilibiliAccountTable {
    
    /**
     * 创建 bilibili_account 表
     */
    fun createTable(host: Host<*>): Table<*, *> {
        return Table("${DatabaseConfig.getTableName("bilibili_account")}", host) {
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
}