package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论折叠信息
 */
data class CommentFolder(
    val hasFolded: Boolean,
    val isFolded: Boolean,
    val rule: String
)