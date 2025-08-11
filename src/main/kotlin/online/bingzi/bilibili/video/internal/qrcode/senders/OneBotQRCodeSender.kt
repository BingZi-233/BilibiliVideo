package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.database.PlayerQQBindingService
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSender
import online.bingzi.onebot.api.OneBotAPI
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

/**
 * OneBot QQ机器人二维码发送器
 * 通过OneBot协议将二维码发送到QQ
 */
class OneBotQRCodeSender : QRCodeSender {
    
    override fun sendQRCode(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String
    ): Boolean {
        return try {
            // 检查OneBot连接状态
            if (!OneBotAPI.isConnected()) {
                player.sendWarn("qrcodeOneBotNotConnected")
                console().sendWarn("qrcodeOneBotNotConnected")
                return false
            }
            
            // 异步获取玩家绑定的QQ号并发送
            PlayerQQBindingService.getPlayerQQNumber(player).thenAccept { qqNumber ->
                if (qqNumber == null) {
                    player.sendWarn("qrcodeOneBotNotBound")
                    console().sendWarn("qrcodeOneBotPlayerNotBound", player.name)
                } else {
                    try {
                        // 将二维码图片转换为Base64并构建消息
                        val base64Image = imageToBase64(qrCodeImage)
                        val message = buildOneBotMessage(title, description, base64Image)
                        
                        // 使用OneBot API发送私聊消息
                        val success = OneBotAPI.sendPrivateMessage(qqNumber, message)
                        
                        if (success) {
                            player.sendInfo("qrcodeOneBotSent", title, qqNumber.toString())
                            console().sendInfo("qrcodeOneBotCreated", player.name, qqNumber.toString())
                        } else {
                            player.sendWarn("qrcodeOneBotSendFailed")
                        }
                    } catch (e: Exception) {
                        console().sendWarn("qrcodeOneBotSendError", player.name, e.message ?: "")
                        player.sendWarn("qrcodeOneBotSendFailed")
                    }
                }
            }.exceptionally { e ->
                console().sendWarn("qrcodeOneBotQueryError", player.name, e.message ?: "")
                player.sendWarn("qrcodeOneBotNotBound")
                null
            }
            
            // 返回true表示处理已开始，具体结果通过消息通知
            true
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeOneBotFailed", player.name, e.message ?: "")
            false
        }
    }
    
    override fun getSenderName(): String = "OneBot发送器"
    
    override fun isAvailable(player: ProxyPlayer?): Boolean {
        return try {
            // 检查OneBot插件连接状态
            if (!OneBotAPI.isConnected()) return false
            
            // 如果指定了玩家，同步检查玩家是否有绑定的QQ号
            if (player != null) {
                // 由于这个方法是同步的，我们需要阻塞等待结果
                // 在实际使用中，调用方会处理异步情况
                try {
                    val hasBinding = PlayerQQBindingService.hasValidQQBinding(player).get()
                    hasBinding
                } catch (e: Exception) {
                    // 如果查询失败，返回false
                    false
                }
            } else {
                // 如果没有指定玩家，只检查OneBot连接状态
                true
            }
            
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 将BufferedImage转换为Base64字符串
     */
    private fun imageToBase64(image: BufferedImage): String {
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        val imageBytes = baos.toByteArray()
        return Base64.getEncoder().encodeToString(imageBytes)
    }
    
    /**
     * 构建OneBot消息内容
     */
    private fun buildOneBotMessage(title: String, description: String, base64Image: String): String {
        return "📱 $title\n$description\n\n[CQ:image,file=base64://$base64Image]"
    }
    
}