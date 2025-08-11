package online.bingzi.bilibili.video.api.event.qrcode

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent
import java.awt.image.BufferedImage

/**
 * 二维码发送前事件
 * 在尝试发送二维码之前触发，可以被取消
 */
class QRCodePreSendEvent(
    player: ProxyPlayer,
    qrCodeImage: BufferedImage,
    title: String,
    description: String,
    val sendMode: QRCodeSendMode
) : QRCodeEvent(player, qrCodeImage, title, description)