package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent
import online.bingzi.bilibili.video.internal.network.entity.TripleActionStatus

/**
 * 视频三连状态获取事件
 * 当获取视频的点赞、投币、收藏状态时触发
 *
 * @param aid 视频 AV 号
 * @param bvid BV 号，如果有的话
 * @param tripleStatus 三连状态，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoTripleStatusFetchEvent(
    val aid: Long,
    val bvid: String? = null,
    val tripleStatus: TripleActionStatus?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()