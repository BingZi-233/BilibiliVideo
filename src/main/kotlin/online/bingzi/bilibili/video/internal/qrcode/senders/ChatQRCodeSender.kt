package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * 聊天框二维码发送器
 * 使用ASCII字符在聊天框显示二维码
 */
class ChatQRCodeSender : QRCodeSender {
    
    companion object {
        private const val QR_SIZE = 25 // 聊天框显示的二维码大小
        private const val BLOCK_CHAR = "█" // 实心方块字符
        private const val EMPTY_CHAR = " " // 空白字符
    }
    
    override fun sendQRCode(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String
    ): Boolean {
        return try {
            // 转换二维码为ASCII字符
            val asciiQRCode = convertToAscii(qrCodeImage)
            
            // 发送二维码标题
            player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            player.sendMessage("§a§l$title")
            player.sendMessage("§7$description")
            player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            
            // 发送ASCII二维码
            asciiQRCode.forEach { line ->
                player.sendMessage("§f$line")
            }
            
            player.sendMessage("§6§l▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬")
            player.sendMessage("§8扫描上方二维码完成操作")
            
            player.sendInfo("qrcodeChatSent", title)
            console().sendInfo("qrcodeChatCreated", player.name)
            
            true
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeChatFailed", player.name, e.message ?: "")
            false
        }
    }
    
    override fun getSenderName(): String = "聊天框发送器"
    
    override fun isAvailable(player: ProxyPlayer?): Boolean {
        // 聊天框发送器总是可用
        return true
    }
    
    /**
     * 将二维码图片转换为ASCII字符
     */
    private fun convertToAscii(qrCodeImage: BufferedImage): List<String> {
        // 缩放图片到合适的大小
        val scaledImage = scaleImage(qrCodeImage, QR_SIZE, QR_SIZE)
        val asciiLines = mutableListOf<String>()
        
        for (y in 0 until scaledImage.height) {
            val lineBuilder = StringBuilder()
            
            for (x in 0 until scaledImage.width) {
                val rgb = scaledImage.getRGB(x, y)
                val color = Color(rgb)
                val brightness = (color.red + color.green + color.blue) / 3
                
                // 二值化处理：亮度大于127为白色（空白），否则为黑色（方块）
                if (brightness > 127) {
                    lineBuilder.append(EMPTY_CHAR)
                } else {
                    lineBuilder.append(BLOCK_CHAR)
                }
            }
            
            asciiLines.add(lineBuilder.toString())
        }
        
        return asciiLines
    }
    
    /**
     * 缩放图片到指定尺寸
     */
    private fun scaleImage(originalImage: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
        val scaledImage = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
        val g2d = scaledImage.createGraphics()
        
        // 使用最近邻插值保持二维码的清晰度
        g2d.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
        g2d.dispose()
        
        return scaledImage
    }
}