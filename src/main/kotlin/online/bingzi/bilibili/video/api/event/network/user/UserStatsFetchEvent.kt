package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent
import online.bingzi.bilibili.video.internal.network.entity.UserStats

/**
 * 用户统计数据获取事件
 * 当获取用户统计数据时触发
 *
 * @param userId 用户 ID，null 表示获取当前登录用户统计
 * @param userStats 用户统计数据，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class UserStatsFetchEvent(
    val userId: String?,
    val userStats: UserStats?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()