package online.bingzi.bilibili.video.internal.rewards

import online.bingzi.bilibili.video.api.event.reward.RewardCheckEvent
import online.bingzi.bilibili.video.api.event.reward.RewardClaimEvent
import online.bingzi.bilibili.video.internal.database.dao.RewardConfigDaoService
import online.bingzi.bilibili.video.internal.database.dao.UploaderVideoDaoService
import online.bingzi.bilibili.video.internal.database.dao.VideoRewardRecordDaoService
import online.bingzi.bilibili.video.internal.database.entity.RewardConfig
import online.bingzi.bilibili.video.internal.network.BilibiliVideoService
import online.bingzi.bilibili.video.internal.network.entity.VideoInfo
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * 三连奖励服务
 * 整合奖励检查和执行逻辑，提供完整的奖励管理功能
 */
@Awake
object TripleRewardService {

    /**
     * 奖励服务结果
     */
    data class RewardServiceResult(
        val success: Boolean,
        val message: String,
        val details: Map<String, Any> = emptyMap()
    )

    /**
     * 可领取奖励信息
     */
    data class AvailableReward(
        val bvId: String,
        val videoTitle: String,
        val uploaderName: String,
        val uploaderUid: Long,
        val rewardScript: String,
        val publishTime: Long
    )

    /**
     * 初始化奖励服务
     */
    @Awake(LifeCycle.ENABLE)
    fun init() {
        console().sendInfo("tripleRewardServiceInit")
    }

    /**
     * 玩家领取指定视频的奖励
     * 
     * @param player 玩家对象
     * @param bvId 视频BV号
     * @return 服务结果
     */
    fun claimReward(player: ProxyPlayer, bvId: String): CompletableFuture<RewardServiceResult> {
        return CompletableFuture.supplyAsync {
            try {
                val playerUuid = player.uniqueId.toString()

                // 触发奖励检查事件
                val checkEvent = RewardCheckEvent(playerUuid, bvId, RewardCheckEvent.CheckType.CLAIM_ATTEMPT)
                checkEvent.call()

                // 1. 检查奖励资格
                val checkResult = RewardChecker.checkRewardEligibility(playerUuid, bvId).get()
                if (!checkResult.eligible) {
                    player.sendError("rewardNotEligible", checkResult.reason)
                    return@supplyAsync RewardServiceResult(false, checkResult.reason)
                }

                val videoInfo = checkResult.videoInfo!!
                val rewardConfig = checkResult.rewardConfig!!

                // 2. 执行奖励
                val executeResult = RewardExecutor.executeReward(player, videoInfo, rewardConfig).get()
                
                if (executeResult.success) {
                    // 触发奖励领取成功事件
                    val claimEvent = RewardClaimEvent(
                        playerUuid = playerUuid,
                        bvId = bvId,
                        uploaderUid = rewardConfig.uploaderUid,
                        success = true,
                        rewardRecord = executeResult.record
                    )
                    claimEvent.call()

                    console().sendInfo("rewardClaimSuccess", player.name, bvId)
                    
                    RewardServiceResult(
                        success = true,
                        message = "奖励领取成功",
                        details = mapOf(
                            "video" to videoInfo,
                            "reward" to rewardConfig,
                            "record" to executeResult.record
                        )
                    )
                } else {
                    // 触发奖励领取失败事件
                    val claimEvent = RewardClaimEvent(
                        playerUuid = playerUuid,
                        bvId = bvId,
                        uploaderUid = rewardConfig.uploaderUid,
                        success = false,
                        errorMessage = executeResult.message
                    )
                    claimEvent.call()

                    player.sendError("rewardExecuteError", executeResult.message)
                    
                    RewardServiceResult(false, executeResult.message)
                }

            } catch (e: Exception) {
                val errorMsg = "奖励领取过程中发生错误：${e.message}"
                console().sendWarn("rewardClaimError", player.name, bvId, e.message ?: "Unknown error")
                player.sendError("rewardExecuteError", errorMsg)
                
                RewardServiceResult(false, errorMsg)
            }
        }
    }

    /**
     * 获取玩家可领取的奖励列表
     * 
     * @param player 玩家对象
     * @param limit 限制数量，默认20
     * @return 可领取奖励列表
     */
    fun getAvailableRewards(player: ProxyPlayer, limit: Int = 20): CompletableFuture<List<AvailableReward>> {
        return CompletableFuture.supplyAsync {
            try {
                val playerUuid = player.uniqueId.toString()
                val availableRewards = mutableListOf<AvailableReward>()

                // 触发奖励检查事件
                val checkEvent = RewardCheckEvent(playerUuid, "", RewardCheckEvent.CheckType.LIST_QUERY)
                checkEvent.call()

                // 1. 获取所有启用的奖励配置
                val enabledConfigs = RewardConfigDaoService.getEnabledConfigs().get()
                if (enabledConfigs.isEmpty()) {
                    return@supplyAsync emptyList()
                }

                // 2. 对每个配置的UP主获取最新视频
                enabledConfigs.forEach { config ->
                    try {
                        val videos = UploaderVideoDaoService.getVideosByUploader(config.uploaderUid).get()
                        
                        // 筛选符合条件的视频
                        videos.take(5).forEach { video -> // 每个UP主最多检查5个最新视频
                            val checkResult = RewardChecker.checkRewardEligibility(playerUuid, video.bvId).get()
                            if (checkResult.eligible && availableRewards.size < limit) {
                                availableRewards.add(AvailableReward(
                                    bvId = video.bvId,
                                    videoTitle = video.title,
                                    uploaderName = config.uploaderName,
                                    uploaderUid = config.uploaderUid,
                                    rewardScript = config.rewardScript,
                                    publishTime = video.publishTime
                                ))
                            }
                        }
                    } catch (e: Exception) {
                        console().sendWarn("rewardListError", config.uploaderUid.toString(), e.message ?: "Unknown error")
                    }
                }

                // 按发布时间排序，最新的在前
                availableRewards.sortedByDescending { it.publishTime }

            } catch (e: Exception) {
                console().sendWarn("rewardListError", player.name, e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 批量领取奖励
     * 
     * @param player 玩家对象
     * @param bvIds 视频BV号列表
     * @return 批量处理结果
     */
    fun claimBatchRewards(player: ProxyPlayer, bvIds: List<String>): CompletableFuture<Map<String, RewardServiceResult>> {
        return CompletableFuture.supplyAsync {
            val results = mutableMapOf<String, RewardServiceResult>()
            
            bvIds.forEach { bvId ->
                try {
                    val result = claimReward(player, bvId).get()
                    results[bvId] = result
                    
                    // 短暂延迟以避免过快处理
                    Thread.sleep(200)
                } catch (e: Exception) {
                    results[bvId] = RewardServiceResult(false, "批量处理失败：${e.message}")
                }
            }
            
            results
        }
    }

    /**
     * 获取玩家奖励统计信息
     * 
     * @param player 玩家对象
     * @return 统计信息
     */
    fun getPlayerRewardStats(player: ProxyPlayer): CompletableFuture<RewardChecker.RewardStatsSummary> {
        return RewardChecker.getRewardStatsSummary(player.uniqueId.toString())
    }

    /**
     * 管理员添加奖励配置
     * 
     * @param uploaderUid UP主UID
     * @param uploaderName UP主名称
     * @param rewardScript 奖励脚本
     * @param enabled 是否启用，默认为true
     * @return 操作结果
     */
    fun addRewardConfig(
        uploaderUid: Long,
        uploaderName: String,
        rewardScript: String,
        enabled: Boolean = true
    ): CompletableFuture<RewardServiceResult> {
        return CompletableFuture.supplyAsync {
            try {
                // 验证脚本语法
                val scriptValidation = RewardExecutor.validateKetherScript(rewardScript).get()
                if (!scriptValidation.success) {
                    return@supplyAsync RewardServiceResult(false, "脚本语法错误：${scriptValidation.message}")
                }

                // 检查是否已存在配置
                val existingConfig = RewardConfigDaoService.getConfigByUploaderUid(uploaderUid).get()
                if (existingConfig != null) {
                    // 更新现有配置
                    existingConfig.updateUploaderName(uploaderName)
                    existingConfig.updateRewardScript(rewardScript)
                    existingConfig.isEnabled = enabled
                    
                    val success = RewardConfigDaoService.saveConfig(existingConfig).get()
                    if (success) {
                        console().sendInfo("rewardConfigUpdated", uploaderUid.toString())
                        RewardServiceResult(true, "奖励配置已更新")
                    } else {
                        RewardServiceResult(false, "奖励配置更新失败")
                    }
                } else {
                    // 创建新配置
                    val newConfig = RewardConfig(
                        uploaderUid = uploaderUid,
                        uploaderName = uploaderName,
                        rewardScript = rewardScript,
                        isEnabled = enabled
                    )
                    
                    val success = RewardConfigDaoService.saveConfig(newConfig).get()
                    if (success) {
                        console().sendInfo("rewardConfigAdded", uploaderUid.toString())
                        RewardServiceResult(true, "奖励配置已添加")
                    } else {
                        RewardServiceResult(false, "奖励配置添加失败")
                    }
                }

            } catch (e: Exception) {
                console().sendWarn("rewardConfigError", uploaderUid.toString(), e.message ?: "Unknown error")
                RewardServiceResult(false, "配置操作失败：${e.message}")
            }
        }
    }

    /**
     * 管理员删除奖励配置
     * 
     * @param uploaderUid UP主UID
     * @return 操作结果
     */
    fun removeRewardConfig(uploaderUid: Long): CompletableFuture<RewardServiceResult> {
        return CompletableFuture.supplyAsync {
            try {
                val success = RewardConfigDaoService.deleteConfig(uploaderUid).get()
                if (success) {
                    console().sendInfo("rewardConfigRemoved", uploaderUid.toString())
                    RewardServiceResult(true, "奖励配置已删除")
                } else {
                    RewardServiceResult(false, "奖励配置不存在或删除失败")
                }
            } catch (e: Exception) {
                console().sendWarn("rewardConfigDeleteError", uploaderUid.toString(), e.message ?: "Unknown error")
                RewardServiceResult(false, "删除配置失败：${e.message}")
            }
        }
    }

    /**
     * 获取所有奖励配置
     * 
     * @return 奖励配置列表
     */
    fun getAllRewardConfigs(): CompletableFuture<List<RewardConfig>> {
        return RewardConfigDaoService.getAllConfigs()
    }

    /**
     * 切换奖励配置启用状态
     * 
     * @param uploaderUid UP主UID
     * @param enabled 是否启用
     * @return 操作结果
     */
    fun toggleRewardConfig(uploaderUid: Long, enabled: Boolean): CompletableFuture<RewardServiceResult> {
        return CompletableFuture.supplyAsync {
            try {
                val success = RewardConfigDaoService.toggleConfigEnabled(uploaderUid, enabled).get()
                if (success) {
                    val status = if (enabled) "启用" else "禁用"
                    console().sendInfo("rewardConfigToggled", uploaderUid.toString(), status)
                    RewardServiceResult(true, "奖励配置已$status")
                } else {
                    RewardServiceResult(false, "奖励配置不存在或操作失败")
                }
            } catch (e: Exception) {
                console().sendWarn("rewardConfigToggleError", uploaderUid.toString(), e.message ?: "Unknown error")
                RewardServiceResult(false, "切换状态失败：${e.message}")
            }
        }
    }

    /**
     * 检查奖励系统状态
     * 
     * @return 系统状态信息
     */
    fun getSystemStatus(): CompletableFuture<Map<String, Any>> {
        return CompletableFuture.supplyAsync {
            try {
                val configCount = RewardConfigDaoService.countConfigs().get()
                val enabledConfigCount = RewardConfigDaoService.countConfigs(true).get()
                val recentRecords = VideoRewardRecordDaoService.getRecentRecords(10).get()
                
                mapOf(
                    "totalConfigs" to configCount,
                    "enabledConfigs" to enabledConfigCount,
                    "recentRewardCount" to recentRecords.size,
                    "systemEnabled" to getRewardSystemEnabled(),
                    "lastUpdateTime" to System.currentTimeMillis()
                )
            } catch (e: Exception) {
                console().sendWarn("rewardSystemStatusError", e.message ?: "Unknown error")
                mapOf(
                    "error" to "获取系统状态失败：${e.message}",
                    "lastUpdateTime" to System.currentTimeMillis()
                )
            }
        }
    }

    /**
     * 检查奖励系统是否启用
     */
    private fun getRewardSystemEnabled(): Boolean {
        return online.bingzi.bilibili.video.internal.config.ConfigManager.mainConfig.getBoolean("reward.enabled", true)
    }
}