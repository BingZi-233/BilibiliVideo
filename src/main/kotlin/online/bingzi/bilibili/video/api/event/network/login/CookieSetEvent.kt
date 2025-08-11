package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent

/**
 * Cookie 设置事件
 * 当通过 Cookie 字符串设置登录状态时触发
 *
 * @param success 是否设置成功
 * @param errorMessage 错误信息，成功时为 null
 */
class CookieSetEvent(
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliLoginEvent()