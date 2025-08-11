package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent

/**
 * 用户登出事件
 * 当用户主动登出或会话过期时触发
 *
 * @param userId 用户 ID，如果无法获取则为 null
 * @param reason 登出原因（logout 主动登出、expired 会话过期）
 */
class UserLogoutEvent(
    val userId: String?,
    val reason: String
) : BilibiliLoginEvent()