package online.bingzi.bilibili.bilibilivideo.internal.reward

import online.bingzi.bilibili.bilibilivideo.api.event.VideoTripleStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import online.bingzi.bilibili.bilibilivideo.internal.config.SettingConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.entity.VideoRewardRecord
import online.bingzi.bilibili.bilibilivideo.internal.helper.ketherEval
import online.bingzi.bilibili.bilibilivideo.internal.manager.BvManager
import org.bukkit.entity.Player
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.submit
import taboolib.platform.util.sendInfo
import taboolib.platform.util.sendWarn

/**
 * 奖励管理类
 * 
 * 负责处理视频三连奖励的发放逻辑。
 * 监听VideoTripleStatusCheckEvent事件，根据配置发放对应奖励。
 * 支持防重复领奖和多种奖励类型。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object RewardManager {
    
    /**
     * 监听视频三连状态检查事件
     */
    @SubscribeEvent
    fun onVideoTripleStatusCheck(event: VideoTripleStatusCheckEvent) {
        val player = event.player
        val tripleData = event.tripleData
        
        // 延迟处理，避免与API请求冲突
        val settings = SettingConfig.getRewardSettings()
        submit(delay = (settings.rewardDelay / 50).toLong()) {
            // 在同步环境中调用异步处理方法
            taboolib.common.platform.function.submit(async = true) {
                processReward(player, tripleData)
            }
        }
    }
    
    /**
     * 处理奖励发放
     * 
     * @param player 玩家
     * @param tripleData 三连数据
     */
    private fun processReward(player: Player, tripleData: VideoTripleData) {
        val bvid = tripleData.bvid
        val settings = SettingConfig.getRewardSettings()

        // 选择奖励配置（优先特定视频，否则使用默认）
        val (videoConfig, defaultConfig) = BvManager.getRewardConfig(bvid)
        val chosenConfig: Any = when {
            videoConfig?.enabled == true -> videoConfig
            defaultConfig?.enabled == true -> defaultConfig
            else -> return // 没有启用的奖励配置
        }

        fun proceedWithReward() {
            // 检查三连要求
            val requireCompleteTriple = when (chosenConfig) {
                is VideoRewardConfig -> chosenConfig.requireCompleteTriple
                is DefaultRewardConfig -> chosenConfig.requireCompleteTriple
                else -> true
            }

            val meetsRequirement = if (requireCompleteTriple) {
                tripleData.hasTripleAction() // 需要完整三连
            } else {
                // 只需要任何一个操作（点赞、投币或收藏）
                tripleData.isLiked || tripleData.coinCount > 0 || tripleData.isFavorited
            }

            if (!meetsRequirement) {
                val requirement = if (requireCompleteTriple) "完整三连" else "至少一项操作"
                player.sendWarn("rewardRequirementNotMet", requirement)
                return
            }

            // 发放奖励
            val rewards = when (chosenConfig) {
                is VideoRewardConfig -> chosenConfig.rewards
                is DefaultRewardConfig -> chosenConfig.rewards
                else -> emptyList()
            }

            if (rewards.isEmpty()) {
                return
            }

            // 使用KetherHelper执行奖励脚本
            var rewardGiven = false
            try {
                rewards.ketherEval(getProxyPlayer(player.uniqueId)!!)
                rewardGiven = true
            } catch (e: Exception) {
                taboolib.common.platform.function.warning("执行奖励脚本失败: ${e.message}")
                rewardGiven = false
            }

            if (rewardGiven) {
                // 播放音效
                if (settings.playSound) {
                    val sound = settings.getSoundType()
                    if (sound != null) {
                        player.playSound(
                            player.location,
                            sound,
                            settings.soundVolume,
                            settings.soundPitch
                        )
                    }
                }

                // 记录奖励发放
                recordReward(player, tripleData, chosenConfig)

                // 发送成功消息
                val videoName = if (videoConfig != null) videoConfig.name else bvid
                player.sendInfo("rewardReceived", videoName)
            }
        }

        // 防重复检查（异步）
        if (settings.preventDuplicateRewards) {
            online.bingzi.bilibili.bilibilivideo.internal.database.service.RewardRecordService
                .hasPlayerReceivedReward(player.uniqueId.toString(), bvid) { has ->
                    if (has) {
                        player.sendWarn("rewardAlreadyClaimed", bvid)
                    } else {
                        proceedWithReward()
                    }
                }
        } else {
            proceedWithReward()
        }
    }
    
    /**
     * 记录奖励发放
     * 
     * @param player 玩家
     * @param tripleData 三连数据
     * @param config 奖励配置
     */
    private fun recordReward(
        player: Player,
        tripleData: VideoTripleData,
        config: Any
    ) {
        val rewardType = when (config) {
            is VideoRewardConfig -> "specific"
            is DefaultRewardConfig -> "default"
            else -> "unknown"
        }
        
        val rewardDataJson = try {
            "脚本奖励发放成功"
        } catch (e: Exception) {
            null
        }
        
        val record = VideoRewardRecord(
            bvid = tripleData.bvid,
            mid = tripleData.mid,
            playerUuid = player.uniqueId.toString(),
            rewardType = rewardType,
            rewardData = rewardDataJson,
            isLiked = tripleData.isLiked,
            isCoined = tripleData.coinCount > 0,
            isFavorited = tripleData.isFavorited,
            createPlayer = player.name,
            updatePlayer = player.name
        )
        
        // 保存奖励记录
        online.bingzi.bilibili.bilibilivideo.internal.database.service.RewardRecordService
            .saveVideoRewardRecord(record) { success ->
                if (!success) {
                    taboolib.common.platform.function.severe("保存奖励记录失败: ${tripleData.bvid} / ${player.name}")
                }
            }
    }
    
    /**
     * 检查玩家是否已经领取过奖励
     * 
     * @param playerUuid 玩家UUID
     * @param bvid BV号
     * @return Boolean 是否已经领取过
     */
    private fun hasPlayerReceivedReward(playerUuid: String, bvid: String): Boolean {
        // 这里需要通过DatabaseService查询
        // 目前先返回false，等DatabaseService扩展后再实现
        return false
    }
    
    /**
     * 手动给予奖励（管理员命令使用）
     * 
     * @param player 玩家
     * @param bvid BV号
     * @param forceGive 是否强制给予（忽略重复检查）
     */
    fun giveRewardManually(player: Player, bvid: String, forceGive: Boolean = false) {
        // 创建模拟的三连数据
        val tripleData = VideoTripleData(
            bvid = bvid,
            mid = 0L, // 管理员手动给予时可能没有MID
            playerUuid = player.uniqueId.toString(),
            isLiked = true,
            coinCount = 2,
            isFavorited = true
        )
        
        if (forceGive) {
            // 临时禁用重复检查
            val originalSettings = SettingConfig.getRewardSettings()
            // 这里需要临时修改设置，但考虑到配置是只读的，直接处理
            processRewardForced(player, tripleData)
        } else {
            processReward(player, tripleData)
        }
    }
    
    /**
     * 强制处理奖励（忽略重复检查）
     */
    private fun processRewardForced(player: Player, tripleData: VideoTripleData) {
        val bvid = tripleData.bvid
        val (videoConfig, defaultConfig) = BvManager.getRewardConfig(bvid)
        
        val config = if (videoConfig?.enabled == true) {
            videoConfig
        } else if (defaultConfig?.enabled == true) {
            defaultConfig
        } else {
            player.sendWarn("noRewardConfigured", bvid)
            return
        }
        
        val rewards = when (config) {
            is VideoRewardConfig -> config.rewards
            is DefaultRewardConfig -> config.rewards
            else -> emptyList()
        }
        
        if (rewards.isEmpty()) {
            player.sendWarn("noValidRewards", bvid)
            return
        }
        
        var rewardGiven = false
        try {
            rewards.ketherEval(getProxyPlayer(player.uniqueId)!!)
            rewardGiven = true
        } catch (e: Exception) {
            taboolib.common.platform.function.warning("执行奖励脚本失败: ${e.message}")
            rewardGiven = false
        }
        
        if (rewardGiven) {
            val settings = SettingConfig.getRewardSettings()
            if (settings.playSound) {
                val sound = settings.getSoundType()
                if (sound != null) {
                    player.playSound(
                        player.location,
                        sound,
                        settings.soundVolume,
                        settings.soundPitch
                    )
                }
            }
            
            val videoName = if (videoConfig != null) videoConfig.name else bvid
            player.sendInfo("rewardReceivedManual", videoName)
        }
    }
}
