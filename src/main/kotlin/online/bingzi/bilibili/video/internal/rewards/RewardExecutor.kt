package online.bingzi.bilibili.video.internal.rewards

import online.bingzi.bilibili.video.internal.database.dao.PlayerRewardStatsDaoService
import online.bingzi.bilibili.video.internal.database.dao.VideoRewardRecordDaoService
import online.bingzi.bilibili.video.internal.database.entity.RewardConfig
import online.bingzi.bilibili.video.internal.database.entity.VideoRewardRecord
import online.bingzi.bilibili.video.internal.network.entity.VideoInfo
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.kether.KetherShell
import taboolib.module.kether.printKetherErrorMessage
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * 奖励执行器
 * 负责执行Kether脚本奖励分发和记录保存
 */
object RewardExecutor {

    /**
     * 奖励执行结果
     */
    data class RewardExecuteResult(
        val success: Boolean,
        val message: String,
        val record: VideoRewardRecord? = null,
        val error: Throwable? = null
    )

    /**
     * 执行奖励发放
     * 
     * @param player 玩家对象
     * @param videoInfo 视频信息
     * @param rewardConfig 奖励配置
     * @return 执行结果
     */
    fun executeReward(
        player: ProxyPlayer,
        videoInfo: VideoInfo,
        rewardConfig: RewardConfig
    ): CompletableFuture<RewardExecuteResult> {
        return CompletableFuture.supplyAsync {
            try {
                val playerUuid = player.uniqueId.toString()
                val currentTime = System.currentTimeMillis()

                // 1. 创建奖励记录
                val rewardRecord = VideoRewardRecord(
                    playerUuid = playerUuid,
                    uploaderUid = rewardConfig.uploaderUid,
                    bvId = videoInfo.bvid,
                    videoTitle = videoInfo.title,
                    rewardType = "TRIPLE_ACTION",
                    rewardClaimedAt = currentTime,
                    rewardContent = rewardConfig.rewardScript,
                    createdAt = currentTime
                )

                // 2. 执行Kether脚本
                val scriptResult = executeKetherScript(player, rewardConfig.rewardScript, videoInfo, rewardConfig)
                if (!scriptResult.success) {
                    return@supplyAsync RewardExecuteResult(
                        false,
                        "奖励脚本执行失败：${scriptResult.message}",
                        error = scriptResult.error
                    )
                }

                // 3. 保存奖励记录
                val saveSuccess = VideoRewardRecordDaoService.saveRecord(rewardRecord).get()
                if (!saveSuccess) {
                    return@supplyAsync RewardExecuteResult(
                        false,
                        "奖励记录保存失败",
                        rewardRecord
                    )
                }

                // 4. 更新玩家奖励统计
                val statsSuccess = PlayerRewardStatsDaoService.incrementPlayerReward(playerUuid, currentTime).get()
                if (!statsSuccess) {
                    console().sendWarn("rewardStatsUpdateFailed", playerUuid)
                }

                // 5. 向玩家发送成功消息
                player.sendInfo("rewardClaimSuccess", videoInfo.bvid)

                RewardExecuteResult(true, "奖励发放成功", rewardRecord)

            } catch (e: Exception) {
                val errorMsg = "奖励执行过程中发生错误：${e.message}"
                console().sendWarn("rewardExecuteError", player.name, videoInfo.bvid, e.message ?: "Unknown error")
                RewardExecuteResult(false, errorMsg, error = e)
            }
        }
    }

    /**
     * 批量执行奖励发放
     * 
     * @param player 玩家对象
     * @param rewardData 奖励数据列表 (视频信息 to 奖励配置)
     * @return 执行结果列表
     */
    fun executeBatchRewards(
        player: ProxyPlayer,
        rewardData: List<Pair<VideoInfo, RewardConfig>>
    ): CompletableFuture<List<RewardExecuteResult>> {
        return CompletableFuture.supplyAsync {
            val results = mutableListOf<RewardExecuteResult>()
            
            rewardData.forEach { (videoInfo, rewardConfig) ->
                try {
                    val result = executeReward(player, videoInfo, rewardConfig).get()
                    results.add(result)
                    
                    // 短暂延迟以避免过快执行
                    Thread.sleep(100)
                } catch (e: Exception) {
                    results.add(RewardExecuteResult(
                        false,
                        "批量执行失败：${e.message}",
                        error = e
                    ))
                }
            }
            
            results
        }
    }

    /**
     * 执行Kether脚本
     */
    private data class KetherExecuteResult(
        val success: Boolean,
        val message: String,
        val error: Throwable? = null
    )

    private fun executeKetherScript(
        player: ProxyPlayer,
        script: String,
        videoInfo: VideoInfo,
        rewardConfig: RewardConfig
    ): KetherExecuteResult {
        return try {
            // 创建Kether脚本执行器
            val ketherShell = KetherShell.eval(script) {
                // 注入变量到脚本上下文
                sender = player
                
                // 玩家相关变量
                set("player", player)
                set("playerName", player.name)
                set("playerUuid", player.uniqueId.toString())
                
                // 视频相关变量
                set("videoTitle", videoInfo.title)
                set("videoBvid", videoInfo.bvid)
                set("videoAid", videoInfo.aid)
                set("videoDescription", videoInfo.description ?: "")
                set("videoCover", videoInfo.cover ?: "")
                set("videoPublishTime", videoInfo.publishTime ?: 0L)
                set("videoDuration", videoInfo.duration ?: 0)
                
                // UP主相关变量
                set("uploaderUid", videoInfo.uploader.uid)
                set("uploaderName", videoInfo.uploader.name)
                set("uploaderAvatar", videoInfo.uploader.avatar ?: "")
                
                // 视频统计相关变量
                videoInfo.stats?.let { stats ->
                    set("videoView", stats.view)
                    set("videoLike", stats.like)
                    set("videoCoin", stats.coin)
                    set("videoFavorite", stats.favorite)
                    set("videoShare", stats.share)
                    set("videoDanmaku", stats.danmaku)
                    set("videoReply", stats.reply)
                }
                
                // 奖励配置相关变量
                set("rewardConfigId", rewardConfig.id)
                set("rewardEnabled", rewardConfig.isEnabled)
                set("rewardMinAge", rewardConfig.minVideoAgeDays)
                set("rewardMaxAge", rewardConfig.maxVideoAgeDays)
                
                // 时间相关变量
                set("currentTime", System.currentTimeMillis())
                set("currentDate", java.time.LocalDate.now().toString())
            }

            // 同步执行脚本
            val result = ketherShell.get()
            
            KetherExecuteResult(true, "脚本执行成功: $result")
            
        } catch (e: Exception) {
            // 打印Kether错误信息到控制台
            console().printKetherErrorMessage(e)
            
            val errorMsg = "Kether脚本执行失败: ${e.message}"
            console().sendWarn("rewardKetherExecuteError", player.name, script, e.message ?: "Unknown error")
            
            KetherExecuteResult(false, errorMsg, e)
        }
    }

    /**
     * 验证Kether脚本语法
     * 
     * @param script Kether脚本内容
     * @return 验证结果
     */
    fun validateKetherScript(script: String): CompletableFuture<KetherExecuteResult> {
        return CompletableFuture.supplyAsync {
            try {
                // 尝试编译脚本以检查语法
                KetherShell.eval(script) {
                    // 设置一些默认变量用于语法检查
                    set("player", null)
                    set("playerName", "test")
                    set("videoTitle", "test")
                }
                
                KetherExecuteResult(true, "脚本语法验证通过")
            } catch (e: Exception) {
                val errorMsg = "脚本语法错误: ${e.message}"
                KetherExecuteResult(false, errorMsg, e)
            }
        }
    }

    /**
     * 获取默认奖励脚本
     */
    fun getDefaultRewardScript(): String {
        return """
            tell player "恭喜您获得视频「&e{{ videoTitle }}&r」的三连奖励！"
            tell player "UP主：&b{{ uploaderName }}&r"
            tell player "视频BV号：&6{{ videoBvid }}&r"
        """.trimIndent()
    }

    /**
     * 获取示例奖励脚本
     */
    fun getExampleRewardScripts(): Map<String, String> {
        return mapOf(
            "基础奖励" to """
                tell player "感谢您对UP主「&b{{ uploaderName }}&r」的支持！"
                tell player "获得视频「&e{{ videoTitle }}&r」的三连奖励"
            """.trimIndent(),
            
            "经济奖励" to """
                tell player "恭喜获得三连奖励！"
                tell player "奖励：&61000金币&r"
                # 这里可以添加给予金币的命令，需要根据实际的经济插件调整
                # 例如：command console "eco give {{ playerName }} 1000"
            """.trimIndent(),
            
            "物品奖励" to """
                tell player "恭喜获得视频「&e{{ videoTitle }}&r」的三连奖励！"
                tell player "奖励：&d钻石 x3&r"
                # 给予物品示例
                # item give diamond 3
            """.trimIndent(),
            
            "经验奖励" to """
                tell player "恭喜获得UP主「&b{{ uploaderName }}&r」的三连奖励！"
                tell player "奖励：&a100经验&r"
                # 给予经验示例
                # exp add 100
            """.trimIndent()
        )
    }
}