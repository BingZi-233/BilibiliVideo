package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.api.qrcode.options.SendOptions
import online.bingzi.bilibili.bilibilivideo.api.qrcode.registry.QRCodeSenderRegistry
import online.bingzi.bilibili.bilibilivideo.api.qrcode.result.SendResult
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.api.QrCodeApi
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.LoginStatus
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.QrCodePollData
import online.bingzi.bilibili.bilibilivideo.internal.database.service.DatabaseService
import online.bingzi.bilibili.bilibilivideo.internal.event.EventManager
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import taboolib.platform.util.sendError
import taboolib.platform.util.sendInfo
import taboolib.platform.util.sendWarn
import java.util.concurrent.ConcurrentHashMap

object LoginCommandHandler {
    
    private val loginTasks = ConcurrentHashMap<String, LoginTask>()
    
    fun handleLogin(player: Player) {
        // 检查玩家是否已经登录
        if (SessionManager.isPlayerLoggedIn(player)) {
            player.sendError("commandsLoginAlreadyLoggedIn")
            return
        }
        
        // 检查是否已经在登录过程中
        if (loginTasks.containsKey(player.uniqueId.toString())) {
            player.sendError("commandsLoginInProgress")
            return
        }
        
        player.sendInfo("commandsLoginGeneratingQrCode")
        
        // 生成二维码
        QrCodeApi.generateQrCode { qrData ->
            if (qrData != null) {
                // 发送二维码给玩家
                val senders = QRCodeSenderRegistry.getAvailableSenders()
                val active = QRCodeSenderRegistry.getActiveSender()
                if (active != null || senders.isNotEmpty()) {
                    val sender = active ?: senders.values.first()
                    val options = SendOptions()

                    sender.sendAsync(player, qrData.url, options) { result ->
                        when (result) {
                            is SendResult.Success -> {
                                player.sendInfo("commandsLoginQrCodeSent")
                                startPolling(player, qrData.qrcodeKey)
                            }
                            is SendResult.Failure -> {
                                player.sendError("commandsLoginQrCodeSendFailed", result.reason)
                            }
                            is SendResult.Partial -> {
                                player.sendWarn("commandsLoginQrCodePartialSuccess", result.details)
                            }
                        }
                    }
                } else {
                    player.sendError("commandsLoginNoQrCodeSender")
                }
            } else {
                player.sendError("commandsLoginQrCodeGenerateFailed")
            }
        }
    }
    
    private fun startPolling(player: Player, qrcodeKey: String) {
        val task = LoginTask(player, qrcodeKey)
        loginTasks[player.uniqueId.toString()] = task
        
        // 开始轮询
        pollQrCodeStatus(task)
    }
    
    private fun pollQrCodeStatus(task: LoginTask) {
        if (task.isExpired() || !task.player.isOnline) {
            loginTasks.remove(task.player.uniqueId.toString())
            task.player.sendError("commandsLoginTimeout")
            return
        }
        
        QrCodeApi.pollQrCodeStatus(task.qrcodeKey) { status, pollData, loginInfo ->
            when (status) {
                LoginStatus.SUCCESS -> {
                    if (pollData != null && loginInfo != null) {
                        handleLoginSuccess(task.player, pollData, loginInfo)
                    } else {
                        task.player.sendError("commandsLoginFailedNoUserInfo")
                    }
                    loginTasks.remove(task.player.uniqueId.toString())
                }
                
                LoginStatus.SCANNED_WAITING -> {
                    task.player.sendInfo("commandsLoginQrCodeScanned")
                    // 继续轮询（2秒后）
                    submit(delay = 40L) {
                        pollQrCodeStatus(task)
                    }
                }
                
                LoginStatus.NOT_SCANNED -> {
                    // 继续轮询（3秒后）
                    submit(delay = 60L) {
                        pollQrCodeStatus(task)
                    }
                }
                
                LoginStatus.EXPIRED -> {
                    task.player.sendError("commandsLoginQrCodeExpired")
                    loginTasks.remove(task.player.uniqueId.toString())
                }
            }
        }
    }
    
    private fun handleLoginSuccess(player: Player, pollData: QrCodePollData, loginInfo: QrCodeApi.LoginInfo) {
        player.sendInfo("commandsLoginSuccess")
        
        // 使用从Cookie中提取的信息直接保存账户
        DatabaseService.saveBilibiliAccount(
            mid = loginInfo.mid,
            nickname = "", // 暂时为空，后续可以通过API获取
            sessdata = loginInfo.sessdata,
            buvid3 = loginInfo.buvid3,
            biliJct = loginInfo.biliJct,
            refreshToken = loginInfo.refreshToken,
            playerName = player.name
        ) { success ->
            if (success) {
                // 绑定玩家账户
                DatabaseService.bindPlayer(
                    playerUuid = player.uniqueId.toString(),
                    mid = loginInfo.mid,
                    playerName = player.name
                ) { bindSuccess ->
                    if (bindSuccess) {
                        // 创建会话
                        val session = SessionManager.createSession(
                            player = player,
                            mid = loginInfo.mid,
                            nickname = "用户${loginInfo.mid}", // 临时昵称
                            sessdata = loginInfo.sessdata,
                            buvid3 = loginInfo.buvid3,
                            biliJct = loginInfo.biliJct,
                            refreshToken = loginInfo.refreshToken
                        )
                        
                        // 触发BilibiliLoginEvent事件
                        EventManager.callBilibiliLoginEvent(player, session)
                        
                        player.sendInfo("commandsLoginSuccessWelcome", "用户${loginInfo.mid}")
                    } else {
                        player.sendError("commandsLoginFailedIncompleteInfo")
                    }
                }
            } else {
                player.sendError("commandsLoginFailedIncompleteInfo")
            }
        }
    }
    
    fun cancelLogin(player: Player) {
        loginTasks.remove(player.uniqueId.toString())?.let {
            player.sendInfo("commandsLoginCancelled")
        }
    }
    
    private data class LoginTask(
        val player: Player,
        val qrcodeKey: String,
        val startTime: Long = System.currentTimeMillis()
    ) {
        companion object {
            const val TIMEOUT_MILLIS = 3 * 60 * 1000L // 3分钟超时
        }
        
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - startTime > TIMEOUT_MILLIS
        }
    }
}
