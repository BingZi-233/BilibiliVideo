package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论勋章信息
 */
data class CommentNameplate(
    val nid: Int,
    val name: String,
    val image: String,
    val imageSmall: String,
    val level: String,
    val condition: String
)