package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论VIP信息
 */
data class CommentVip(
    val vipType: Int,
    val vipDueDate: Long,
    val dueRemark: String,
    val accessStatus: Int,
    val vipStatus: Int,
    val vipStatusWarn: String,
    val themeType: Int,
    val label: CommentVipLabel?,
    val avatarSubscript: Int,
    val nicknameColor: String
)