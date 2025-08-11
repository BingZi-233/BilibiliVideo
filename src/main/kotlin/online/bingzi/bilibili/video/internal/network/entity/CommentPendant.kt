package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论挂件信息
 */
data class CommentPendant(
    val pid: Int,
    val name: String,
    val image: String,
    val expire: Int,
    val imageEnhance: String,
    val imageEnhanceFrame: String
)