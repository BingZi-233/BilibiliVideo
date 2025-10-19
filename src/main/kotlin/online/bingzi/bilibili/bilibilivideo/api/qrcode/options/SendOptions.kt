package online.bingzi.bilibili.bilibilivideo.api.qrcode.options

/**
 * 二维码发送选项
 * 
 * 配置二维码发送的各种参数，包括过期时间、重试次数和自定义数据。
 * 
 * @property expireTime 过期时间（毫秒），默认60秒
 * @property retryCount 重试次数，默认不重试
 * @property customData 自定义参数供发送器实现使用，不同发送器可能支持不同的参数
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class SendOptions(
    val expireTime: Long = 60000,
    val retryCount: Int = 0,
    val customData: Map<String, Any> = emptyMap()
)