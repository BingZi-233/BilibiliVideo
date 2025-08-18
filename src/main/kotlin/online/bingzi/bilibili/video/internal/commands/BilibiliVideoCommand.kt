package online.bingzi.bilibili.video.internal.commands

import online.bingzi.bilibili.video.internal.commands.subcommands.InfoSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.RewardSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.RewardAdminSubCommand
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.module.lang.sendInfo

/**
 * BilibiliVideo插件主命令
 * 统一的命令入口，包含所有功能模块的子命令
 */
@CommandHeader(
    name = "bilibilivideo",
    aliases = ["bv", "bili"],
    description = "BilibiliVideo插件主命令",
    permission = "bilibilivideo.command.use"
)
object BilibiliVideoCommand {
    
    /**
     * 主命令，显示帮助信息
     */
    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("bilibiliVideoCommandHelp")
        }
    }
    
    /**
     * 信息查看子命令
     */
    @CommandBody
    val info = InfoSubCommand.info
    
    /**
     * 奖励相关子命令
     */
    @CommandBody
    val reward = RewardSubCommand.main
    
    @CommandBody
    val rewardClaim = RewardSubCommand.claim
    
    @CommandBody
    val rewardList = RewardSubCommand.list
    
    @CommandBody
    val rewardStats = RewardSubCommand.stats
    
    /**
     * 奖励管理员子命令
     */
    @CommandBody
    val rewardAdmin = RewardAdminSubCommand.main
    
    @CommandBody
    val rewardAdminAdd = RewardAdminSubCommand.add
    
    @CommandBody
    val rewardAdminRemove = RewardAdminSubCommand.remove
    
    @CommandBody
    val rewardAdminList = RewardAdminSubCommand.list
    
    @CommandBody
    val rewardAdminToggle = RewardAdminSubCommand.toggle
    
    @CommandBody
    val rewardAdminStatus = RewardAdminSubCommand.status
    
    @CommandBody
    val rewardAdminExamples = RewardAdminSubCommand.examples
}