package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class BilibiliLoginEvent(
    val player: Player,
    val session: LoginSession
) : BukkitProxyEvent() {
    
    fun getMid(): Long = session.mid
    
    fun getNickname(): String = session.nickname
    
    fun getLoginTime(): Long = session.loginTime
}