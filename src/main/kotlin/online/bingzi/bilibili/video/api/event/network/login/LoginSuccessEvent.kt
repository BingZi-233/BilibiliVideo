package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent

/**
 * 登录成功事件
 * 当用户成功登录时触发
 *
 * @param userId 用户 ID
 * @param loginMethod 登录方式（qrcode 或 cookie）
 */
class LoginSuccessEvent(
    val userId: String,
    val loginMethod: String
) : BilibiliLoginEvent()