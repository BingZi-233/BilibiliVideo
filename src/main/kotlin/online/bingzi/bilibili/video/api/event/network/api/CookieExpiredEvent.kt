package online.bingzi.bilibili.video.api.event.network.api

import online.bingzi.bilibili.video.api.event.network.BilibiliApiEvent

/**
 * Cookie 过期事件
 * 当检测到 Cookie 已过期需要重新登录时触发
 *
 * @param userId 用户 ID，如果无法获取则为 null
 * @param cookieName 过期的 Cookie 名称
 * @param requestUrl 检测到过期的请求URL
 * @param errorMessage 错误信息
 */
class CookieExpiredEvent(
    val userId: String?,
    val cookieName: String,
    val requestUrl: String,
    val errorMessage: String
) : BilibiliApiEvent()