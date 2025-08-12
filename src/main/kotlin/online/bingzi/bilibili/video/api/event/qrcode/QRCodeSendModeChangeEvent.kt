package online.bingzi.bilibili.video.api.event.qrcode

import taboolib.common.platform.ProxyPlayer
import java.awt.image.BufferedImage

/**
 * 二维码发送器切换事件
 * 当优先发送器失败，尝试使用备用发送器时触发
 */
class QRCodeSendModeChangeEvent(
    player: ProxyPlayer,
    qrCodeImage: BufferedImage,
    title: String,
    description: String,
    val failedSenderName: String,
    val fallbackSenderName: String,
    val reason: String
) : QRCodeEvent(player, qrCodeImage, title, description) {

    // 此事件不允许被取消
    override val allowCancelled: Boolean
        get() = false
}