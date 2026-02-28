package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.*

/**
 * 奖励发放记录。
 */
internal data class RewardRecord(
    val id: Long? = null,
    val playerUuid: String,
    val playerName: String,
    val bilibiliMid: Long? = null,
    val targetKey: String,
    val rewardKey: String,
    val status: Int,
    val issuedAt: Long,
    val context: String? = null,
    val failReason: String? = null
)

internal object RewardRecords : Table<Nothing>(DATABASE_TABLE_PREFIX + "reward_record") {

    val id = long("id").primaryKey()
    val playerUuid = varchar("player_uuid")
    val playerName = varchar("player_name")
    val bilibiliMid = long("bilibili_mid")
    val targetKey = varchar("target_key")
    val rewardKey = varchar("reward_key")
    val status = int("status")
    val issuedAt = long("issued_at")
    val context = text("context")
    val failReason = varchar("fail_reason")
}

internal fun QueryRowSet.toRewardRecord() = RewardRecord(
    id = this[RewardRecords.id],
    playerUuid = this[RewardRecords.playerUuid]!!,
    playerName = this[RewardRecords.playerName]!!,
    bilibiliMid = this[RewardRecords.bilibiliMid],
    targetKey = this[RewardRecords.targetKey]!!,
    rewardKey = this[RewardRecords.rewardKey]!!,
    status = this[RewardRecords.status]!!,
    issuedAt = this[RewardRecords.issuedAt]!!,
    context = this[RewardRecords.context],
    failReason = this[RewardRecords.failReason]
)
