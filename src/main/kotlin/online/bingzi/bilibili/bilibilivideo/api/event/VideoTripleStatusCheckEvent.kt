package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

class VideoTripleStatusCheckEvent(
    val player: Player,
    val tripleData: VideoTripleData
) : BukkitProxyEvent() {
    
    fun hasTripleAction(): Boolean = tripleData.hasTripleAction()
    
    fun getBvid(): String = tripleData.bvid
    
    fun getMid(): Long = tripleData.mid
    
    fun isLiked(): Boolean = tripleData.isLiked
    
    fun getCoinCount(): Int = tripleData.coinCount
    
    fun isFavorited(): Boolean = tripleData.isFavorited
}