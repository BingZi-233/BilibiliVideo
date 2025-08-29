package online.bingzi.bilibili.bilibilivideo.internal.command

import online.bingzi.bilibili.bilibilivideo.internal.command.handler.FollowStatusCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LoginCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LogoutCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.TripleStatusCommandHandler
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper
import taboolib.module.lang.sendError

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
        execute<ProxyCommandSender> { sender, _, _ ->
            createHelper()
        }
    }

    @CommandBody
    val login = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            if (sender !is ProxyPlayer) {
                sender.sendError("commonPlayerOnly")
                return@execute
            }
            
            val player = sender.cast<Player>()
            LoginCommandHandler.handleLogin(player)
        }
    }

    @CommandBody
    val logout = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            if (sender !is ProxyPlayer) {
                sender.sendError("commonPlayerOnly")
                return@execute
            }
            
            val player = sender.cast<Player>()
            LogoutCommandHandler.handleLogout(player)
        }
    }

    @CommandBody
    val triple = subCommand {
        dynamic("bvid") {
            restrict<ProxyCommandSender> { sender, _, argument ->
                if (!isValidBvid(argument)) {
                    sender.sendError("commandsTripleInvalidBvid")
                    false
                } else {
                    true
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("commonPlayerOnly")
                    return@execute
                }
                
                val bvid = argument
                val player = sender.cast<Player>()
                TripleStatusCommandHandler.handleTripleStatus(player, bvid)
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
            
            execute<ProxyCommandSender> { sender, _, argument ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("commonPlayerOnly")
                    return@execute
                }
                
                val midStr = argument
                val mid = midStr.toLongOrNull()!! // restrict已经验证过格式
                val player = sender.cast<Player>()
                FollowStatusCommandHandler.handleFollowStatus(player, mid)
            }
        }
    }
    
    // 辅助验证方法
    private fun isValidBvid(bvid: String): Boolean {
        // BV号格式：BV + 10位大小写字母和数字（不包含0、I、O、l）
        return bvid.matches(Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$"))
    }
    
    private fun isValidMid(midStr: String): Boolean {
        val mid = midStr.toLongOrNull()
        return mid != null && mid > 0 && midStr.length >= 6 && midStr.length <= 12
    }
}