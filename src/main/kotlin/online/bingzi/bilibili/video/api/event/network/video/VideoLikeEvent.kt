package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent

/**
 * 视频点赞事件
 * 当对视频进行点赞操作时触发
 *
 * @param aid 视频 AV 号
 * @param bvid BV 号，如果有的话
 * @param like true 为点赞，false 为取消点赞
 * @param success 操作是否成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoLikeEvent(
    val aid: Long,
    val bvid: String? = null,
    val like: Boolean,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()