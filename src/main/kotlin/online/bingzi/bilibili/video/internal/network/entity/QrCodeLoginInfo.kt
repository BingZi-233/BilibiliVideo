package online.bingzi.bilibili.video.internal.network.entity

/**
 * 二维码登录信息
 * @param url 二维码内容URL
 * @param qrcodeKey 二维码密钥，用于轮询登录状态
 */
data class QrCodeLoginInfo(
    val url: String,
    val qrcodeKey: String
)