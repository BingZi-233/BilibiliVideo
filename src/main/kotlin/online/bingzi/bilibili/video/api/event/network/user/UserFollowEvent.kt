package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent

/**
 * 用户关注操作事件
 * 当对用户进行关注/取消关注操作时触发
 *
 * @param targetUserId 目标用户 ID
 * @param follow true 为关注，false 为取消关注
 * @param success 操作是否成功
 * @param errorMessage 错误信息，成功时为 null
 */
class UserFollowEvent(
    val targetUserId: String,
    val follow: Boolean,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()