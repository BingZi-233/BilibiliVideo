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

/**
 * 用户详细信息
 * @param uid 用户 ID
 * @param name 用户名
 * @param avatar 头像 URL
 * @param signature 个性签名
 * @param level 用户等级
 * @param gender 性别
 */
data class UserDetailInfo(
    val uid: Long,
    val name: String,
    val avatar: String?,
    val signature: String?,
    val level: Int,
    val gender: String?
)

/**
 * 用户关注统计信息
 * @param uid 用户 ID
 * @param following 关注数
 * @param follower 粉丝数
 */
data class UserStats(
    val uid: Long,
    val following: Long,
    val follower: Long
)