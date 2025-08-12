package online.bingzi.bilibili.video.internal.qrcode

import online.bingzi.bilibili.video.api.event.qrcode.QRCodePreSendEvent
import online.bingzi.bilibili.video.api.event.qrcode.QRCodeSendFailureEvent
import online.bingzi.bilibili.video.api.event.qrcode.QRCodeSendModeChangeEvent
import online.bingzi.bilibili.video.api.event.qrcode.QRCodeSendSuccessEvent
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
    
    private val senders = mutableMapOf<String, QRCodeSender>()
    
    /**
     * 注册二维码发送器
     * @param senderName 发送器名称
     * @param sender 发送器实例
     * @return 是否注册成功
     */
    fun registerSender(senderName: String, sender: QRCodeSender): Boolean {
        return try {
            // 检查发送器是否可用
            if (!sender.isAvailable()) {
                console().sendWarn("qrcodeSenderUnavailable", senderName, "发送器不可用")
                return false
            }
            
            // 如果已存在同名发送器，先注销
            if (senders.containsKey(senderName)) {
                console().sendWarn("qrcodeSenderReplaced", senderName)
            }
            
            // 注册新发送器
            senders[senderName] = sender
            console().sendInfo("qrcodeSenderRegistered", senderName)
            true
        } catch (e: Exception) {
            console().sendWarn("qrcodeSenderRegisterFailed", senderName, e.message ?: "")
            false
        }
    }
    
    /**
     * 注销二维码发送器
     * @param senderName 发送器名称
     * @return 是否注销成功
     */
    fun unregisterSender(senderName: String): Boolean {
        return if (senders.remove(senderName) != null) {
            console().sendInfo("qrcodeSenderUnregistered", senderName)
            true
        } else {
            false
        }
    }
    
    /**
     * 获取已注册的发送器数量
     * @return 发送器数量
     */
    fun getRegisteredSenderCount(): Int = senders.size
    
    /**
     * 获取所有已注册的发送器名称
     * @return 发送器名称集合
     */
    fun getRegisteredSenderNames(): Set<String> = senders.keys.toSet()
    
    /**
     * 发送二维码给玩家
     * @param player 目标玩家
     * @param qrCodeImage 二维码图片
     * @param title 二维码标题
     * @param description 二维码描述
     * @param preferredSenderName 优先发送器名称
     * @return 发送是否成功
     */
    fun sendQRCode(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String,
        preferredSenderName: String = QRCodeSenderNames.CHAT
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            runCatching {
                sendQRCodeInternal(player, qrCodeImage, title, description, preferredSenderName)
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
        preferredSenderName: String
    ): Boolean {
        // 触发发送前事件
        val preSendEvent = QRCodePreSendEvent(player, qrCodeImage, title, description, preferredSenderName)
        if (!preSendEvent.call()) {
            // 事件被取消
            console().sendWarn("qrcodeSendCancelled", player.name)
            return false
        }
        
        val startTime = System.currentTimeMillis()
        
        // 尝试使用优先发送器
        val preferredSender = senders[preferredSenderName]
        if (preferredSender != null && preferredSender.isAvailable(player)) {
            console().sendInfo("qrcodeSendAttempt", preferredSenderName, player.name)
            
            try {
                if (preferredSender.sendQRCode(player, qrCodeImage, title, description)) {
                    val endTime = System.currentTimeMillis()
                    console().sendInfo("qrcodeSendSuccess", preferredSenderName, player.name)
                    
                    // 触发发送成功事件
                    QRCodeSendSuccessEvent(
                        player, qrCodeImage, title, description, preferredSenderName, endTime - startTime
                    ).call()
                    
                    return true
                } else {
                    console().sendWarn("qrcodeSendModeFailed", preferredSenderName, player.name)
                    
                    // 触发发送失败事件
                    QRCodeSendFailureEvent(
                        player, qrCodeImage, title, description, preferredSenderName, "发送器返回失败", null
                    ).call()
                }
            } catch (e: Exception) {
                console().sendWarn("qrcodeSendModeFailed", preferredSenderName, player.name)
                
                // 触发发送失败事件
                QRCodeSendFailureEvent(
                    player, qrCodeImage, title, description, preferredSenderName, e.message, e
                ).call()
            }
        }
        
        // 优先发送器失败，尝试其他可用的发送器
        for ((senderName, sender) in senders) {
            if (senderName == preferredSenderName) continue // 跳过已尝试的优先发送器
            
            if (sender.isAvailable(player)) {
                console().sendInfo("qrcodeSendFallback", senderName, player.name)
                
                // 触发模式切换事件
                QRCodeSendModeChangeEvent(
                    player, qrCodeImage, title, description, preferredSenderName, senderName, "优先发送器发送失败"
                ).call()
                
                try {
                    if (sender.sendQRCode(player, qrCodeImage, title, description)) {
                        val endTime = System.currentTimeMillis()
                        console().sendInfo("qrcodeSendSuccess", senderName, player.name)
                        
                        // 触发发送成功事件
                        QRCodeSendSuccessEvent(
                            player, qrCodeImage, title, description, senderName, endTime - startTime
                        ).call()
                        
                        return true
                    } else {
                        console().sendWarn("qrcodeSendModeFailed", senderName, player.name)
                        
                        // 触发发送失败事件
                        QRCodeSendFailureEvent(
                            player, qrCodeImage, title, description, senderName, "发送器返回失败", null
                        ).call()
                    }
                } catch (e: Exception) {
                    console().sendWarn("qrcodeSendModeFailed", senderName, player.name)
                    
                    // 触发发送失败事件
                    QRCodeSendFailureEvent(
                        player, qrCodeImage, title, description, senderName, e.message, e
                    ).call()
                }
            }
        }
        
        console().sendWarn("qrcodeSendAllFailed", player.name)
        
        // 触发所有发送器都失败的事件
        QRCodeSendFailureEvent(
            player, qrCodeImage, title, description, preferredSenderName, "所有发送器都失败", null
        ).call()
        
        return false
    }
    
    /**
     * 获取可用的发送器名称
     * @param player 目标玩家
     * @return 可用的发送器名称列表
     */
    fun getAvailableSenderNames(player: ProxyPlayer): List<String> {
        return senders.entries
            .filter { it.value.isAvailable(player) }
            .map { it.key }
    }
    
    /**
     * 检查特定发送器是否可用
     * @param senderName 发送器名称
     * @param player 目标玩家
     * @return 是否可用
     */
    fun isSenderAvailable(senderName: String, player: ProxyPlayer? = null): Boolean {
        val sender = senders[senderName] ?: return false
        return sender.isAvailable(player)
    }
    
    /**
     * 清理资源
     */
    fun shutdown() {
        val count = senders.size
        senders.clear()
        console().sendInfo("qrcodeServiceShutdown", count.toString())
    }
}