package online.bingzi.bilibili.bilibilivideo.internal.command

import online.bingzi.bilibili.bilibilivideo.internal.command.handler.FollowStatusCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LoginCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LogoutCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.TripleStatusCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.manager.BvManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo

/**
 * Bilibili插件主命令类
 * 
 * 使用TabooLib CommandHelper模块实现的声明式命令系统。
 * 提供玩家Bilibili账户管理和状态查询功能。
 * 支持权限管理和多级子命令结构。
 * 
 * 主要命令：
 * - /bili login [玩家] - 登录Bilibili账户
 * - /bili logout [玩家] - 登出Bilibili账户
 * - /bili triple <BV号> [玩家] - 查询视频三连状态
 * - /bili follow <MID> [玩家] - 查询UP主关注状态
 * 
 * 权限说明：
 * - bilibili.use: 基础使用权限（默认true）
 * - bilibili.admin: 管理员权限，可操作其他玩家
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
@CommandHeader(
    name = "bili",
    aliases = ["bilibili", "bl"],
    description = "Bilibili相关命令",
    permission = "bilibili.use",
    permissionDefault = PermissionDefault.TRUE
)
object BilibiliCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val login = subCommand {
        dynamic("player", optional = true) {
            suggestion<ProxyCommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val targetPlayer = resolveTargetPlayer(sender, argument) ?: return@execute
                executeForPlayer(sender, targetPlayer, argument) {
                    LoginCommandHandler.handleLogin(it)
                }
            }
        }
    }

    @CommandBody
    val logout = subCommand {
        dynamic("player", optional = true) {
            suggestion<ProxyCommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val targetPlayer = resolveTargetPlayer(sender, argument) ?: return@execute
                executeForPlayer(sender, targetPlayer, argument) {
                    LogoutCommandHandler.handleLogout(it)
                }
            }
        }
    }

    @CommandBody
    val triple = subCommand {
        dynamic("bvid") {
            suggestion<ProxyCommandSender> { _, _ ->
                // 返回配置文件中的BV号列表用于自动补全
                BvManager.getEnabledBvids()
            }
            
            restrict<ProxyCommandSender> { sender, _, argument ->
                if (!isValidBvid(argument)) {
                    sender.sendError("commandsTripleInvalidBvid")
                    false
                } else {
                    true
                }
            }
            
            dynamic("player", optional = true) {
                suggestion<ProxyCommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val bvid = context["bvid"]
                    val targetPlayer = resolveTargetPlayer(sender, argument) ?: return@execute
                    executeForPlayer(sender, targetPlayer, argument) {
                        TripleStatusCommandHandler.handleTripleStatus(it, bvid)
                    }
                }
            }
        }
    }

    @CommandBody
    val follow = subCommand {
        dynamic("mid") {
            restrict<ProxyCommandSender> { sender, _, argument ->
                if (!isValidMid(argument)) {
                    sender.sendError("commandsFollowInvalidMid")
                    false
                } else {
                    true
                }
            }
            
            dynamic("player", optional = true) {
                suggestion<ProxyCommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val midStr = context["mid"]
                    val mid = midStr.toLongOrNull()!! // restrict已经验证过格式
                    val targetPlayer = resolveTargetPlayer(sender, argument) ?: return@execute
                    executeForPlayer(sender, targetPlayer, argument) {
                        FollowStatusCommandHandler.handleFollowStatus(it, mid)
                    }
                }
            }
        }
    }
    
    // 辅助方法
    private fun resolveTargetPlayer(sender: ProxyCommandSender, playerName: String?): Player? {
        return if (playerName != null) {
            // 指定了玩家，需要admin权限
            if (!sender.hasPermission("bilibili.admin")) {
                sender.sendError("noPermissionForOthers")
                return null
            }
            Bukkit.getPlayer(playerName) ?: run {
                sender.sendError("playerNotFound", playerName)
                null
            }
        } else {
            // 未指定玩家，使用执行者
            if (sender !is ProxyPlayer) {
                sender.sendError("commonPlayerOnly")
                return null
            }
            sender.cast<Player>()
        }
    }
    
    private fun executeForPlayer(
        sender: ProxyCommandSender,
        targetPlayer: Player,
        playerName: String?,
        action: (Player) -> Unit
    ) {
        action(targetPlayer)
        
        // 如果是为他人执行，给管理员反馈
        if (playerName != null && sender is ProxyPlayer && sender.cast<Player>() != targetPlayer) {
            sender.sendInfo("executedForPlayer", targetPlayer.name)
        }
    }
    
    private fun isValidBvid(bvid: String): Boolean {
        // BV号格式：BV + 10位大小写字母和数字（不包含0、I、O、l）
        return bvid.matches(Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$"))
    }
    
    private fun isValidMid(midStr: String): Boolean {
        val mid = midStr.toLongOrNull()
        return mid != null && mid > 0 && midStr.length >= 6 && midStr.length <= 12
    }
}
