package online.bingzi.bilibili.video.internal.network.entity

/**
 * 评论内容
 *
 * @param message 评论文本
 * @param plat 平台
 * @param device 设备
 * @param members 提到的用户列表
 * @param emote 表情信息
 * @param jumpUrl 跳转链接信息
 * @param maxLine 最大行数
 */
data class CommentContent(
    val message: String,
    val plat: Int,
    val device: String,
    val members: List<CommentMentionedMember>?,
    val emote: Map<String, CommentEmote>?,
    val jumpUrl: Map<String, CommentJumpUrl>?,
    val maxLine: Int
)