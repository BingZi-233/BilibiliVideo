package online.bingzi.bilibili.video.internal.qrcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageConfig
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.awt.image.BufferedImage
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 二维码生成工具
 * 用于将文本或URL转换为二维码图片
 * 
 * 特性：
 * - 配置化参数管理，避免硬编码
 * - MatrixToImageConfig对象复用，提升性能
 * - 智能尺寸推荐，根据内容长度自动选择合适尺寸
 * - 详细的错误处理和日志记录
 * 
 * @author BingZi-233
 * @since 1.0.0
 */
object QRCodeGenerator {
    
    /** 二维码生成配置，启动时初始化并缓存 */
    private val config: QRCodeGenerationConfig by lazy {
        val generationConfig = QRCodeGenerationConfig.fromConfig()
        console().sendInfo("qrcodeGeneratorInitialized")
        generationConfig
    }
    
    /** MatrixToImageConfig缓存，避免重复创建相同配置的对象 */
    private val matrixConfigCache = ConcurrentHashMap<Pair<Int, Int>, MatrixToImageConfig>()
    
    /** 单例QRCodeWriter，避免重复创建 */
    private val qrCodeWriter = QRCodeWriter()
    
    /**
     * 生成二维码图片
     * 
     * @param content 要编码的内容（支持URL、文本、JSON等）
     * @param size 二维码尺寸（正方形像素），如果为null则根据内容长度自动推荐
     * @return 二维码BufferedImage，生成失败时返回null
     */
    fun generateQRCode(content: String, size: Int? = null): BufferedImage? {
        return try {
            // 参数验证
            if (content.isBlank()) {
                console().sendWarn("qrcodeGenerateFailed", content, "内容为空")
                return null
            }
            
            // 确定使用的尺寸：优先使用传入参数，其次智能推荐，最后使用默认值
            val targetSize = size ?: config.sizeRecommendation.getRecommendedSize(content.length)
            
            // 构建二维码生成参数映射
            val hints = createEncodingHints()
            
            // 生成二维码位矩阵
            val bitMatrix = qrCodeWriter.encode(
                content, 
                BarcodeFormat.QR_CODE, 
                targetSize, 
                targetSize, 
                hints
            )
            
            // 获取或创建MatrixToImageConfig（复用缓存以提升性能）
            val imageConfig = getOrCreateMatrixConfig(config.foregroundColor, config.backgroundColor)
            
            // 转换位矩阵为BufferedImage
            MatrixToImageWriter.toBufferedImage(bitMatrix, imageConfig)
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeGenerateFailed", content, e.message ?: "未知错误")
            null
        }
    }
    
    /**
     * 创建二维码编码参数映射
     * 使用配置化的参数，避免硬编码
     * 
     * @return 编码参数映射
     */
    private fun createEncodingHints(): Map<EncodeHintType, Any> {
        return EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            // 纠错级别：决定二维码的容错能力，级别越高越能抵抗污损但密度越大
            put(EncodeHintType.ERROR_CORRECTION, config.errorCorrectionLevel)
            
            // 字符编码：使用UTF-8确保中文等多字节字符正确编码
            put(EncodeHintType.CHARACTER_SET, config.characterSet)
            
            // 边距设置：确保二维码四周有足够的空白区域，提高扫描成功率
            put(EncodeHintType.MARGIN, config.margin)
        }
    }
    
    /**
     * 获取或创建MatrixToImageConfig
     * 使用缓存避免重复创建相同配置的对象，提升性能
     * 
     * @param foregroundColor 前景色（二维码方块颜色）
     * @param backgroundColor 背景色（二维码背景颜色）
     * @return MatrixToImageConfig实例
     */
    private fun getOrCreateMatrixConfig(foregroundColor: Int, backgroundColor: Int): MatrixToImageConfig {
        val cacheKey = Pair(foregroundColor, backgroundColor)
        return matrixConfigCache.computeIfAbsent(cacheKey) { 
            MatrixToImageConfig(foregroundColor, backgroundColor)
        }
    }
    
    /**
     * 验证二维码内容是否为有效URL
     * 检查内容是否符合URL格式并且使用HTTP/HTTPS协议
     * 
     * @param content 要验证的内容
     * @return true表示是有效的HTTP/HTTPS URL
     */
    fun isValidUrl(content: String): Boolean {
        return try {
            // 尝试创建URL对象验证格式
            java.net.URL(content)
            // 额外检查协议类型，确保是HTTP或HTTPS
            content.startsWith("http://", ignoreCase = true) || 
            content.startsWith("https://", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取推荐的二维码尺寸
     * 基于配置化的阈值和尺寸推荐，根据内容长度智能选择合适的二维码尺寸
     * 
     * @param contentLength 内容字符长度
     * @return 推荐的二维码尺寸（像素）
     */
    fun getRecommendedSize(contentLength: Int): Int {
        return config.sizeRecommendation.getRecommendedSize(contentLength)
    }
    
    /**
     * 清理MatrixToImageConfig缓存
     * 用于内存管理，在不需要时清理缓存
     */
    fun clearCache() {
        matrixConfigCache.clear()
    }
    
    /**
     * 获取当前缓存状态信息
     * 用于监控和调试
     * 
     * @return 缓存统计信息
     */
    fun getCacheStats(): String {
        return "MatrixToImageConfig缓存: ${matrixConfigCache.size} 个对象"
    }
    
    /**
     * 获取当前使用的配置信息
     * 用于调试和配置验证
     * 
     * @return 配置信息摘要
     */
    fun getConfigSummary(): String {
        return "二维码生成配置: 默认尺寸=${config.defaultSize}px, 边距=${config.margin}, " +
               "纠错级别=${config.errorCorrectionLevel}, 编码=${config.characterSet}"
    }
}