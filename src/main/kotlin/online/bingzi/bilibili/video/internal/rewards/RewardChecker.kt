package online.bingzi.bilibili.video.internal.rewards

import online.bingzi.bilibili.video.internal.config.ConfigManager
import online.bingzi.bilibili.video.internal.database.dao.PlayerRewardStatsDaoService
import online.bingzi.bilibili.video.internal.database.dao.RewardConfigDaoService
import online.bingzi.bilibili.video.internal.database.dao.VideoRewardRecordDaoService
import online.bingzi.bilibili.video.internal.database.entity.PlayerRewardStats
import online.bingzi.bilibili.video.internal.database.entity.RewardConfig
import online.bingzi.bilibili.video.internal.network.BilibiliVideoService
import online.bingzi.bilibili.video.internal.network.entity.VideoInfo
import online.bingzi.bilibili.video.internal.network.entity.TripleActionStatus
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * 奖励检查器
 * 负责检查玩家是否符合领取视频三连奖励的条件
 */
object RewardChecker {

    /**
     * 奖励检查结果
     */
    data class RewardCheckResult(
        val eligible: Boolean,
        val reason: String,
        val videoInfo: VideoInfo? = null,
        val rewardConfig: RewardConfig? = null,
        val tripleStatus: TripleActionStatus? = null
    )

    /**
     * 检查玩家是否可以领取指定视频的奖励
     * 
     * @param playerUuid 玩家UUID
     * @param bvId 视频BV号
     * @return 检查结果
     */
    fun checkRewardEligibility(playerUuid: String, bvId: String): CompletableFuture<RewardCheckResult> {
        return CompletableFuture.supplyAsync {
            try {
                // 1. 检查奖励系统是否启用
                if (!getRewardSystemEnabled()) {
                    return@supplyAsync RewardCheckResult(false, "奖励系统未启用")
                }

                // 2. 获取视频信息
                val videoInfo = BilibiliVideoService.getVideoInfo(bvId).get()
                    ?: return@supplyAsync RewardCheckResult(false, "视频不存在或获取失败")

                // 3. 检查UP主是否配置了奖励
                val rewardConfig = RewardConfigDaoService.getConfigByUploaderUid(videoInfo.uploader.uid).get()
                    ?: return@supplyAsync RewardCheckResult(false, "该UP主未配置奖励")

                // 4. 检查奖励配置是否启用
                if (!rewardConfig.isEnabled) {
                    return@supplyAsync RewardCheckResult(false, "该UP主的奖励配置已禁用")
                }

                // 5. 检查视频发布时间是否在有效范围内
                if (!rewardConfig.isVideoInValidAge(videoInfo.publishTime ?: 0)) {
                    return@supplyAsync RewardCheckResult(false, "视频发布时间不在奖励有效期内")
                }

                // 6. 检查玩家是否已经领取过此视频的奖励
                val alreadyClaimed = VideoRewardRecordDaoService.hasClaimedReward(playerUuid, bvId).get()
                if (alreadyClaimed) {
                    return@supplyAsync RewardCheckResult(false, "您已经领取过该视频的奖励")
                }

                // 7. 检查玩家今日奖励次数是否达到上限
                val dailyLimit = getRewardDailyLimit()
                val reachedLimit = PlayerRewardStatsDaoService.hasReachedDailyLimit(playerUuid, dailyLimit).get()
                if (reachedLimit) {
                    return@supplyAsync RewardCheckResult(false, "今日奖励次数已达上限")
                }

                // 8. 检查玩家是否完成了三连操作
                val tripleStatus = BilibiliVideoService.getTripleStatus(videoInfo.aid).get()
                if (!isTripleRequirementMet(tripleStatus)) {
                    return@supplyAsync RewardCheckResult(false, "未完成必要的三连操作", videoInfo, rewardConfig, tripleStatus)
                }

                // 所有检查通过
                RewardCheckResult(true, "符合奖励条件", videoInfo, rewardConfig, tripleStatus)

            } catch (e: Exception) {
                console().sendWarn("rewardCheckError", playerUuid, bvId, e.message ?: "Unknown error")
                RewardCheckResult(false, "检查过程中发生错误：${e.message}")
            }
        }
    }

    /**
     * 批量检查玩家可领取的奖励
     * 
     * @param playerUuid 玩家UUID
     * @param bvIds 视频BV号列表
     * @return 可领取的视频BV号列表
     */
    fun checkMultipleRewards(playerUuid: String, bvIds: List<String>): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            if (bvIds.isEmpty()) return@supplyAsync emptyList()

            val eligibleVideos = mutableListOf<String>()
            
            bvIds.forEach { bvId ->
                try {
                    val result = checkRewardEligibility(playerUuid, bvId).get()
                    if (result.eligible) {
                        eligibleVideos.add(bvId)
                    }
                } catch (e: Exception) {
                    console().sendWarn("rewardBatchCheckError", playerUuid, bvId, e.message ?: "Unknown error")
                }
            }

            eligibleVideos
        }
    }

    /**
     * 检查玩家今日剩余奖励次数
     * 
     * @param playerUuid 玩家UUID
     * @return 剩余次数
     */
    fun getRemainingRewards(playerUuid: String): CompletableFuture<Int> {
        return PlayerRewardStatsDaoService.getRemainingRewards(playerUuid, getRewardDailyLimit())
    }

    /**
     * 获取玩家奖励统计摘要
     * 
     * @param playerUuid 玩家UUID
     * @return 统计摘要
     */
    data class RewardStatsSummary(
        val dailyCount: Int,
        val dailyLimit: Int,
        val totalCount: Long,
        val remaining: Int
    )

    fun getRewardStatsSummary(playerUuid: String): CompletableFuture<RewardStatsSummary> {
        return CompletableFuture.supplyAsync {
            try {
                val dailyLimit = getRewardDailyLimit()
                val todayStats = PlayerRewardStatsDaoService.getOrCreateTodayStats(playerUuid).get()
                val totalCount = PlayerRewardStatsDaoService.getPlayerTotalRewards(playerUuid).get()
                
                RewardStatsSummary(
                    dailyCount = todayStats.dailyRewardCount,
                    dailyLimit = dailyLimit,
                    totalCount = totalCount,
                    remaining = todayStats.getRemainingRewards(dailyLimit)
                )
            } catch (e: Exception) {
                console().sendWarn("rewardStatsError", playerUuid, e.message ?: "Unknown error")
                RewardStatsSummary(0, getRewardDailyLimit(), 0, getRewardDailyLimit())
            }
        }
    }

    /**
     * 检查三连要求是否满足
     */
    private fun isTripleRequirementMet(tripleStatus: TripleActionStatus?): Boolean {
        if (tripleStatus == null) return false

        val requireFullTriple = getRequireFullTriple()
        if (requireFullTriple) {
            // 需要完整三连
            return tripleStatus.liked && tripleStatus.coined && tripleStatus.favorited
        } else {
            // 检查最低要求操作
            val minimumActions = getMinimumActions()
            return minimumActions.all { action ->
                when (action.uppercase()) {
                    "LIKE" -> tripleStatus.liked
                    "COIN" -> tripleStatus.coined
                    "FAVORITE" -> tripleStatus.favorited
                    else -> false
                }
            }
        }
    }

    /**
     * 获取奖励系统是否启用
     */
    private fun getRewardSystemEnabled(): Boolean {
        return ConfigManager.mainConfig.getBoolean("reward.enabled", true)
    }

    /**
     * 获取每日奖励限制
     */
    private fun getRewardDailyLimit(): Int {
        return ConfigManager.mainConfig.getInt("reward.daily-limit", 3)
    }

    /**
     * 是否需要完整三连
     */
    private fun getRequireFullTriple(): Boolean {
        return ConfigManager.mainConfig.getBoolean("reward.require-full-triple", false)
    }

    /**
     * 获取最低要求操作
     */
    private fun getMinimumActions(): List<String> {
        return ConfigManager.mainConfig.getStringList("reward.minimum-actions")
            ?: listOf("LIKE", "COIN")
    }

    /**
     * 获取视频有效天数
     */
    private fun getVideoValidDays(): Int {
        return ConfigManager.mainConfig.getInt("reward.video-valid-days", 7)
    }
}