package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 玩家奖励统计实体
 * 记录玩家的每日奖励统计信息
 */
@DatabaseTable(tableName = "player_reward_stats")
data class PlayerRewardStats(
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
     * 奖励日期 (yyyy-MM-dd)
     */
    @DatabaseField(columnName = "reward_date", canBeNull = false, index = true)
    var rewardDate: String = "",

    /**
     * 当日奖励次数
     */
    @DatabaseField(columnName = "daily_reward_count")
    var dailyRewardCount: Int = 0,

    /**
     * 总计奖励次数
     */
    @DatabaseField(columnName = "total_reward_count")
    var totalRewardCount: Long = 0,

    /**
     * 最后一次奖励时间
     */
    @DatabaseField(columnName = "last_reward_time")
    var lastRewardTime: Long = 0,

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
     * 创建今日统计记录的构造函数
     */
    constructor(playerUuid: String) : this(
        playerUuid = playerUuid,
        rewardDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )

    /**
     * 创建指定日期统计记录的构造函数
     */
    constructor(playerUuid: String, date: String) : this(
        playerUuid = playerUuid,
        rewardDate = date
    )

    /**
     * 增加奖励次数
     * 
     * @param rewardTime 奖励获得时间戳，默认为当前时间
     */
    fun incrementReward(rewardTime: Long = System.currentTimeMillis()) {
        dailyRewardCount++
        totalRewardCount++
        lastRewardTime = rewardTime
        updateTimestamp()
    }

    /**
     * 检查是否达到每日限制
     * 
     * @param dailyLimit 每日限制数量
     * @return 是否达到限制
     */
    fun hasReachedDailyLimit(dailyLimit: Int): Boolean {
        return dailyRewardCount >= dailyLimit
    }

    /**
     * 检查是否是今日记录
     * 
     * @return 是否是今日记录
     */
    fun isToday(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return rewardDate == today
    }

    /**
     * 获取剩余奖励次数
     * 
     * @param dailyLimit 每日限制数量
     * @return 剩余奖励次数
     */
    fun getRemainingRewards(dailyLimit: Int): Int {
        return maxOf(0, dailyLimit - dailyRewardCount)
    }

    /**
     * 重置每日计数（用于跨日处理）
     */
    fun resetDailyCount() {
        dailyRewardCount = 0
        rewardDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        updateTimestamp()
    }

    /**
     * 更新记录时间
     */
    fun updateTimestamp() {
        updatedAt = System.currentTimeMillis()
    }

    /**
     * 获取最后奖励时间的友好显示
     * 
     * @return 格式化的时间字符串
     */
    fun getLastRewardTimeFormatted(): String {
        return if (lastRewardTime > 0) {
            java.time.Instant.ofEpochMilli(lastRewardTime)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            "从未领取"
        }
    }

    /**
     * 创建或获取今日统计对象
     */
    companion object {
        /**
         * 创建今日统计记录
         */
        fun createTodayStats(playerUuid: String): PlayerRewardStats {
            return PlayerRewardStats(playerUuid)
        }

        /**
         * 获取今日日期字符串
         */
        fun getTodayDateString(): String {
            return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    override fun toString(): String {
        return "PlayerRewardStats(playerUuid='$playerUuid', date='$rewardDate', daily=$dailyRewardCount, total=$totalRewardCount)"
    }
}