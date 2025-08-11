package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * API 限流事件
 * 当检测到 API 限流时触发
 *
 * @param url 请求URL
 * @param method 请求方法
 * @param requestId 请求唯一标识符
 * @param rateLimitType 限流类型（user, ip, global 等）
 * @param remainingRequests 剩余请求次数，-1 表示未知
 * @param resetTime 限流重置时间戳，-1 表示未知
 * @param retryAfter 建议的重试延迟时间（秒）
 */
class ApiRateLimitEvent(
    val url: String,
    val method: String,
    val requestId: String,
    val rateLimitType: String,
    val remainingRequests: Int = -1,
    val resetTime: Long = -1L,
    val retryAfter: Int
) : BilibiliApiEvent()