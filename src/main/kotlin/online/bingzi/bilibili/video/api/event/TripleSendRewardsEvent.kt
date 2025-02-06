package online.bingzi.bilibili.video.api.event

import taboolib.common.platform.ProxyPlayer
import taboolib.platform.type.BukkitProxyEvent

/**
 * 三连奖励发放事件类
 * 该类用于表示在用户触发三连奖励时的事件，包含了相关的玩家信息和视频标识符。
 *
 * @property player 参与该事件的玩家，类型为 ProxyPlayer。
 * @property bvid 参与三连奖励的视频的 BV 号，类型为 String。
 * @constructor 创建一个空的三连奖励发放事件实例。
 */
class TripleSendRewardsEvent(val player: ProxyPlayer, val bvid: String) : BukkitProxyEvent() {
    // 此类继承自 BukkitProxyEvent，表示这是一个与 Bukkit 平台相关的事件
}