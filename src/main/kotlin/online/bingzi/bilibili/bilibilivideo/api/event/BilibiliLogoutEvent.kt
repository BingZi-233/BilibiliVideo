package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class BilibiliLogoutEvent(
    val player: Player,
    val previousSession: LoginSession
) : BukkitProxyEvent() {
    
    fun getMid(): Long = previousSession.mid
    
    fun getNickname(): String = previousSession.nickname
    
    fun getSessionDuration(): Long = 
        System.currentTimeMillis() - previousSession.loginTime
}