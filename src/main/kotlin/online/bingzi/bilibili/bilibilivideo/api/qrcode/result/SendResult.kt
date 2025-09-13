package online.bingzi.bilibili.bilibilivideo.api.qrcode.result

/**
 * 二维码发送结果密封类
 * 
 * 使用密封类模式表示二维码发送的三种可能结果：成功、失败、部分成功。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
sealed class SendResult {
    /**
     * 发送成功
     * 
     * @property senderId 发送器ID
     * @property timestamp 发送时间戳
     * @property metadata 额外元数据，可包含发送器特定的信息
     */
    data class Success(
        val senderId: String,
        val timestamp: Long,
        val metadata: Map<String, Any> = emptyMap()
    ) : SendResult()
    
    /**
     * 发送失败
     * 
     * @property senderId 发送器ID
     * @property reason 失败原因描述
     * @property exception 导致失败的异常对象，可为null
     * @property canRetry 是否可以重试，true表示错误可能是暂时的
     */
    data class Failure(
        val senderId: String,
        val reason: String,
        val exception: Exception? = null,
        val canRetry: Boolean = false
    ) : SendResult()
    
    /**
     * 部分成功
     * 
     * 用于批量操作或有多个发送目标时的场景。
     * 
     * @property senderId 发送器ID
     * @property successCount 成功数量
     * @property failureCount 失败数量
     * @property details 详细信息描述
     */
    data class Partial(
        val senderId: String,
        val successCount: Int,
        val failureCount: Int,
        val details: String
    ) : SendResult()
}