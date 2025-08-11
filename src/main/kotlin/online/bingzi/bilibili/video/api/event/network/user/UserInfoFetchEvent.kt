package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent
import online.bingzi.bilibili.video.internal.network.entity.UserInfo

/**
 * 用户基本信息获取事件
 * 当获取用户基本信息时触发
 *
 * @param userId 用户 ID，null 表示获取当前登录用户信息
 * @param userInfo 用户信息，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class UserInfoFetchEvent(
    val userId: String?,
    val userInfo: UserInfo?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()