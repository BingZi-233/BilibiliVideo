package online.bingzi.bilibili.video.internal.qrcode

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import online.bingzi.bilibili.video.internal.config.ConfigManager
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn

/**
 * 二维码生成配置类
 * 提供配置化的二维码生成参数，避免硬编码
 * 
 * @author BingZi-233
 * @since 1.0.0
 */
data class QRCodeGenerationConfig(
    /** 前景色（二维码方块颜色），ARGB格式 */
    val foregroundColor: Int,
    
    /** 背景色（二维码背景颜色），ARGB格式 */
    val backgroundColor: Int,
    
    /** 默认二维码尺寸（像素） */
    val defaultSize: Int,
    
    /** 边距大小（模块数量），用于确保二维码四周有足够空白 */
    val margin: Int,
    
    /** 纠错级别，决定二维码的容错能力 */
    val errorCorrectionLevel: ErrorCorrectionLevel,
    
    /** 字符编码，通常为UTF-8以支持中文等多字节字符 */
    val characterSet: String,
    
    /** 尺寸推荐配置 */
    val sizeRecommendation: SizeRecommendationConfig
) {
    
    companion object {
        
        /**
         * 从配置文件创建二维码生成配置
         * 如果配置解析失败，将使用默认值并记录警告信息
         * 
         * @return 二维码生成配置实例
         */
        fun fromConfig(): QRCodeGenerationConfig {
            return try {
                QRCodeGenerationConfig(
                    foregroundColor = ConfigManager.getQRCodeForegroundColor(),
                    backgroundColor = ConfigManager.getQRCodeBackgroundColor(),
                    defaultSize = ConfigManager.getQRCodeDefaultSize(),
                    margin = ConfigManager.getQRCodeMargin(),
                    errorCorrectionLevel = parseErrorCorrectionLevel(ConfigManager.getQRCodeErrorCorrection()),
                    characterSet = ConfigManager.getQRCodeCharacterSet(),
                    sizeRecommendation = SizeRecommendationConfig.fromConfig()
                )
            } catch (e: Exception) {
                console().sendWarn("qrcodeConfigurationError", e.message ?: "未知错误")
                // 返回默认配置作为降级方案
                createDefaultConfig()
            }
        }
        
        /**
         * 创建默认配置
         * 用作配置解析失败时的降级方案
         * 
         * @return 使用默认值的二维码生成配置
         */
        private fun createDefaultConfig(): QRCodeGenerationConfig {
            return QRCodeGenerationConfig(
                foregroundColor = 0xFF000000.toInt(), // 黑色
                backgroundColor = 0xFFFFFFFF.toInt(), // 白色
                defaultSize = 256, // 256x256像素
                margin = 2, // 2个模块边距
                errorCorrectionLevel = ErrorCorrectionLevel.M, // 中等纠错级别
                characterSet = "UTF-8", // UTF-8编码
                sizeRecommendation = SizeRecommendationConfig.createDefault()
            )
        }
        
        /**
         * 解析纠错级别字符串
         * 支持 L/M/Q/H 四种级别，默认为M级别
         * 
         * @param level 纠错级别字符串
         * @return 对应的ErrorCorrectionLevel枚举值
         */
        private fun parseErrorCorrectionLevel(level: String): ErrorCorrectionLevel {
            return when (level.uppercase()) {
                "L" -> ErrorCorrectionLevel.L // 低级别（约7%容错）
                "M" -> ErrorCorrectionLevel.M // 中等级别（约15%容错）
                "Q" -> ErrorCorrectionLevel.Q // 高级别（约25%容错） 
                "H" -> ErrorCorrectionLevel.H // 最高级别（约30%容错）
                else -> {
                    console().sendWarn("qrcodeErrorCorrectionInvalid", level)
                    ErrorCorrectionLevel.M // 默认使用中等级别
                }
            }
        }
    }
}

/**
 * 尺寸推荐配置类
 * 根据内容长度提供合适的二维码尺寸推荐
 * 
 * @author BingZi-233
 * @since 1.0.0
 */
data class SizeRecommendationConfig(
    /** 短内容推荐尺寸（通常用于简短URL或文本） */
    val shortContentSize: Int,
    
    /** 中等内容推荐尺寸（通常用于普通长度的URL） */
    val mediumContentSize: Int,
    
    /** 长内容推荐尺寸（通常用于较长的URL或JSON数据） */
    val longContentSize: Int,
    
    /** 超长内容推荐尺寸（通常用于复杂数据或超长URL） */
    val extraLongContentSize: Int,
    
    /** 短内容长度上限（字符数） */
    val shortContentLimit: Int,
    
    /** 中等内容长度上限（字符数） */
    val mediumContentLimit: Int,
    
    /** 长内容长度上限（字符数） */
    val longContentLimit: Int
) {
    
    companion object {
        
        /**
         * 从配置文件创建尺寸推荐配置
         * 
         * @return 尺寸推荐配置实例
         */
        fun fromConfig(): SizeRecommendationConfig {
            return SizeRecommendationConfig(
                shortContentSize = ConfigManager.getQRCodeShortContentSize(),
                mediumContentSize = ConfigManager.getQRCodeMediumContentSize(),
                longContentSize = ConfigManager.getQRCodeLongContentSize(),
                extraLongContentSize = ConfigManager.getQRCodeExtraLongContentSize(),
                shortContentLimit = ConfigManager.getQRCodeShortContentLimit(),
                mediumContentLimit = ConfigManager.getQRCodeMediumContentLimit(),
                longContentLimit = ConfigManager.getQRCodeLongContentLimit()
            )
        }
        
        /**
         * 创建默认尺寸推荐配置
         * 
         * @return 使用默认值的尺寸推荐配置
         */
        fun createDefault(): SizeRecommendationConfig {
            return SizeRecommendationConfig(
                shortContentSize = 200,    // 200x200像素，适合短URL
                mediumContentSize = 256,   // 256x256像素，适合一般URL
                longContentSize = 300,     // 300x300像素，适合长URL
                extraLongContentSize = 400, // 400x400像素，适合复杂数据
                shortContentLimit = 50,    // 50字符以下视为短内容
                mediumContentLimit = 100,  // 100字符以下视为中等内容
                longContentLimit = 200     // 200字符以下视为长内容
            )
        }
    }
    
    /**
     * 根据内容长度获取推荐尺寸
     * 使用内容长度阈值来确定最适合的二维码尺寸
     * 
     * @param contentLength 内容字符长度
     * @return 推荐的二维码尺寸（像素）
     */
    fun getRecommendedSize(contentLength: Int): Int {
        return when {
            contentLength < shortContentLimit -> shortContentSize
            contentLength < mediumContentLimit -> mediumContentSize
            contentLength < longContentLimit -> longContentSize
            else -> extraLongContentSize
        }
    }
}