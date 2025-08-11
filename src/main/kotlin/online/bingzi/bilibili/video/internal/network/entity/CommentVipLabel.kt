package online.bingzi.bilibili.video.internal.network.entity

/**
 * VIP标签信息
 */
data class CommentVipLabel(
    val path: String,
    val text: String,
    val labelTheme: String,
    val textColor: String,
    val bgStyle: Int,
    val bgColor: String,
    val borderColor: String,
    val useImgLabel: Boolean,
    val imgLabelUriHans: String,
    val imgLabelUriHant: String,
    val imgLabelUriHansStatic: String,
    val imgLabelUriHantStatic: String
)