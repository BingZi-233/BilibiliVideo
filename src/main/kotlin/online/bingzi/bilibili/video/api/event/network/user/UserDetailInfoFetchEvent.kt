package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent
import online.bingzi.bilibili.video.internal.network.entity.UserDetailInfo

/**
 * 用户详细信息获取事件
 * 当获取用户详细信息时触发
 *
 * @param userId 用户 ID，null 表示获取当前登录用户信息
 * @param userDetailInfo 用户详细信息，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class UserDetailInfoFetchEvent(
    val userId: String?,
    val userDetailInfo: UserDetailInfo?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()