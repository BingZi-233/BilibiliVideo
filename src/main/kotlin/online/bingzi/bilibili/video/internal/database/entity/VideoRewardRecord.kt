package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * 视频奖励记录实体
 * 记录玩家领取视频三连奖励的信息
 */
@DatabaseTable(tableName = "video_reward_records")
data class VideoRewardRecord(
    /**
     * 自增主键ID
     */
    @DatabaseField(generatedId = true, columnName = "id")
    var id: Long = 0,

    /**
     * 玩家UUID
     */
    @DatabaseField(columnName = "player_uuid", canBeNull = false, index = true)
    var playerUuid: String = "",

    /**
     * UP主UID
     */
    @DatabaseField(columnName = "uploader_uid", canBeNull = false, index = true)
    var uploaderUid: Long = 0,

    /**
     * 视频BV号
     */
    @DatabaseField(columnName = "bv_id", canBeNull = false, index = true)
    var bvId: String = "",

    /**
     * 视频标题
     */
    @DatabaseField(columnName = "video_title", canBeNull = false, width = 500)
    var videoTitle: String = "",

    /**
     * 奖励类型
     */
    @DatabaseField(columnName = "reward_type", canBeNull = false, width = 50)
    var rewardType: String = "TRIPLE_ACTION",

    /**
     * 奖励领取时间戳
     */
    @DatabaseField(columnName = "reward_claimed_at", canBeNull = false)
    var rewardClaimedAt: Long = System.currentTimeMillis(),

    /**
     * 奖励内容（Kether脚本或奖励说明）
     */
    @DatabaseField(columnName = "reward_content", columnDefinition = "TEXT")
    var rewardContent: String = "",

    /**
     * 记录创建时间
     */
    @DatabaseField(columnName = "created_at", canBeNull = false)
    var createdAt: Long = System.currentTimeMillis()
) {
    /**
     * OrmLite需要的无参构造函数
     */
    constructor() : this(0)

    /**
     * 检查是否在指定日期领取
     * 
     * @param date 日期字符串 (yyyy-MM-dd)
     * @return 是否在指定日期领取
     */
    fun isClaimedOnDate(date: String): Boolean {
        val claimedDate = java.time.Instant.ofEpochMilli(rewardClaimedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        return claimedDate == date
    }

    /**
     * 获取领取日期字符串
     * 
     * @return 日期字符串 (yyyy-MM-dd)
     */
    fun getClaimedDateString(): String {
        return java.time.Instant.ofEpochMilli(rewardClaimedAt)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }

    /**
     * 更新记录创建时间（用于数据迁移等场景）
     */
    fun updateCreatedTime() {
        createdAt = System.currentTimeMillis()
    }

    override fun toString(): String {
        return "VideoRewardRecord(playerUuid='$playerUuid', bvId='$bvId', rewardType='$rewardType', claimedAt=$rewardClaimedAt)"
    }
}