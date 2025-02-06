package online.bingzi.bilibili.video.internal.listener

import online.bingzi.bilibili.video.internal.config.VideoConfig
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.helper.ketherEval
import taboolib.common.platform.event.SubscribeEvent

// TripleSendRewardsListener 是一个对象，用于监听三重发送奖励事件
// 该对象实现了事件订阅，并处理与三重发送奖励相关的逻辑
object TripleSendRewardsListener {

    // 当三重发送奖励事件发生时触发该方法
    // @param event 触发的三重发送奖励事件，包含事件相关的信息，例如 bvid 和玩家信息
    // 该方法执行以下操作：
    // 1. 调用 VideoConfig 中的 receiveMap 对象，使用事件中的 bvid 作为键值，执行 ketherEval 方法
    // 2. 将事件中玩家的 bvid 数据保存到数据库中
    // 3. 将玩家的唯一标识符和 bvid 存入缓存中
    @SubscribeEvent
    fun onTripleSendRewardsEvent(event: online.bingzi.bilibili.video.api.event.TripleSendRewardsEvent) {
        // 从 VideoConfig 中的 receiveMap 获取 bvid 对应的值，并执行 ketherEval 方法
        // 该方法可能用于处理奖励相关的逻辑
        VideoConfig.receiveMap[event.bvid]?.ketherEval(event.player)

        // 完成数据保存
        // 将当前玩家的 bvid 数据标记为已接收，并存储到数据容器中
        event.player.setDataContainer(event.bvid, true.toString())

        // 将玩家的唯一标识符和 bvid 作为键值对放入缓存中，标记为已完成
        online.bingzi.bilibili.video.internal.cache.bvCache.put(event.player.uniqueId to event.bvid, true)
    }
}