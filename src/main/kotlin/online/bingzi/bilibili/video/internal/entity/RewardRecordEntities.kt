package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.entity.Entity
import org.ktorm.schema.*

/**
 * 奖励发放记录。
 */
internal interface RewardRecord : Entity<RewardRecord> {

    companion object : Entity.Factory<RewardRecord>()

    var id: Long?
    var playerUuid: String
    var playerName: String
    var bilibiliMid: Long?
    var targetKey: String
    var rewardKey: String
    var status: Int
    var issuedAt: Long
    var context: String?
    var failReason: String?
}

internal object RewardRecords : Table<RewardRecord>(DATABASE_TABLE_PREFIX + "reward_record") {

    val id = long("id").primaryKey().bindTo(RewardRecord::id)
    val playerUuid = varchar("player_uuid").bindTo(RewardRecord::playerUuid)
    val playerName = varchar("player_name").bindTo(RewardRecord::playerName)
    val bilibiliMid = long("bilibili_mid").bindTo(RewardRecord::bilibiliMid)
    val targetKey = varchar("target_key").bindTo(RewardRecord::targetKey)
    val rewardKey = varchar("reward_key").bindTo(RewardRecord::rewardKey)
    val status = int("status").bindTo(RewardRecord::status)
    val issuedAt = long("issued_at").bindTo(RewardRecord::issuedAt)
    val context = text("context").bindTo(RewardRecord::context)
    val failReason = varchar("fail_reason").bindTo(RewardRecord::failReason)
}
