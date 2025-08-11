package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * API 请求完成事件
 * 当 API 请求完成（无论成功还是失败）时触发
 *
 * @param url 请求URL
 * @param method 请求方法（GET, POST 等）
 * @param requestId 请求唯一标识符
 * @param success 请求是否成功
 * @param statusCode HTTP状态码
 * @param responseSize 响应数据大小（字节）
 * @param duration 请求耗时（毫秒）
 * @param errorMessage 错误信息，成功时为 null
 */
class ApiRequestCompleteEvent(
    val url: String,
    val method: String,
    val requestId: String,
    val success: Boolean,
    val statusCode: Int,
    val responseSize: Long,
    val duration: Long,
    val errorMessage: String? = null
) : BilibiliApiEvent()