package online.bingzi.bilibili.video.internal.network.entity

/**
 * 视频统计信息
 */
data class VideoStats(
    val view: Long,      // 播放量
    val danmaku: Long,   // 弹幕数
    val reply: Long,     // 评论数
    val favorite: Long,  // 收藏数
    val coin: Long,      // 投币数
    val share: Long,     // 分享数
    val like: Long       // 点赞数
)