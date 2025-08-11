package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent
import online.bingzi.bilibili.video.internal.network.entity.LoginStatus

/**
 * 登录状态轮询事件
 * 当轮询登录状态时触发
 *
 * @param qrcodeKey 二维码密钥
 * @param loginStatus 当前登录状态
 * @param isStatusChanged 状态是否发生变化
 */
class LoginStatusPollEvent(
    val qrcodeKey: String,
    val loginStatus: LoginStatus,
    val isStatusChanged: Boolean
) : BilibiliLoginEvent()