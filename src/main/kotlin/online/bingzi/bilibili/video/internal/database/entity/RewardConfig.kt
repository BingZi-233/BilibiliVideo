package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * 奖励配置实体
 * 存储UP主的奖励配置信息
 */
@DatabaseTable(tableName = "reward_configs")
data class RewardConfig(
    /**
     * 自增主键ID
     */
    @DatabaseField(generatedId = true, columnName = "id")
    var id: Long = 0,

    /**
     * UP主UID（唯一键）
     */
    @DatabaseField(columnName = "uploader_uid", canBeNull = false, unique = true, index = true)
    var uploaderUid: Long = 0,

    /**
     * UP主名称
     */
    @DatabaseField(columnName = "uploader_name", canBeNull = false, width = 200)
    var uploaderName: String = "",

    /**
     * 奖励脚本（Kether脚本）
     */
    @DatabaseField(columnName = "reward_script", canBeNull = false, columnDefinition = "TEXT")
    var rewardScript: String = "",

    /**
     * 是否启用
     */
    @DatabaseField(columnName = "is_enabled")
    var isEnabled: Boolean = true,

    /**
     * 最小视频发布天数
     */
    @DatabaseField(columnName = "min_video_age_days")
    var minVideoAgeDays: Int = 0,

    /**
     * 最大视频发布天数
     */
    @DatabaseField(columnName = "max_video_age_days")
    var maxVideoAgeDays: Int = 7,

    /**
     * 记录创建时间
     */
    @DatabaseField(columnName = "created_at", canBeNull = false)
    var createdAt: Long = System.currentTimeMillis(),

    /**
     * 记录更新时间
     */
    @DatabaseField(columnName = "updated_at", canBeNull = false)
    var updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * OrmLite需要的无参构造函数
     */
    constructor() : this(0)

    /**
     * 带UP主信息的构造函数
     */
    constructor(
        uploaderUid: Long,
        uploaderName: String,
        rewardScript: String,
        isEnabled: Boolean = true
    ) : this(
        uploaderUid = uploaderUid,
        uploaderName = uploaderName,
        rewardScript = rewardScript,
        isEnabled = isEnabled
    )

    /**
     * 检查视频是否在有效的年龄范围内
     * 
     * @param videoPublishTime 视频发布时间戳
     * @return 是否在有效范围内
     */
    fun isVideoInValidAge(videoPublishTime: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val videoAge = (currentTime - videoPublishTime) / (24 * 60 * 60 * 1000) // 转换为天数
        
        return videoAge >= minVideoAgeDays && videoAge <= maxVideoAgeDays
    }

    /**
     * 更新记录时间
     */
    fun updateTimestamp() {
        updatedAt = System.currentTimeMillis()
    }

    /**
     * 更新UP主名称
     */
    fun updateUploaderName(newName: String) {
        uploaderName = newName
        updateTimestamp()
    }

    /**
     * 更新奖励脚本
     */
    fun updateRewardScript(newScript: String) {
        rewardScript = newScript
        updateTimestamp()
    }

    /**
     * 切换启用状态
     */
    fun toggleEnabled() {
        isEnabled = !isEnabled
        updateTimestamp()
    }

    /**
     * 设置视频年龄范围
     */
    fun setVideoAgeRange(minDays: Int, maxDays: Int) {
        minVideoAgeDays = minDays
        maxVideoAgeDays = maxDays
        updateTimestamp()
    }

    override fun toString(): String {
        return "RewardConfig(uploaderUid=$uploaderUid, uploaderName='$uploaderName', enabled=$isEnabled, ageRange=$minVideoAgeDays-$maxVideoAgeDays days)"
    }
}