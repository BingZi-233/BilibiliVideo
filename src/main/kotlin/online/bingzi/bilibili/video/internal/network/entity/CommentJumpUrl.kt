package online.bingzi.bilibili.video.internal.network.entity

/**
 * 跳转链接
 */
data class CommentJumpUrl(
    val title: String,
    val state: Int,
    val prefixIcon: String,
    val appUrlSchema: String,
    val appName: String,
    val appPackageName: String,
    val clickReport: String
)