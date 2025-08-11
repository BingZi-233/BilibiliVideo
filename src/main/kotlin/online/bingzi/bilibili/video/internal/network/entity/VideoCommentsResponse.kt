package online.bingzi.bilibili.video.internal.network.entity

/**
 * 视频评论区响应
 *
 * @param comments 评论列表
 * @param total 评论总数
 * @param pages 总页数
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 * @param cursor 游标（用于下一页获取）
 * @param hasMore 是否还有更多评论
 * @param topComments 置顶评论列表
 */
data class VideoCommentsResponse(
    val comments: List<CommentInfo>,
    val total: Long,
    val pages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val cursor: CommentCursor?,
    val hasMore: Boolean,
    val topComments: List<CommentInfo>
)