package online.bingzi.bilibili.video.api.event.network.login

import online.bingzi.bilibili.video.api.event.network.BilibiliLoginEvent
import online.bingzi.bilibili.video.internal.network.entity.QrCodeLoginInfo

/**
 * 二维码生成事件
 * 当生成新的登录二维码时触发
 *
 * @param qrCodeInfo 二维码信息，如果生成失败则为 null
 * @param success 是否生成成功
 * @param errorMessage 错误信息，成功时为 null
 */
class QrCodeGenerateEvent(
    val qrCodeInfo: QrCodeLoginInfo?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliLoginEvent()