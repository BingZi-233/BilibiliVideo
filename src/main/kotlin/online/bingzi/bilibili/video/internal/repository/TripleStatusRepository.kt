package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.database.SqliteWriteExecutor
import online.bingzi.bilibili.video.internal.entity.TripleStatus
import online.bingzi.bilibili.video.internal.entity.TripleStatuses
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf

/**
 * 三连状态仓储。
 */
internal object TripleStatusRepository {

    private val db get() = DatabaseFactory.database()

    private val tripleStatuses get() = db.sequenceOf(TripleStatuses)

    fun findByPlayerAndTarget(playerUuid: String, targetKey: String): TripleStatus? {
        return tripleStatuses.find {
            (it.playerUuid eq playerUuid) and (it.targetKey eq targetKey)
        }
    }

    fun insert(
        playerUuid: String,
        bilibiliMid: Long,
        targetKey: String,
        targetBvid: String,
        lastStatus: Int = 0,
        lastTripleTime: Long? = null,
        lastCheckTime: Long? = null,
        lastErrorCode: Int? = null,
        lastErrorMessage: String? = null
    ): Int = SqliteWriteExecutor.executeWrite {
        db.insert(TripleStatuses) {
            set(it.playerUuid, playerUuid)
            set(it.bilibiliMid, bilibiliMid)
            set(it.targetKey, targetKey)
            set(it.targetBvid, targetBvid)
            set(it.lastStatus, lastStatus)
            set(it.lastTripleTime, lastTripleTime)
            set(it.lastCheckTime, lastCheckTime)
            set(it.lastErrorCode, lastErrorCode)
            set(it.lastErrorMessage, lastErrorMessage)
        }
    }

    fun updateStatus(
        playerUuid: String,
        targetKey: String,
        lastStatus: Int,
        lastTripleTime: Long?,
        lastCheckTime: Long,
        lastErrorCode: Int?,
        lastErrorMessage: String?
    ): Int = SqliteWriteExecutor.executeWrite {
        db.update(TripleStatuses) {
            set(it.lastStatus, lastStatus)
            set(it.lastTripleTime, lastTripleTime)
            set(it.lastCheckTime, lastCheckTime)
            set(it.lastErrorCode, lastErrorCode)
            set(it.lastErrorMessage, lastErrorMessage)
            where {
                (it.playerUuid eq playerUuid) and (it.targetKey eq targetKey)
            }
        }
    }
}
