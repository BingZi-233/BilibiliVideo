package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * API 请求开始事件
 * 当开始执行 API 请求时触发
 *
 * @param url 请求URL
 * @param method 请求方法（GET, POST 等）
 * @param requestId 请求唯一标识符
 * @param timestamp 请求开始时间戳
 */
class ApiRequestStartEvent(
    val url: String,
    val method: String,
    val requestId: String,
) : BilibiliApiEvent()