package online.bingzi.bilibili.video.internal.network.entity

/**
 * 一键三连操作结果
 */
data class TripleActionResult(
    val liked: Boolean,    // 点赞是否成功
    val coined: Boolean,   // 投币是否成功
    val favorited: Boolean // 收藏是否成功
)