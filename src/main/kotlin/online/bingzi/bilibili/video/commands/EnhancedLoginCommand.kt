package online.bingzi.bilibili.video.commands

import online.bingzi.bilibili.video.internal.network.EnhancedLoginService
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 增强的Bilibili登录命令
 * 支持二维码发送功能
 */
@CommandHeader(
    name = "blogin",
    description = "Bilibili登录命令",
    permission = "bilibilivideo.login"
)
object EnhancedLoginCommand {

    @CommandBody
    val main = mainCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            // 默认使用聊天框模式
            startLogin(sender, QRCodeSendMode.CHAT)
        }
    }

    @CommandBody
    val chat = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            startLogin(sender, QRCodeSendMode.CHAT)
        }
    }

    @CommandBody
    val map = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            startLogin(sender, QRCodeSendMode.MAP)
        }
    }

    @CommandBody
    val qq = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            startLogin(sender, QRCodeSendMode.ONEBOT)
        }
    }

    @CommandBody
    val cancel = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            if (EnhancedLoginService.cancelLogin(sender)) {
                sender.sendInfo("loginCancelled")
            } else {
                sender.sendWarn("loginNothingToCancel")
            }
        }
    }

    @CommandBody
    val status = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            val session = EnhancedLoginService.getLoginSession(sender)
            if (session != null) {
                val elapsedTime = (System.currentTimeMillis() - session.startTime) / 1000
                sender.sendInfo("loginStatusActive", elapsedTime.toString())
            } else {
                sender.sendInfo("loginStatusInactive")
            }
        }
    }

    @CommandBody
    val modes = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            val availableModes = QRCodeSendService.getAvailableModes(sender)
            if (availableModes.isEmpty()) {
                sender.sendWarn("loginNoAvailableModes")
            } else {
                val modeList = availableModes.map { 
                    "${it.displayName} (${it.name.lowercase()})" 
                }.joinToString(", ")
                sender.sendInfo("loginAvailableModes", modeList)
            }
        }
    }

    /**
     * 启动登录流程
     */
    private fun startLogin(player: ProxyPlayer, mode: QRCodeSendMode) {
        // 检查是否有正在进行的登录
        val existingSession = EnhancedLoginService.getLoginSession(player)
        if (existingSession != null) {
            val elapsedTime = (System.currentTimeMillis() - existingSession.startTime) / 1000
            player.sendWarn("loginAlreadyInProgress", elapsedTime.toString())
            return
        }

        // 检查模式是否可用
        if (!QRCodeSendService.isModeAvailable(mode, player)) {
            player.sendError("loginModeUnavailable", mode.displayName)
            
            // 推荐可用的模式
            val availableModes = QRCodeSendService.getAvailableModes(player)
            if (availableModes.isNotEmpty()) {
                val suggestion = availableModes.first()
                player.sendInfo("loginModeSuggestion", suggestion.displayName, suggestion.name.lowercase())
            }
            return
        }

        player.sendInfo("loginStarting", mode.displayName)
        
        EnhancedLoginService.startLoginFlow(player, mode).thenAccept { success ->
            if (!success) {
                player.sendError("loginStartFailed")
            }
        }
    }
}