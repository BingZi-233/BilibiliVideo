package online.bingzi.bilibili.video.api.event

import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * Player change bilibili event
 * <p>
 * 玩家变更Bilibili事件(玩家换绑账户事件)
 *
 * @author BingZi-233
 * @since 2.0.0
 * @property player 玩家
 * @property oldMid 旧MID
 * @property newMid 新MID
 * @constructor Create empty Player change bilibili event
 */
class PlayerChangeBilibiliEvent(val player: Player, val oldMid: String, val newMid: String) : BukkitProxyEvent()