package online.bingzi.bilibili.video.internal.commands

import online.bingzi.bilibili.video.internal.commands.subcommands.InfoSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.RewardSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.RewardAdminSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.LoginSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.BindSubCommand
import online.bingzi.bilibili.video.internal.commands.subcommands.UploaderSubCommand
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
    
    /**
     * 登录相关子命令
     */
    @CommandBody
    val login = LoginSubCommand.login
    
    @CommandBody
    val loginCancel = LoginSubCommand.cancel
    
    @CommandBody
    val loginStatus = LoginSubCommand.status
    
    @CommandBody
    val loginLogout = LoginSubCommand.logout
    
    /**
     * 绑定相关子命令
     */
    @CommandBody
    val bind = BindSubCommand.main
    
    @CommandBody
    val bindBilibili = BindSubCommand.bilibili
    
    @CommandBody
    val bindBilibiliUnbind = BindSubCommand.bilibiliUnbind
    
    @CommandBody
    val bindBilibiliInfo = BindSubCommand.bilibiliInfo
    
    @CommandBody
    val bindBilibiliRefresh = BindSubCommand.bilibiliRefresh
    
    @CommandBody
    val bindQq = BindSubCommand.qq
    
    @CommandBody
    val bindQqUnbind = BindSubCommand.qqUnbind
    
    @CommandBody
    val bindQqInfo = BindSubCommand.qqInfo
    
    @CommandBody(permission = "bilibilivideo.admin.bind")
    val bindAdminUnbind = BindSubCommand.adminUnbind
    
    @CommandBody(permission = "bilibilivideo.admin.bind")
    val bindAdminList = BindSubCommand.adminList
    
    /**
     * UP主监控相关子命令
     */
    @CommandBody
    val uploader = UploaderSubCommand.main
    
    @CommandBody
    val uploaderAdd = UploaderSubCommand.add
    
    @CommandBody
    val uploaderRemove = UploaderSubCommand.remove
    
    @CommandBody
    val uploaderList = UploaderSubCommand.list
    
    @CommandBody
    val uploaderSync = UploaderSubCommand.sync
    
    @CommandBody
    val uploaderSyncAll = UploaderSubCommand.syncAll
    
    @CommandBody
    val uploaderToggle = UploaderSubCommand.toggle
    
    @CommandBody
    val uploaderStatus = UploaderSubCommand.status
    
    @CommandBody
    val uploaderSearch = UploaderSubCommand.search
}