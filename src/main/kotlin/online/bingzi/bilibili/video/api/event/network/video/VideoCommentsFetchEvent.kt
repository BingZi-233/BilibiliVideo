package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent
import online.bingzi.bilibili.video.internal.network.entity.VideoCommentsResponse

/**
 * 视频评论获取事件
 * 当获取视频评论区数据时触发
 * 
 * @param oid 对象ID（视频AV号或BV号）
 * @param type 评论类型（1=视频）
 * @param page 请求的页码
 * @param pageSize 每页数量
 * @param sort 排序方式
 * @param commentsResponse 评论响应数据，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoCommentsFetchEvent(
    val oid: String, // 可以是AV号或BV号
    val type: Int,
    val page: Int,
    val pageSize: Int,
    val sort: String,
    val commentsResponse: VideoCommentsResponse?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()