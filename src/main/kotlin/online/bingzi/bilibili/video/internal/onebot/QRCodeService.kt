package online.bingzi.bilibili.video.internal.onebot

import online.bingzi.bilibili.video.internal.config.OneBotConfig
import online.bingzi.bilibili.video.internal.helper.toBufferedImage
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.warning
import taboolib.module.lang.asLangText
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

/**
 * 二维码发送服务
 * 
 * 通过QQ发送二维码
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object QRCodeService {
    
    /**
     * 发送二维码到QQ
     * 
     * @param player 玩家
     * @param qrCodeUrl 二维码内容
     * @param title 标题
     * @param description 描述
     * @return 是否发送成功
     */
    fun sendQRCode(
        player: ProxyPlayer,
        qrCodeUrl: String,
        title: String = "Bilibili登录",
        description: String = "请使用Bilibili客户端扫描二维码"
    ): Boolean {
        // 检查OneBot是否连接
        if (!OneBotManager.isConnected()) {
            player.infoAsLang("OneBotNotConnected")
            return false
        }
        
        // 检查玩家是否绑定QQ
        val binding = QQBindManager.getBinding(player.uniqueId)
        if (binding == null) {
            player.infoAsLang("QQNotBound")
            return false
        }
        
        // 生成二维码图片
        val qrCodeImage = generateQRCodeImage(qrCodeUrl)
        if (qrCodeImage == null) {
            player.infoAsLang("QRCodeGenerateFailed")
            return false
        }
        
        // 转换为Base64
        val base64Image = imageToBase64(qrCodeImage)
        if (base64Image == null) {
            player.infoAsLang("QRCodeProcessFailed")
            return false
        }
        
        // 构建消息
        val message = buildString {
            append("【$title】\n")
            append("$description\n")
            append("二维码有效期：3分钟\n")
            append("扫码后请在手机上确认登录")
        }
        
        // 发送到QQ
        val success = OneBotManager.sendPrivateImage(binding.qqNumber, base64Image, message)
        
        if (success) {
            player.infoAsLang("QRCodeSent")
            
            // 更新最后使用时间
            QQBindManager.updateLastUsed(player.uniqueId)
        } else {
            player.infoAsLang("QRCodeSendFailed")
        }
        
        return success
    }
    
    /**
     * 生成二维码图片
     * 
     * @param content 二维码内容
     * @return BufferedImage对象，如果失败则返回null
     */
    private fun generateQRCodeImage(content: String): BufferedImage? {
        return try {
            content.toBufferedImage(OneBotConfig.qrCodeSize)
        } catch (e: Exception) {
            warning("生成二维码图片失败: ${e.message}")
            null
        }
    }
    
    /**
     * 将图片转换为Base64
     * 
     * @param image BufferedImage对象
     * @return Base64字符串，如果失败则返回null
     */
    private fun imageToBase64(image: BufferedImage): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image, "PNG", outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.getEncoder().encodeToString(imageBytes)
        } catch (e: Exception) {
            warning("图片转Base64失败: ${e.message}")
            null
        }
    }
    
    /**
     * 发送扫码成功通知
     * 
     * @param player 玩家
     */
    fun sendScanSuccessNotification(player: ProxyPlayer) {
        val binding = QQBindManager.getBinding(player.uniqueId) ?: return
        
        if (OneBotManager.isConnected()) {
            OneBotManager.sendPrivateMessage(
                binding.qqNumber,
                player.asLangText("QRCodeScanned")
            )
        }
    }
    
    /**
     * 发送登录成功通知
     * 
     * @param player 玩家
     * @param bilibiliName Bilibili用户名
     */
    fun sendLoginSuccessNotification(player: ProxyPlayer, bilibiliName: String) {
        val binding = QQBindManager.getBinding(player.uniqueId) ?: return
        
        if (OneBotManager.isConnected()) {
            val message = "✅ 登录成功！\n" +
                    "Bilibili账号：$bilibiliName\n" +
                    "游戏账号：${player.name}\n" +
                    "您现在可以领取视频奖励了！"
            
            OneBotManager.sendPrivateMessage(binding.qqNumber, message)
        }
    }
    
    /**
     * 发送二维码过期通知
     * 
     * @param player 玩家
     */
    fun sendQRCodeExpiredNotification(player: ProxyPlayer) {
        val binding = QQBindManager.getBinding(player.uniqueId) ?: return
        
        if (OneBotManager.isConnected()) {
            OneBotManager.sendPrivateMessage(
                binding.qqNumber,
                player.asLangText("QRCodeExpired")
            )
        }
    }
}