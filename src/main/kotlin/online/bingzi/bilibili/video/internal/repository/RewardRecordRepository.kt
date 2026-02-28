package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.database.SqliteWriteExecutor
import online.bingzi.bilibili.video.internal.entity.RewardRecord
import online.bingzi.bilibili.video.internal.entity.RewardRecords
import online.bingzi.bilibili.video.internal.entity.toRewardRecord
import org.ktorm.dsl.*

/**
 * 奖励记录仓储。
 */
internal object RewardRecordRepository {

    private val db get() = DatabaseFactory.database()

    fun findAllByPlayerAndTarget(playerUuid: String, targetKey: String): List<RewardRecord> {
        return db.from(RewardRecords)
            .select()
            .where { (RewardRecords.playerUuid eq playerUuid) and (RewardRecords.targetKey eq targetKey) }
            .map { it.toRewardRecord() }
    }

    fun findAllByBilibiliMidAndTarget(bilibiliMid: Long, targetKey: String): List<RewardRecord> {
        return db.from(RewardRecords)
            .select()
            .where { (RewardRecords.bilibiliMid eq bilibiliMid) and (RewardRecords.targetKey eq targetKey) }
            .map { it.toRewardRecord() }
    }

    fun findAllByPlayerAndBilibiliMidAndTarget(
        playerUuid: String,
        bilibiliMid: Long,
        targetKey: String
    ): List<RewardRecord> {
        return db.from(RewardRecords)
            .select()
            .where {
                (RewardRecords.playerUuid eq playerUuid) and
                        (RewardRecords.bilibiliMid eq bilibiliMid) and
                        (RewardRecords.targetKey eq targetKey)
            }
            .map { it.toRewardRecord() }
    }

    fun insert(
        playerUuid: String,
        playerName: String,
        bilibiliMid: Long?,
        targetKey: String,
        rewardKey: String,
        status: Int,
        issuedAt: Long = System.currentTimeMillis(),
        context: String? = null,
        failReason: String? = null
    ): Int = SqliteWriteExecutor.executeWrite {
        db.insert(RewardRecords) {
            set(it.playerUuid, playerUuid)
            set(it.playerName, playerName)
            set(it.bilibiliMid, bilibiliMid)
            set(it.targetKey, targetKey)
            set(it.rewardKey, rewardKey)
            set(it.status, status)
            set(it.issuedAt, issuedAt)
            set(it.context, context)
            set(it.failReason, failReason)
        }
    }
}
