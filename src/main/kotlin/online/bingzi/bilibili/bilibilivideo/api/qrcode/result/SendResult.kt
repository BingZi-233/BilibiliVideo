package online.bingzi.bilibili.bilibilivideo.api.qrcode.result

sealed class SendResult {
    data class Success(
        val senderId: String,                           // 发送器ID
        val timestamp: Long,                            // 发送时间戳
        val metadata: Map<String, Any> = emptyMap()     // 额外元数据
    ) : SendResult()
    
    data class Failure(
        val senderId: String,                           // 发送器ID
        val reason: String,                             // 失败原因
        val exception: Exception? = null,               // 异常对象
        val canRetry: Boolean = false                   // 是否可以重试
    ) : SendResult()
    
    data class Partial(
        val senderId: String,                           // 发送器ID
        val successCount: Int,                          // 成功数量
        val failureCount: Int,                          // 失败数量
        val details: String                             // 详细信息
    ) : SendResult()
}