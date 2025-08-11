package online.bingzi.bilibili.video.internal.network.entity

/**
 * 视频作者信息（简化版本）
 *
 * @param uid 用户UID
 * @param name 用户名
 * @param avatar 头像URL
 */
data class VideoAuthor(
    val uid: Long,
    val name: String,
    val avatar: String?
)