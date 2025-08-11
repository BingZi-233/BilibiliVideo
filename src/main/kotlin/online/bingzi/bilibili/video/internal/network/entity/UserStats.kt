package online.bingzi.bilibili.video.internal.network.entity

/**
 * 用户关注统计信息
 * @param uid 用户 ID
 * @param following 关注数
 * @param follower 粉丝数
 */
data class UserStats(
    val uid: Long,
    val following: Long,
    val follower: Long
)