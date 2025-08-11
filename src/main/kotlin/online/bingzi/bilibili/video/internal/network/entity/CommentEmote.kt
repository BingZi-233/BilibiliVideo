package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论表情
 */
data class CommentEmote(
    val id: Int,
    val packageId: Int,
    val state: Int,
    val type: Int,
    val attr: Int,
    val text: String,
    val url: String,
    val meta: CommentEmoteMeta?,
    val mtime: Long
)