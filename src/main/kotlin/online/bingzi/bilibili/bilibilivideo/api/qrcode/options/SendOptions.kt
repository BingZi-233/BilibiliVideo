package online.bingzi.bilibili.bilibilivideo.api.qrcode.options

data class SendOptions(
    val expireTime: Long = 60000,                               // 过期时间（毫秒）
    val retryCount: Int = 0,                                    // 重试次数
    val customData: Map<String, Any> = emptyMap()               // 自定义参数供实现使用
)