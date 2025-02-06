package online.bingzi.bilibili.video.internal.listener

import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.cache.bvCache
import online.bingzi.bilibili.video.internal.cache.cookieCache
import online.bingzi.bilibili.video.internal.cache.midCache
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * PlayerListener类
 * 玩家事件监听器，用于处理与玩家相关的事件，例如玩家退出游戏时的处理逻辑。
 */
object PlayerListener {
    /**
     * 处理玩家退出事件
     * 当玩家退出游戏时，执行相关的清理和数据保存操作。
     *
     * @param event 玩家退出事件对象，包含与玩家退出相关的信息。
     */
    @SubscribeEvent
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        // 获取退出的玩家对象
        val player = event.player

        // 清除玩家的冷却状态
        baffleCache.reset(player.name)

        // 保存玩家的Cookie数据
        // 从Cookie缓存中获取与玩家唯一ID对应的Cookie数据，如果存在则进行保存操作
        cookieCache.get(player.uniqueId)?.let { cookieData ->
            // 如果sessData存在，则保存到玩家的数据容器中
            cookieData.sessData?.let { sessData -> player.setDataContainer("SESSDATA", sessData) }
            // 如果biliJct存在，则保存到玩家的数据容器中
            cookieData.biliJct?.let { biliJct -> player.setDataContainer("bili_jct", biliJct) }
            // 如果dedeUserID存在，则保存到玩家的数据容器中
            cookieData.dedeUserID?.let { dedeUserID -> player.setDataContainer("DedeUserID", dedeUserID) }
            // 如果dedeUserIDCkMd5存在，则保存到玩家的数据容器中
            cookieData.dedeUserIDCkMd5?.let { dedeUserIDCkMd5 -> player.setDataContainer("DedeUserID__ckMd5", dedeUserIDCkMd5) }
            // 如果sid存在，则保存到玩家的数据容器中
            cookieData.sid?.let { sid -> player.setDataContainer("sid", sid) }
        }

        // 驱逐与该玩家唯一ID对应的Cookie缓存
        cookieCache.invalidate(player.uniqueId)

        // 驱逐与该玩家唯一ID对应的Mid缓存
        midCache.invalidate(player.uniqueId)

        // 驱逐与该玩家唯一ID对应的所有BV缓存
        // 过滤出与玩家唯一ID相关的所有BV缓存进行驱逐
        bvCache.invalidateAll(bvCache.asMap().keys.filter { it.first == player.uniqueId })
    }
}