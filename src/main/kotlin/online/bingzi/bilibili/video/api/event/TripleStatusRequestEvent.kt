package online.bingzi.bilibili.video.api.event

import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * 请求获取视频三连状态事件
 * @param player 玩家
 * @param bvid 视频BV号
 */
class TripleStatusRequestEvent(val player: ProxyPlayer, val bvid: String) : BukkitProxyEvent() 