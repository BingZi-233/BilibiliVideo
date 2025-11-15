package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 玩家与 B 站账号绑定关系。
 */
internal interface BoundAccount : Entity<BoundAccount> {

    companion object : Entity.Factory<BoundAccount>()

    var id: Long?
    var playerUuid: String
    var playerName: String
    var bilibiliMid: Long
    var bilibiliName: String
    var status: Int
    var createdAt: Long
    var updatedAt: Long
}

internal object BoundAccounts : Table<BoundAccount>(DATABASE_TABLE_PREFIX + "bound_account") {

    val id = long("id").primaryKey().bindTo(BoundAccount::id)
    val playerUuid = varchar("player_uuid").bindTo(BoundAccount::playerUuid)
    val playerName = varchar("player_name").bindTo(BoundAccount::playerName)
    val bilibiliMid = long("bilibili_mid").bindTo(BoundAccount::bilibiliMid)
    val bilibiliName = varchar("bilibili_name").bindTo(BoundAccount::bilibiliName)
    val status = int("status").bindTo(BoundAccount::status)
    val createdAt = long("created_at").bindTo(BoundAccount::createdAt)
    val updatedAt = long("updated_at").bindTo(BoundAccount::updatedAt)
}
