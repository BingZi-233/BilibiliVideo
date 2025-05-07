package online.bingzi.bilibili.video.api.event

import online.bingzi.bilibili.video.internal.entity.QRCodeGenerateData
import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * B站二维码生成结果事件
 * @param player 触发玩家
 * @param target 目标玩家
 * @param qrCodeGenerateData 二维码生成数据，成功时包含
 * @param isSuccess 操作是否成功
 * @param errorMessage 错误信息，失败时包含
 */
class BilibiliQRCodeGeneratedResultEvent(
    val player: ProxyPlayer,
    val target: ProxyPlayer?,
    val qrCodeGenerateData: QRCodeGenerateData?,
    val isSuccess: Boolean,
    val errorMessage: String? = null
) : BukkitProxyEvent() 