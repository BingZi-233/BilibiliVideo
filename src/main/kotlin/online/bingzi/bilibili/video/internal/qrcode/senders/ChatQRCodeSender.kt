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
        private const val BLOCK_CHAR = "██" // 双重实心方块字符，增强显示效果
        private const val EMPTY_CHAR = "  " // 双空格字符，保持对称
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
                player.sendMessage("§0$line") // 使用黑色文字确保方块字符清晰可见
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
                
                // 直接使用RGB值进行判断，避免颜色空间转换问题
                // ZXing生成的二维码：黑色为0x000000，白色为0xFFFFFF
                val isWhite = (rgb and 0xFFFFFF) > 0x800000  // 更严格的白色判断
                
                if (isWhite) {
                    // 白色区域显示空格
                    lineBuilder.append(EMPTY_CHAR)
                } else {
                    // 黑色区域显示实心方块
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
        // 使用与原图相同的颜色模型，确保颜色信息不丢失
        val scaledImage = BufferedImage(targetWidth, targetHeight, originalImage.type)
        val g2d = scaledImage.createGraphics()
        
        try {
            // 使用最近邻插值保持二维码的清晰度，避免颜色混合
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
            )
            
            // 禁用抗锯齿，保持二维码的锐利边缘
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_OFF
            )
            
            // 使用精确的颜色渲染
            g2d.setRenderingHint(
                java.awt.RenderingHints.KEY_COLOR_RENDERING,
                java.awt.RenderingHints.VALUE_COLOR_RENDER_QUALITY
            )
            
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
            
        } finally {
            g2d.dispose()
        }
        
        return scaledImage
    }
}