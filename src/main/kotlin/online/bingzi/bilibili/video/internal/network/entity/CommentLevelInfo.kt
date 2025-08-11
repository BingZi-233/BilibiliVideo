package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论等级信息
 */
data class CommentLevelInfo(
    val currentLevel: Int,
    val currentMin: Int,
    val currentExp: Int,
    val nextExp: Int
)