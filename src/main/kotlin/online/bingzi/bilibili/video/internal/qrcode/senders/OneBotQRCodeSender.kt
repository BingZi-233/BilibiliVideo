package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSender
import online.bingzi.onebot.api.OneBotAPI
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
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
            
            // 获取玩家绑定的QQ号
            val qqNumber = getPlayerQQNumber(player) ?: return false
            
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
            
            success
            
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
            
            // TODO: 等待其他模块实现玩家QQ绑定功能后，这里检查玩家绑定状态
            // 如果指定了玩家，检查玩家是否有绑定的QQ号
            player?.let { getPlayerQQNumber(it) != null } ?: true
            
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
     * 获取玩家绑定的QQ号
     * TODO: 玩家QQ绑定功能由其他模块实现
     */
    private fun getPlayerQQNumber(player: ProxyPlayer): Long? {
        // TODO: 这里应该调用其他模块提供的玩家QQ绑定API
        // 临时返回null，等待其他模块实现绑定功能
        player.sendWarn("qrcodeOneBotNotBound")
        console().sendWarn("qrcodeOneBotPlayerNotBound", player.name)
        return null
    }
    
    /**
     * 构建OneBot消息内容
     */
    private fun buildOneBotMessage(title: String, description: String, base64Image: String): String {
        return "📱 $title\n$description\n\n[CQ:image,file=base64://$base64Image]"
    }
    
}