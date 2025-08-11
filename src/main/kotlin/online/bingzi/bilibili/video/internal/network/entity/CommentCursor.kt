package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论游标信息
 * 用于分页获取更多评论
 */
data class CommentCursor(
    val allCount: Long,
    val isBegin: Boolean,
    val prev: Long,
    val next: Long,
    val isEnd: Boolean,
    val mode: Int,
    val showType: Int,
    val supportMode: List<Int>,
    val name: String
)