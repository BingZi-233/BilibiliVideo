package online.bingzi.bilibili.video.internal.network.entity

/**
 * 用户上传视频列表响应
 *
 * @param videos 视频列表
 * @param total 总视频数量
 * @param pages 总页数
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 */
data class UserVideoListResponse(
    val videos: List<UserVideoInfo>,
    val total: Long,
    val pages: Int,
    val currentPage: Int,
    val pageSize: Int
)

