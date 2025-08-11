package online.bingzi.bilibili.video.internal.network.entity

/**
 * 登录会话数据类
 */
data class LoginSession(
    val playerUuid: String,
    val playerName: String,
    val qrcodeKey: String,
    val startTime: Long
)