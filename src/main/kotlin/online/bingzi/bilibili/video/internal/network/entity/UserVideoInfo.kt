package online.bingzi.bilibili.video.internal.network.entity

/**
 * 用户视频信息（简化版本）
 * 专门用于视频列表展示
 *
 * @param aid 视频AV号
 * @param bvid 视频BV号
 * @param title 视频标题
 * @param description 视频描述
 * @param cover 封面图片URL
 * @param duration 视频时长（秒）
 * @param publishTime 发布时间戳
 * @param videoStats 视频统计数据
 * @param author 作者信息（UID和用户名）
 */
data class UserVideoInfo(
    val aid: Long,
    val bvid: String,
    val title: String,
    val description: String?,
    val cover: String?,
    val duration: Int?,
    val publishTime: Long?,
    val videoStats: SimpleVideoStats,
    val author: VideoAuthor
)