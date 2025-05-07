package online.bingzi.bilibili.video.api.event

import online.bingzi.bilibili.video.internal.entity.CookieData
import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * B站二维码扫描状态更新事件
 * @param player 触发玩家 (通常是扫码者)
 * @param target 目标玩家 (二维码对应的玩家)
 * @param qrCodeKey 当前二维码的key
 * @param scanStatusCode B站返回的扫描状态码 (0: 成功, 86038: 超时, 86090: 待扫描, 86101: 未确认, 等)
 * @param cookieData 扫码成功时获取到的Cookie数据
 * @param refreshToken 扫码成功时获取到的refreshToken
 * @param timestamp 扫码成功时获取到的时间戳
 * @param message 额外信息 (例如错误描述或状态描述)
 */
class BilibiliQRCodeScanUpdateEvent(
    val player: ProxyPlayer,
    val target: ProxyPlayer?,
    val qrCodeKey: String,
    val scanStatusCode: Int,
    val cookieData: CookieData? = null,
    val refreshToken: String? = null,
    val timestamp: Long? = null,
    val message: String? = null
) : BukkitProxyEvent() 