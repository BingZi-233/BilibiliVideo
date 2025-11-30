package online.bingzi.bilibili.video.internal.listener

import online.bingzi.bilibili.video.internal.credential.QrLoginService
import online.bingzi.bilibili.video.internal.ui.VirtualItemSession
import org.bukkit.event.player.PlayerQuitEvent
import taboolib.common.platform.event.SubscribeEvent

/**
 * 玩家事件监听器。
 *
 * 处理玩家下线时的资源清理。
 */
object PlayerListener {

    /**
     * 玩家下线时清理二维码登录会话和虚拟物品会话。
     */
    @SubscribeEvent
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val uuid = player.uniqueId

        // 清理虚拟物品会话
        VirtualItemSession.cleanup(uuid)

        // 取消二维码登录会话
        QrLoginService.cancelLogin(player)
    }
}
