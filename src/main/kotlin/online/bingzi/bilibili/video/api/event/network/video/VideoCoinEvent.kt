package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent

/**
 * 视频投币事件
 * 当对视频进行投币操作时触发
 *
 * @param aid 视频 AV 号
 * @param bvid BV 号，如果有的话
 * @param multiply 投币数量（1 或 2）
 * @param selectLike 是否同时点赞
 * @param success 操作是否成功
 * @param errorMessage 错误信息，成功时为 null
 */
class VideoCoinEvent(
    val aid: Long,
    val bvid: String? = null,
    val multiply: Int,
    val selectLike: Boolean,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliVideoEvent()