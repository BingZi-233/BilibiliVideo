package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent
import online.bingzi.bilibili.video.internal.network.entity.VideoInfo

/**
 * 视频信息获取事件
 * 当获取视频信息时触发
 *
 * @param bvid BV 号
 * @param videoInfo 视频信息，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoInfoFetchEvent(
    val bvid: String,
    val videoInfo: VideoInfo?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()