package online.bingzi.bilibili.video.internal.network.entity

/**
 * 基本用户信息
 * @param uid 用户 ID
 * @param username 用户名
 * @param avatar 头像 URL
 * @param level 用户等级
 * @param coins 硬币数量
 */
data class UserInfo(
    val uid: Long,
    val username: String,
    val avatar: String?,
    val level: Int,
    val coins: Double
)