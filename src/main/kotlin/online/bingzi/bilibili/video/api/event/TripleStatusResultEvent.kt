package online.bingzi.bilibili.video.api.event

import online.bingzi.bilibili.video.internal.entity.TripleData
import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * 获取视频三连状态结果事件
 * @param player 玩家
 * @param bvid 视频BV号
 * @param tripleData 三连数据，成功时且有数据时包含
 * @param isSuccess 操作是否成功 (基于HTTP响应和B站业务code)
 * @param code B站返回的业务code (-404, 0, -101, 10003等)
 * @param errorMessage 错误信息
 */
class TripleStatusResultEvent(
    val player: ProxyPlayer,
    val bvid: String,
    val tripleData: TripleData?,
    val isSuccess: Boolean,
    val code: Int?,
    val errorMessage: String? = null
) : BukkitProxyEvent() 