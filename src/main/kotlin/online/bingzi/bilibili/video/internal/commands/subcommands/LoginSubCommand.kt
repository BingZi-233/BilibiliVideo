package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.network.BilibiliNetworkManager
import online.bingzi.bilibili.video.internal.network.EnhancedLoginService
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 登录相关子命令
 * 处理 Bilibili 账户登录功能
 */
object LoginSubCommand {
    
    /**
     * 登录主命令 - /bilibilivideo login [mode]
     */
    val login = subCommand {
        // 可选的登录模式参数
        dynamic("mode", optional = true) {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取可用的二维码发送模式
                QRCodeSendService.getRegisteredSenderNames().toList()
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("loginOnlyPlayer")
                    return@execute
                }
                
                val mode = argument
                val playerUuid = sender.uniqueId
                
                // 检查指定模式是否可用
                if (!QRCodeSendService.getRegisteredSenderNames().contains(mode)) {
                    sender.sendError("loginModeUnavailable", mode)
                    val availableModes = QRCodeSendService.getRegisteredSenderNames()
                    if (availableModes.isNotEmpty()) {
                        sender.sendInfo("loginAvailableModes", availableModes.joinToString(", "))
                    }
                    return@execute
                }
                
                performLogin(sender, playerUuid.toString(), mode)
            }
        }
        
        // 无参数时使用默认模式
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("loginOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            val availableModes = QRCodeSendService.getRegisteredSenderNames().toList()
            
            if (availableModes.isEmpty()) {
                sender.sendError("loginNoAvailableModes")
                return@execute
            }
            
            // 使用第一个可用模式
            val defaultMode = availableModes.first()
            performLogin(sender, playerUuid.toString(), defaultMode)
        }
    }
    
    /**
     * 取消登录命令 - /bilibilivideo login cancel
     */
    val cancel = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("loginOnlyPlayer")
                return@execute
            }
            
            submit(async = true) {
                if (EnhancedLoginService.cancelLogin(sender)) {
                    sender.sendInfo("loginCancelled")
                } else {
                    sender.sendWarn("loginNothingToCancel")
                }
            }
        }
    }
    
    /**
     * 登录状态命令 - /bilibilivideo login status
     */
    val status = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("loginOnlyPlayer")
                return@execute
            }
            
            submit(async = true) {
                val loginSession = EnhancedLoginService.getLoginSession(sender)
                if (loginSession != null) {
                    val duration = (System.currentTimeMillis() - loginSession.startTime) / 1000
                    sender.sendInfo("loginStatusActive", duration.toString())
                } else {
                    sender.sendInfo("loginStatusInactive")
                }
            }
        }
    }
    
    /**
     * 登出命令 - /bilibilivideo login logout
     */
    val logout = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("loginOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId.toString()
            val networkManager = BilibiliNetworkManager.getPlayerService(playerUuid)
            
            submit(async = true) {
                networkManager.logout()
                sender.sendInfo("loginLogout")
            }
        }
    }
    
    /**
     * 执行登录逻辑
     */
    private fun performLogin(sender: ProxyPlayer, playerUuid: String, mode: String) {
        val networkManager = BilibiliNetworkManager.getPlayerService(playerUuid)
        
        submit(async = true) {
            // 检查是否已经登录
            if (networkManager.isLoggedIn()) {
                val userInfo = networkManager.getCurrentUserInfo().join()
                if (userInfo != null) {
                    sender.sendInfo("loginAlreadyLoggedIn", userInfo.uid.toString())
                    return@submit
                }
            }
            
            // 检查是否已有登录进程在运行
            val existingSession = EnhancedLoginService.getLoginSession(sender)
            if (existingSession != null) {
                val duration = (System.currentTimeMillis() - existingSession.startTime) / 1000
                sender.sendError("loginAlreadyInProgress", duration.toString())
                return@submit
            }
            
            try {
                sender.sendInfo("loginStarting", mode)
                
                // 启动登录流程
                EnhancedLoginService.startLoginFlow(sender, mode).thenAccept { success ->
                    if (!success) {
                        sender.sendError("loginStartFailed")
                    }
                }.exceptionally { throwable ->
                    sender.sendError("loginStartFailed")
                    null
                }
            } catch (e: Exception) {
                sender.sendError("loginStartFailed")
            }
        }
    }
}