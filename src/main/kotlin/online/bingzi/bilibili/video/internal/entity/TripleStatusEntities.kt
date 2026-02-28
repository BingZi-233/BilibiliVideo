package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 玩家针对某个视频任务（三连目标）的检测状态。
 */
internal data class TripleStatus(
    val id: Long? = null,
    val playerUuid: String,
    val bilibiliMid: Long,
    val targetKey: String,
    val targetBvid: String,
    val lastStatus: Int,
    val lastTripleTime: Long? = null,
    val lastCheckTime: Long? = null,
    val lastErrorCode: Int? = null,
    val lastErrorMessage: String? = null
)

internal object TripleStatuses : Table<Nothing>(DATABASE_TABLE_PREFIX + "triple_status") {

    val id = long("id").primaryKey()
    val playerUuid = varchar("player_uuid")
    val bilibiliMid = long("bilibili_mid")
    val targetKey = varchar("target_key")
    val targetBvid = varchar("target_bvid")
    val lastStatus = int("last_status")
    val lastTripleTime = long("last_triple_time")
    val lastCheckTime = long("last_check_time")
    val lastErrorCode = int("last_error_code")
    val lastErrorMessage = varchar("last_error_message")
}

internal fun QueryRowSet.toTripleStatus() = TripleStatus(
    id = this[TripleStatuses.id],
    playerUuid = this[TripleStatuses.playerUuid]!!,
    bilibiliMid = this[TripleStatuses.bilibiliMid]!!,
    targetKey = this[TripleStatuses.targetKey]!!,
    targetBvid = this[TripleStatuses.targetBvid]!!,
    lastStatus = this[TripleStatuses.lastStatus]!!,
    lastTripleTime = this[TripleStatuses.lastTripleTime],
    lastCheckTime = this[TripleStatuses.lastCheckTime],
    lastErrorCode = this[TripleStatuses.lastErrorCode],
    lastErrorMessage = this[TripleStatuses.lastErrorMessage]
)
