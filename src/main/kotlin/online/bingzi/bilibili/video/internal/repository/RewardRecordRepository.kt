package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.database.SqliteWriteExecutor
import online.bingzi.bilibili.video.internal.entity.RewardRecord
import online.bingzi.bilibili.video.internal.entity.RewardRecords
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList

/**
 * 奖励记录仓储。
 */
internal object RewardRecordRepository {

    private val db get() = DatabaseFactory.database()

    private val rewardRecords get() = db.sequenceOf(RewardRecords)

    fun findAllByPlayerAndTarget(playerUuid: String, targetKey: String): List<RewardRecord> {
        return rewardRecords
            .filter { (it.playerUuid eq playerUuid) and (it.targetKey eq targetKey) }
            .toList()
    }

    fun findAllByBilibiliMidAndTarget(bilibiliMid: Long, targetKey: String): List<RewardRecord> {
        return rewardRecords
            .filter { (it.bilibiliMid eq bilibiliMid) and (it.targetKey eq targetKey) }
            .toList()
    }

    fun findAllByPlayerAndBilibiliMidAndTarget(
        playerUuid: String,
        bilibiliMid: Long,
        targetKey: String
    ): List<RewardRecord> {
        return rewardRecords
            .filter {
                (it.playerUuid eq playerUuid) and
                        (it.bilibiliMid eq bilibiliMid) and
                        (it.targetKey eq targetKey)
            }
            .toList()
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
