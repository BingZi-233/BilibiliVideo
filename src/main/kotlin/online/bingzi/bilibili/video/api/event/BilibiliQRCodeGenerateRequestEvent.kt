package online.bingzi.bilibili.video.api.event

import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * 请求生成B站二维码事件
 * @param player 触发玩家
 * @param target 目标玩家 (可选, 如为他人生成)
 */
class BilibiliQRCodeGenerateRequestEvent(val player: ProxyPlayer, val target: ProxyPlayer? = null) : BukkitProxyEvent() 