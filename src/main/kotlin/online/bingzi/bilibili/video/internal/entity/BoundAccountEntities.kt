package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 玩家与 B 站账号绑定关系。
 */
internal data class BoundAccount(
    val id: Long? = null,
    val playerUuid: String,
    val playerName: String,
    val bilibiliMid: Long,
    val bilibiliName: String,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long
)

internal object BoundAccounts : Table<Nothing>(DATABASE_TABLE_PREFIX + "bound_account") {

    val id = long("id").primaryKey()
    val playerUuid = varchar("player_uuid")
    val playerName = varchar("player_name")
    val bilibiliMid = long("bilibili_mid")
    val bilibiliName = varchar("bilibili_name")
    val status = int("status")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
}

internal fun QueryRowSet.toBoundAccount() = BoundAccount(
    id = this[BoundAccounts.id],
    playerUuid = this[BoundAccounts.playerUuid]!!,
    playerName = this[BoundAccounts.playerName]!!,
    bilibiliMid = this[BoundAccounts.bilibiliMid]!!,
    bilibiliName = this[BoundAccounts.bilibiliName]!!,
    status = this[BoundAccounts.status]!!,
    createdAt = this[BoundAccounts.createdAt]!!,
    updatedAt = this[BoundAccounts.updatedAt]!!
)
