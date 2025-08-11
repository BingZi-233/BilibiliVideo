package online.bingzi.bilibili.video.api.event.qrcode

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import taboolib.common.platform.ProxyPlayer
import java.awt.image.BufferedImage

/**
 * 二维码发送失败事件
 * 当二维码发送失败时触发
 */
class QRCodeSendFailureEvent(
    player: ProxyPlayer,
    qrCodeImage: BufferedImage,
    title: String,
    description: String,
    val attemptedMode: QRCodeSendMode,
    val errorMessage: String?,
    val exception: Throwable?
) : QRCodeEvent(player, qrCodeImage, title, description) {

    // 此事件不允许被取消
    override val allowCancelled: Boolean
        get() = false
}