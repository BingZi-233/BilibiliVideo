package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent

/**
 * 用户账号状态检查事件
 * 当检查用户账号状态时触发
 *
 * @param userId 用户 ID
 * @param isValid 账号是否有效
 * @param isLoggedIn 是否已登录
 * @param errorMessage 错误信息，检查成功时为 null
 */
class UserAccountStatusCheckEvent(
    val userId: String?,
    val isValid: Boolean,
    val isLoggedIn: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()