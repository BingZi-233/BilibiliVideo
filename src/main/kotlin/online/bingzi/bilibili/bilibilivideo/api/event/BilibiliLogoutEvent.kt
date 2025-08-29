package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import taboolib.platform.type.BukkitProxyEvent

class BilibiliLogoutEvent(
    val player: Player,
    val previousSession: LoginSession
) : BukkitProxyEvent(), Cancellable {
    
    private var cancelled = false
    
    override fun isCancelled(): Boolean = cancelled
    
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
    
    fun getMid(): Long = previousSession.mid
    
    fun getNickname(): String = previousSession.nickname
    
    fun getSessionDuration(): Long = 
        System.currentTimeMillis() - previousSession.loginTime
}