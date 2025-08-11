package online.bingzi.bilibili.video.internal.network.entity

/**
 * 粉丝详情
 */
data class CommentFansDetail(
    val uid: Long,
    val medalId: Int,
    val medalName: String,
    val score: Int,
    val level: Int,
    val intimacy: Int,
    val masterStatus: Int,
    val isReceive: Int
)