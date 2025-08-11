package online.bingzi.bilibili.video.api.event.qrcode

import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent
import java.awt.image.BufferedImage

/**
 * 二维码事件基类
 * 所有二维码相关事件的基础类
 */
abstract class QRCodeEvent(
    val player: ProxyPlayer,
    val qrCodeImage: BufferedImage,
    val title: String,
    val description: String
) : BukkitProxyEvent()