package online.bingzi.bilibili.video.api.event

import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * Show模式获取视频三连状态结果事件
 * @param player 玩家
 * @param bvid 视频BV号
 * @param isSuccess 操作是否成功
 * @param errorMessage 错误信息 (可选)
 */
class TripleStatusShowResultEvent(
    val player: ProxyPlayer,
    val bvid: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
) : BukkitProxyEvent() 