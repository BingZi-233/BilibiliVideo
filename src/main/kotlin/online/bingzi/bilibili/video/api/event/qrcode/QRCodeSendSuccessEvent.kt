package online.bingzi.bilibili.video.api.event.qrcode

import taboolib.common.platform.ProxyPlayer
import java.awt.image.BufferedImage

/**
 * 二维码发送成功事件
 * 当二维码成功发送给玩家后触发
 */
class QRCodeSendSuccessEvent(
    player: ProxyPlayer,
    qrCodeImage: BufferedImage,
    title: String,
    description: String,
    val senderName: String,
    val sendDuration: Long // 发送耗时（毫秒）
) : QRCodeEvent(player, qrCodeImage, title, description) {

    // 此事件不允许被取消
    override val allowCancelled: Boolean
        get() = false
}