package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * 视频三连状态检查事件
 * 
 * 当检查玩家对某个视频的三连状态（点赞、投币、收藏）时触发此事件。
 * 其他插件可以监听此事件来处理三连相关的逻辑，例如奖励发放或统计记录。
 * 
 * @param player 执行检查的玩家
 * @param tripleData 视频三连数据
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
class VideoTripleStatusCheckEvent(
    val player: Player,
    val tripleData: VideoTripleData
) : BukkitProxyEvent() {
    
    /**
     * 检查是否有三连操作
     * 
     * @return true 如果玩家对该视频进行了任何三连操作（点赞、投币或收藏）
     */
    fun hasTripleAction(): Boolean = tripleData.hasTripleAction()
    
    /**
     * 获取视频BV号
     * 
     * @return 视频的BV号
     */
    fun getBvid(): String = tripleData.bvid
    
    /**
     * 获取用户MID
     * 
     * @return 用户的Bilibili MID
     */
    fun getMid(): Long = tripleData.mid
    
    /**
     * 检查是否已点赞
     * 
     * @return true 如果已点赞
     */
    fun isLiked(): Boolean = tripleData.isLiked
    
    /**
     * 获取投币数量
     * 
     * @return 投币数量（0-2）
     */
    fun getCoinCount(): Int = tripleData.coinCount
    
    /**
     * 检查是否已收藏
     * 
     * @return true 如果已收藏
     */
    fun isFavorited(): Boolean = tripleData.isFavorited
}