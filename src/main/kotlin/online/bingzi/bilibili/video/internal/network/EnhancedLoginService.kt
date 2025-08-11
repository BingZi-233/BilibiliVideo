package online.bingzi.bilibili.video.internal.network

import online.bingzi.bilibili.video.internal.network.entity.LoginSession
import online.bingzi.bilibili.video.internal.network.entity.LoginStatus
import online.bingzi.bilibili.video.internal.qrcode.QRCodeGenerator
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 增强的Bilibili登录服务
 * 集成二维码生成和发送功能
 */
object EnhancedLoginService {
    
    // 存储每个玩家的登录会话
    private val loginSessions = ConcurrentHashMap<String, LoginSession>()
    
    /**
     * 为玩家启动登录流程
     * @param player 目标玩家
     * @param sendMode 二维码发送模式
     * @return 是否成功启动登录流程
     */
    fun startLoginFlow(
        player: ProxyPlayer,
        sendMode: QRCodeSendMode = QRCodeSendMode.CHAT
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 设置当前玩家上下文
                BilibiliCookieJar.setCurrentPlayer(player.uniqueId.toString())
                
                // 检查是否已经登录
                if (BilibiliCookieJar.isLoggedIn()) {
                    player.sendInfo("loginAlreadyLoggedIn", BilibiliCookieJar.getUserId() ?: "未知")
                    return@supplyAsync true
                }
                
                // 生成二维码登录信息
                val qrCodeInfo = BilibiliLoginService.generateQrCode().get()
                if (qrCodeInfo == null) {
                    player.sendWarn("loginQrCodeGenerateFailed", "获取登录二维码失败")
                    return@supplyAsync false
                }
                
                // 生成二维码图片
                val qrCodeSize = QRCodeGenerator.getRecommendedSize(qrCodeInfo.url.length)
                val qrCodeImage = QRCodeGenerator.generateQRCode(qrCodeInfo.url, qrCodeSize)
                if (qrCodeImage == null) {
                    player.sendWarn("qrcodeGenerateFailed", "生成二维码图片失败")
                    return@supplyAsync false
                }
                
                // 发送二维码给玩家
                val sendSuccess = QRCodeSendService.sendQRCode(
                    player = player,
                    qrCodeImage = qrCodeImage,
                    title = "Bilibili登录",
                    description = "请使用Bilibili手机APP扫描二维码登录",
                    preferredMode = sendMode
                ).get()
                
                if (!sendSuccess) {
                    player.sendWarn("qrcodeSendAllFailed")
                    return@supplyAsync false
                }
                
                // 创建登录会话
                val session = LoginSession(
                    playerUuid = player.uniqueId.toString(),
                    playerName = player.name,
                    qrcodeKey = qrCodeInfo.qrcodeKey,
                    startTime = System.currentTimeMillis()
                )
                loginSessions[player.uniqueId.toString()] = session
                
                // 启动状态轮询
                startLoginPolling(player, qrCodeInfo.qrcodeKey)
                
                player.sendInfo("loginQrCodeSent", "登录二维码已发送")
                console().sendInfo("loginFlowStarted", player.name, sendMode.displayName)
                
                true
                
            } catch (e: Exception) {
                console().sendWarn("loginFlowFailed", player.name, e.message ?: "")
                false
            }
        }
    }
    
    /**
     * 启动登录状态轮询
     */
    private fun startLoginPolling(player: ProxyPlayer, qrcodeKey: String) {
        CompletableFuture.runAsync {
            val maxPollingTime = 5 * 60 * 1000L // 5分钟超时
            val startTime = System.currentTimeMillis()
            val playerUuid = player.uniqueId.toString()
            
            try {
                while (System.currentTimeMillis() - startTime < maxPollingTime) {
                    // 检查会话是否还存在（玩家可能已经取消）
                    if (!loginSessions.containsKey(playerUuid)) {
                        break
                    }
                    
                    // 设置玩家上下文
                    BilibiliCookieJar.setCurrentPlayer(playerUuid)
                    
                    // 轮询登录状态
                    val status = BilibiliLoginService.pollLoginStatus(qrcodeKey).get()
                    
                    when (status) {
                        LoginStatus.SUCCESS -> {
                            // 登录成功
                            onLoginSuccess(player)
                            return@runAsync
                        }
                        
                        LoginStatus.EXPIRED -> {
                            // 二维码过期
                            onLoginExpired(player)
                            return@runAsync
                        }
                        
                        LoginStatus.FAILED -> {
                            // 登录失败
                            onLoginFailed(player)
                            return@runAsync
                        }
                        
                        LoginStatus.WAITING_FOR_SCAN,
                        LoginStatus.WAITING_FOR_CONFIRM -> {
                            // 继续等待
                            Thread.sleep(3000) // 3秒轮询间隔
                        }
                    }
                }
                
                // 超时处理
                onLoginTimeout(player)
                
            } catch (e: Exception) {
                console().sendWarn("loginPollingFailed", player.name, e.message ?: "")
                onLoginFailed(player)
            }
        }
    }
    
    /**
     * 登录成功处理
     */
    private fun onLoginSuccess(player: ProxyPlayer) {
        try {
            val playerUuid = player.uniqueId.toString()
            loginSessions.remove(playerUuid)
            
            // 为用户预获取 buvid
            BuvidService.ensureBuvid(playerUuid).thenAccept { buvidSuccess ->
                if (buvidSuccess) {
                    console().sendInfo("loginBuvidEnsured", player.name)
                } else {
                    console().sendWarn("loginBuvidFailed", player.name)
                }
            }
            
            val userId = BilibiliCookieJar.getUserId() ?: "未知"
            player.sendInfo("loginSuccessComplete", userId)
            console().sendInfo("loginCompleted", player.name, userId)
            
        } catch (e: Exception) {
            console().sendWarn("loginSuccessHandleFailed", player.name, e.message ?: "")
        }
    }
    
    /**
     * 二维码过期处理
     */
    private fun onLoginExpired(player: ProxyPlayer) {
        loginSessions.remove(player.uniqueId.toString())
        player.sendWarn("loginQrCodeExpiredPlayer")
        console().sendWarn("loginExpiredForPlayer", player.name)
    }
    
    /**
     * 登录失败处理
     */
    private fun onLoginFailed(player: ProxyPlayer) {
        loginSessions.remove(player.uniqueId.toString())
        player.sendWarn("loginFailedPlayer")
        console().sendWarn("loginFailedForPlayer", player.name)
    }
    
    /**
     * 登录超时处理
     */
    private fun onLoginTimeout(player: ProxyPlayer) {
        loginSessions.remove(player.uniqueId.toString())
        player.sendWarn("loginTimeout")
        console().sendWarn("loginTimeoutForPlayer", player.name)
    }
    
    /**
     * 取消玩家的登录流程
     */
    fun cancelLogin(player: ProxyPlayer): Boolean {
        val session = loginSessions.remove(player.uniqueId.toString())
        if (session != null) {
            player.sendInfo("loginCancelled")
            console().sendInfo("loginCancelledForPlayer", player.name)
            return true
        }
        return false
    }
    
    /**
     * 获取玩家的登录会话信息
     */
    fun getLoginSession(player: ProxyPlayer): LoginSession? {
        return loginSessions[player.uniqueId.toString()]
    }
    
    /**
     * 获取所有活跃的登录会话
     */
    fun getActiveLoginSessions(): Map<String, LoginSession> {
        return loginSessions.toMap()
    }
    
    /**
     * 清理过期的登录会话
     */
    fun cleanupExpiredSessions() {
        val currentTime = System.currentTimeMillis()
        val expiredTime = 10 * 60 * 1000L // 10分钟过期
        
        loginSessions.entries.removeIf { (_, session) ->
            val isExpired = currentTime - session.startTime > expiredTime
            if (isExpired) {
                console().sendInfo("loginSessionExpired", session.playerName)
            }
            isExpired
        }
    }
}

