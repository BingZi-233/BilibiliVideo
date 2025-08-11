package online.bingzi.bilibili.video.api.event.network.video

import online.bingzi.bilibili.video.api.event.network.BilibiliVideoEvent
import online.bingzi.bilibili.video.internal.network.entity.TripleActionResult

/**
 * 一键三连操作事件
 * 当执行一键三连操作时触发
 *
 * @param aid 视频 AV 号
 * @param bvid BV 号，如果有的话
 * @param result 三连操作结果
 * @param success 整体操作是否成功（至少一个操作成功）
 */
class VideoTripleActionEvent(
    val aid: Long,
    val bvid: String? = null,
    val result: TripleActionResult,
    val success: Boolean
) : BilibiliVideoEvent()