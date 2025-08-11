package online.bingzi.bilibili.video.internal.network.entity

/**
 * UP主信息
 */
data class UploaderInfo(
    val uid: Long,        // UID
    val name: String,     // 用户名
    val avatar: String?   // 头像
)