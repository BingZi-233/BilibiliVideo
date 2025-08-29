package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.api.qrcode.registry.QRCodeSenderRegistry
import online.bingzi.bilibili.bilibilivideo.api.qrcode.options.SendOptions
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.api.QrCodeApi
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.LoginStatus
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.QrCodePollData
import online.bingzi.bilibili.bilibilivideo.internal.database.service.DatabaseService
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitTask
import taboolib.module.lang.*
import taboolib.platform.util.sendError
import taboolib.platform.util.sendInfo
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
                if (senders.isNotEmpty()) {
                    val sender = senders.first()
                    val options = SendOptions()
                    
                    sender.sendAsync(player, qrData.url, options) { result ->
                        if (result.isSuccess) {
                            player.sendInfo("commandsLoginQrCodeSent")
                            startPolling(player, qrData.qrcodeKey)
                        } else {
                            player.sendError("commandsLoginQrCodeSendFailed", result.errorMessage)
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
        
        QrCodeApi.pollQrCodeStatus(task.qrcodeKey) { status, pollData ->
            when (status) {
                LoginStatus.SUCCESS -> {
                    if (pollData != null) {
                        handleLoginSuccess(task.player, pollData)
                    } else {
                        task.player.sendError("commandsLoginFailedNoUserInfo")
                    }
                    loginTasks.remove(task.player.uniqueId.toString())
                }
                
                LoginStatus.SCANNED_WAITING -> {
                    task.player.sendInfo("commandsLoginQrCodeScanned")
                    // 继续轮询
                    submitTask(delay = 40L) { // 2秒后再次轮询
                        pollQrCodeStatus(task)
                    }
                }
                
                LoginStatus.NOT_SCANNED -> {
                    // 继续轮询
                    submitTask(delay = 60L) { // 3秒后再次轮询
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
    
    private fun handleLoginSuccess(player: Player, pollData: QrCodePollData) {
        player.sendInfo("commandsLoginSuccess")
        
        // 这里需要从response中提取Cookie信息
        // 由于OkHttp的限制，我们需要通过其他方式获取Cookie
        // 暂时使用模拟数据，实际实现时需要正确提取Cookie
        val mid = extractMidFromUrl(pollData.url)
        if (mid != null) {
            // 从数据库加载已有的账户信息或创建新的
            DatabaseService.getBilibiliAccount(mid) { account ->
                if (account != null) {
                    // 创建会话
                    SessionManager.createSession(
                        player = player,
                        mid = account.mid,
                        nickname = account.nickname,
                        sessdata = account.sessdata,
                        buvid3 = account.buvid3,
                        biliJct = account.biliJct,
                        refreshToken = account.refreshToken
                    )
                    
                    player.sendInfo("commandsLoginSuccessWelcome", account.nickname)
                } else {
                    player.sendError("commandsLoginFailedIncompleteInfo")
                }
            }
        } else {
            player.sendError("commandsLoginFailedNoUserId")
        }
    }
    
    private fun extractMidFromUrl(url: String?): Long? {
        // 从登录成功的URL中提取MID
        // 这是一个简化实现，实际需要根据具体的URL格式解析
        return try {
            url?.let {
                // TODO: 实际的URL解析逻辑
                null
            }
        } catch (e: Exception) {
            null
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