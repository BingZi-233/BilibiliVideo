package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import taboolib.platform.type.BukkitProxyEvent

class UpFollowStatusCheckEvent(
    val player: Player,
    val followData: UpFollowData
) : BukkitProxyEvent(), Cancellable {
    
    private var cancelled = false
    
    override fun isCancelled(): Boolean = cancelled
    
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
    
    fun getUpMid(): Long = followData.upMid
    
    fun getUpName(): String = followData.upName
    
    fun getFollowerMid(): Long = followData.followerMid
    
    fun isFollowing(): Boolean = followData.isFollowing
}