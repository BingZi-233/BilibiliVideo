package online.bingzi.bilibili.video.api.event.qrcode

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import taboolib.common.platform.ProxyPlayer
import java.awt.image.BufferedImage

/**
 * 二维码发送模式切换事件
 * 当优先发送模式失败，尝试使用备用模式时触发
 */
class QRCodeSendModeChangeEvent(
    player: ProxyPlayer,
    qrCodeImage: BufferedImage,
    title: String,
    description: String,
    val failedMode: QRCodeSendMode,
    val fallbackMode: QRCodeSendMode,
    val reason: String
) : QRCodeEvent(player, qrCodeImage, title, description) {

    // 此事件不允许被取消
    override val allowCancelled: Boolean
        get() = false
}