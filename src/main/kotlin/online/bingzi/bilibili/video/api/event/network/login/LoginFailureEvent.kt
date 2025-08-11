package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent
import online.bingzi.bilibili.video.internal.network.entity.LoginStatus

/**
 * 登录失败事件
 * 当登录失败时触发
 *
 * @param qrcodeKey 二维码密钥，使用 Cookie 登录时为 null
 * @param loginStatus 失败时的登录状态
 * @param errorMessage 错误信息
 * @param loginMethod 登录方式（qrcode 或 cookie）
 */
class LoginFailureEvent(
    val qrcodeKey: String?,
    val loginStatus: LoginStatus,
    val errorMessage: String,
    val loginMethod: String
) : BilibiliLoginEvent()