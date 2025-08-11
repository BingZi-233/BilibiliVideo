package online.bingzi.bilibili.video.internal.qrcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.awt.image.BufferedImage
import java.util.*

/**
 * 二维码生成工具
 * 用于将文本或URL转换为二维码图片
 */
object QRCodeGenerator {
    
    /**
     * 生成二维码图片
     * @param content 要编码的内容
     * @param size 二维码尺寸（正方形）
     * @return 二维码BufferedImage，失败时返回null
     */
    fun generateQRCode(content: String, size: Int = 256): BufferedImage? {
        return try {
            val qrCodeWriter = QRCodeWriter()
            
            // 设置二维码生成参数
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
                put(EncodeHintType.MARGIN, 1)
            }
            
            // 生成二维码矩阵
            val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            
            // 转换为BufferedImage
            MatrixToImageWriter.toBufferedImage(bitMatrix)
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeGenerateFailed", content, e.message ?: "")
            null
        }
    }
    
    /**
     * 验证二维码内容是否为有效URL
     */
    fun isValidUrl(content: String): Boolean {
        return try {
            java.net.URL(content)
            content.startsWith("http://") || content.startsWith("https://")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取推荐的二维码尺寸
     * @param contentLength 内容长度
     * @return 推荐尺寸
     */
    fun getRecommendedSize(contentLength: Int): Int {
        return when {
            contentLength < 50 -> 200
            contentLength < 100 -> 256
            contentLength < 200 -> 300
            else -> 400
        }
    }
}