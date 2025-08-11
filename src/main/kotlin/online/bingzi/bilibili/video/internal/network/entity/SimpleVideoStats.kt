package online.bingzi.bilibili.video.internal.network.entity

/**
 * 简化的视频统计数据
 * 用于视频列表展示
 *
 * @param view 播放数
 * @param danmaku 弹幕数
 * @param reply 评论数
 * @param favorite 收藏数
 * @param coin 投币数
 * @param share 分享数
 * @param like 点赞数
 */
data class SimpleVideoStats(
    val view: Long,
    val danmaku: Long,
    val reply: Long,
    val favorite: Long,
    val coin: Long,
    val share: Long,
    val like: Long
)