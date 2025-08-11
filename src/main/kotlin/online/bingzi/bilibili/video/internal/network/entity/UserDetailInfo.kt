package online.bingzi.bilibili.video.internal.network.entity

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