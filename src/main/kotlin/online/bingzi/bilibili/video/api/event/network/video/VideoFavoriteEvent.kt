package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent

/**
 * 视频收藏事件
 * 当对视频进行收藏操作时触发
 *
 * @param aid 视频 AV 号
 * @param bvid BV 号，如果有的话
 * @param addMediaIds 添加到的收藏夹 ID 列表
 * @param delMediaIds 从中移除的收藏夹 ID 列表
 * @param success 操作是否成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoFavoriteEvent(
    val aid: Long,
    val bvid: String? = null,
    val addMediaIds: List<Long>,
    val delMediaIds: List<Long>,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()