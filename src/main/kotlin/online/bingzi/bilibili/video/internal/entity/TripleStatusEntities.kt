package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 玩家针对某个视频任务（三连目标）的检测状态。
 */
internal interface TripleStatus : Entity<TripleStatus> {

    companion object : Entity.Factory<TripleStatus>()

    var id: Long?
    var playerUuid: String
    var bilibiliMid: Long
    var targetKey: String
    var targetBvid: String
    var lastStatus: Int
    var lastTripleTime: Long?
    var lastCheckTime: Long?
    var lastErrorCode: Int?
    var lastErrorMessage: String?
}

internal object TripleStatuses : Table<TripleStatus>(DATABASE_TABLE_PREFIX + "triple_status") {

    val id = long("id").primaryKey().bindTo(TripleStatus::id)
    val playerUuid = varchar("player_uuid").bindTo(TripleStatus::playerUuid)
    val bilibiliMid = long("bilibili_mid").bindTo(TripleStatus::bilibiliMid)
    val targetKey = varchar("target_key").bindTo(TripleStatus::targetKey)
    val targetBvid = varchar("target_bvid").bindTo(TripleStatus::targetBvid)
    val lastStatus = int("last_status").bindTo(TripleStatus::lastStatus)
    val lastTripleTime = long("last_triple_time").bindTo(TripleStatus::lastTripleTime)
    val lastCheckTime = long("last_check_time").bindTo(TripleStatus::lastCheckTime)
    val lastErrorCode = int("last_error_code").bindTo(TripleStatus::lastErrorCode)
    val lastErrorMessage = varchar("last_error_message").bindTo(TripleStatus::lastErrorMessage)
}
