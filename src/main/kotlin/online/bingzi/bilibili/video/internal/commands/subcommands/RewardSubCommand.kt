package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.rewards.TripleRewardService
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo

/**
 * 奖励领取子命令
 * 处理玩家的奖励领取相关操作
 */
@CommandHeader(
    name = "reward",
    description = "三连奖励系统命令",
    permission = "bilibilivideo.command.reward"
)
object RewardSubCommand {

    /**
     * 主奖励命令 - 显示帮助信息
     */
    @CommandBody
    val main = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("rewardCommandHelp")
        }
    }

    /**
     * 领取奖励命令
     * /bilibili reward claim <BV号>
     */
    @CommandBody
    val claim = subCommand {
        literal("claim")
        dynamic("bvid") {
            suggestion<ProxyPlayer> { _, _ ->
                // 这里可以提供BV号的建议，暂时返回空列表
                emptyList()
            }
            execute<ProxyPlayer> { player, _, argument ->
                val bvId = argument
                
                if (bvId.isBlank()) {
                    player.sendError("rewardBvIdRequired")
                    return@execute
                }

                // 验证BV号格式
                if (!isValidBvId(bvId)) {
                    player.sendError("rewardInvalidBvId", bvId)
                    return@execute
                }

                player.sendInfo("rewardClaimProcessing", bvId)

                // 异步处理奖励领取
                TripleRewardService.claimReward(player, bvId).thenAccept { result ->
                    if (result.success) {
                        player.sendInfo("rewardClaimSuccess", bvId)
                    } else {
                        player.sendError("rewardClaimFailed", result.message)
                    }
                }.exceptionally { throwable ->
                    player.sendError("rewardClaimError", throwable.message ?: "未知错误")
                    null
                }
            }
        }
    }

    /**
     * 查看可领取奖励列表
     * /bilibili reward list
     */
    @CommandBody
    val list = subCommand {
        literal("list")
        execute<ProxyPlayer> { player, _, _ ->
            player.sendInfo("rewardListLoading")

            TripleRewardService.getAvailableRewards(player, 10).thenAccept { rewards ->
                if (rewards.isEmpty()) {
                    player.sendInfo("rewardListEmpty")
                } else {
                    player.sendInfo("rewardListHeader")
                    rewards.forEachIndexed { index, reward ->
                        player.sendInfo("rewardListItem", 
                            index + 1,
                            reward.videoTitle,
                            reward.uploaderName,
                            reward.bvId
                        )
                    }
                    player.sendInfo("rewardListFooter", rewards.size)
                }
            }.exceptionally { throwable ->
                player.sendError("rewardListError", throwable.message ?: "未知错误")
                null
            }
        }
    }

    /**
     * 查看奖励统计
     * /bilibili reward stats
     */
    @CommandBody
    val stats = subCommand {
        literal("stats")
        execute<ProxyPlayer> { player, _, _ ->
            TripleRewardService.getPlayerRewardStats(player).thenAccept { stats ->
                player.sendInfo("rewardStatsHeader")
                player.sendInfo("rewardStatsDaily", stats.dailyCount, stats.dailyLimit)
                player.sendInfo("rewardStatsRemaining", stats.remaining)
                player.sendInfo("rewardStatsTotal", stats.totalCount)
            }.exceptionally { throwable ->
                player.sendError("rewardStatsError", throwable.message ?: "未知错误")
                null
            }
        }
    }

    /**
     * 验证BV号格式
     */
    private fun isValidBvId(bvId: String): Boolean {
        // BV号格式：BV + 10位字符（数字和大小写字母）
        val bvPattern = Regex("^BV[a-zA-Z0-9]{10}$")
        return bvPattern.matches(bvId)
    }
}