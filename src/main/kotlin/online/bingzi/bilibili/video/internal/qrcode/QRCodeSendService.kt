package online.bingzi.bilibili.video.internal.qrcode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.bingzi.bilibili.video.api.event.qrcode.*
import online.bingzi.bilibili.video.internal.qrcode.senders.ChatQRCodeSender
import online.bingzi.bilibili.video.internal.qrcode.senders.MapQRCodeSender
import online.bingzi.bilibili.video.internal.qrcode.senders.OneBotQRCodeSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.awt.image.BufferedImage
import java.util.concurrent.CompletableFuture

/**
 * 二维码发送服务
 * 管理和协调各种二维码发送方式
 */
object QRCodeSendService {
    
    private val senders = mutableMapOf<QRCodeSendMode, QRCodeSender>()
    
    /**
     * 初始化所有发送器
     */
    fun initialize() {
        try {
            // 注册聊天框发送器
            senders[QRCodeSendMode.CHAT] = ChatQRCodeSender()
            console().sendInfo("qrcodeSenderRegistered", "聊天框")
            
            // 注册地图发送器
            if (MapQRCodeSender().isAvailable()) {
                senders[QRCodeSendMode.MAP] = MapQRCodeSender()
                console().sendInfo("qrcodeSenderRegistered", "地图")
            } else {
                console().sendWarn("qrcodeSenderUnavailable", "地图", "Bukkit环境不可用")
            }
            
            // 注册OneBot发送器
            if (OneBotQRCodeSender().isAvailable()) {
                senders[QRCodeSendMode.ONEBOT] = OneBotQRCodeSender()
                console().sendInfo("qrcodeSenderRegistered", "OneBot")
            } else {
                console().sendWarn("qrcodeSenderUnavailable", "OneBot", "OneBot服务不可用")
            }
            
            console().sendInfo("qrcodeServiceInitialized", senders.size.toString())
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeServiceInitFailed", e.message ?: "")
        }
    }
    
    /**
     * 发送二维码给玩家
     * @param player 目标玩家
     * @param qrCodeImage 二维码图片
     * @param title 二维码标题
     * @param description 二维码描述
     * @param preferredMode 优先发送模式
     * @return 发送是否成功
     */
    fun sendQRCode(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String,
        preferredMode: QRCodeSendMode = QRCodeSendMode.CHAT
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            runCatching {
                sendQRCodeInternal(player, qrCodeImage, title, description, preferredMode)
            }.getOrElse { e ->
                console().sendWarn("qrcodeSendFailed", player.name, e.message ?: "")
                false
            }
        }
    }
    
    /**
     * 内部发送逻辑
     */
    private fun sendQRCodeInternal(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String,
        preferredMode: QRCodeSendMode
    ): Boolean {
        // 触发发送前事件
        val preSendEvent = QRCodePreSendEvent(player, qrCodeImage, title, description, preferredMode)
        if (!preSendEvent.call()) {
            // 事件被取消
            console().sendWarn("qrcodeSendCancelled", player.name)
            return false
        }
        
        val startTime = System.currentTimeMillis()
        
        // 尝试使用优先模式
        val preferredSender = senders[preferredMode]
        if (preferredSender != null && preferredSender.isAvailable(player)) {
            console().sendInfo("qrcodeSendAttempt", preferredMode.displayName, player.name)
            
            try {
                if (preferredSender.sendQRCode(player, qrCodeImage, title, description)) {
                    val endTime = System.currentTimeMillis()
                    console().sendInfo("qrcodeSendSuccess", preferredMode.displayName, player.name)
                    
                    // 触发发送成功事件
                    QRCodeSendSuccessEvent(
                        player, qrCodeImage, title, description, preferredMode, endTime - startTime
                    ).call()
                    
                    return true
                } else {
                    console().sendWarn("qrcodeSendModeFailed", preferredMode.displayName, player.name)
                    
                    // 触发发送失败事件
                    QRCodeSendFailureEvent(
                        player, qrCodeImage, title, description, preferredMode, "发送器返回失败", null
                    ).call()
                }
            } catch (e: Exception) {
                console().sendWarn("qrcodeSendModeFailed", preferredMode.displayName, player.name)
                
                // 触发发送失败事件
                QRCodeSendFailureEvent(
                    player, qrCodeImage, title, description, preferredMode, e.message, e
                ).call()
            }
        }
        
        // 优先模式失败，尝试其他可用的发送器
        for ((mode, sender) in senders) {
            if (mode == preferredMode) continue // 跳过已尝试的优先模式
            
            if (sender.isAvailable(player)) {
                console().sendInfo("qrcodeSendFallback", mode.displayName, player.name)
                
                // 触发模式切换事件
                QRCodeSendModeChangeEvent(
                    player, qrCodeImage, title, description, preferredMode, mode, "优先模式发送失败"
                ).call()
                
                try {
                    if (sender.sendQRCode(player, qrCodeImage, title, description)) {
                        val endTime = System.currentTimeMillis()
                        console().sendInfo("qrcodeSendSuccess", mode.displayName, player.name)
                        
                        // 触发发送成功事件
                        QRCodeSendSuccessEvent(
                            player, qrCodeImage, title, description, mode, endTime - startTime
                        ).call()
                        
                        return true
                    } else {
                        console().sendWarn("qrcodeSendModeFailed", mode.displayName, player.name)
                        
                        // 触发发送失败事件
                        QRCodeSendFailureEvent(
                            player, qrCodeImage, title, description, mode, "发送器返回失败", null
                        ).call()
                    }
                } catch (e: Exception) {
                    console().sendWarn("qrcodeSendModeFailed", mode.displayName, player.name)
                    
                    // 触发发送失败事件
                    QRCodeSendFailureEvent(
                        player, qrCodeImage, title, description, mode, e.message, e
                    ).call()
                }
            }
        }
        
        console().sendWarn("qrcodeSendAllFailed", player.name)
        
        // 触发所有模式都失败的事件
        QRCodeSendFailureEvent(
            player, qrCodeImage, title, description, preferredMode, "所有发送模式都失败", null
        ).call()
        
        return false
    }
    
    /**
     * 获取可用的发送模式
     * @param player 目标玩家
     * @return 可用的发送模式列表
     */
    fun getAvailableModes(player: ProxyPlayer): List<QRCodeSendMode> {
        return senders.entries
            .filter { it.value.isAvailable(player) }
            .map { it.key }
    }
    
    /**
     * 检查特定模式是否可用
     * @param mode 发送模式
     * @param player 目标玩家
     * @return 是否可用
     */
    fun isModeAvailable(mode: QRCodeSendMode, player: ProxyPlayer? = null): Boolean {
        val sender = senders[mode] ?: return false
        return sender.isAvailable(player)
    }
    
    /**
     * 清理资源
     */
    fun shutdown() {
        senders.clear()
        console().sendInfo("qrcodeServiceShutdown")
    }
}