package online.bingzi.bilibili.video.internal.network.entity

/**
 * 一键三连状态
 */
data class TripleActionStatus(
    val aid: Long,         // 视频 AV 号
    val liked: Boolean,    // 是否已点赞
    val coined: Boolean,   // 是否已投币
    val favorited: Boolean // 是否已收藏
)