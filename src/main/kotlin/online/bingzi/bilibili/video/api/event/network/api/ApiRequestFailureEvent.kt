package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * API 请求失败事件
 * 当 API 请求发生错误时触发
 *
 * @param url 请求URL
 * @param method 请求方法（GET, POST 等）
 * @param requestId 请求唯一标识符
 * @param errorType 错误类型（network, timeout, auth, server 等）
 * @param errorMessage 错误详细信息
 * @param statusCode HTTP状态码，如果有的话
 * @param retryCount 重试次数
 * @param duration 请求耗时（毫秒）
 */
class ApiRequestFailureEvent(
    val url: String,
    val method: String,
    val requestId: String,
    val errorType: String,
    val errorMessage: String,
    val statusCode: Int? = null,
    val retryCount: Int = 0,
    val duration: Long
) : BilibiliApiEvent()