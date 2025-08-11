package online.bingzi.bilibili.video.internal.network.entity

/**
 * 精简版视频评论响应
 * 只保留必要的评论数据
 * 
 * @param comments 评论列表
 * @param total 评论总数
 * @param currentPage 当前页码
 * @param hasMore 是否还有更多评论
 */
data class SimpleCommentsResponse(
    val comments: List<SimpleComment>,
    val total: Long,
    val currentPage: Int,
    val hasMore: Boolean
)