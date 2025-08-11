package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * API 请求重试事件
 * 当 API 请求需要重试时触发
 *
 * @param url 请求URL
 * @param method 请求方法（GET, POST 等）
 * @param requestId 请求唯一标识符
 * @param retryCount 当前重试次数
 * @param maxRetries 最大重试次数
 * @param reason 重试原因
 * @param delayMs 重试延迟时间（毫秒）
 */
class ApiRequestRetryEvent(
    val url: String,
    val method: String,
    val requestId: String,
    val retryCount: Int,
    val maxRetries: Int,
    val reason: String,
    val delayMs: Long
) : BilibiliApiEvent()