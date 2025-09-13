package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * UP主关注状态检查事件
 * 
 * 当检查玩家对某个UP主的关注状态时触发此事件。
 * 其他插件可以监听此事件来处理关注相关的逻辑，例如奖励发放或统计记录。
 * 
 * @param player 执行检查的玩家
 * @param followData UP主关注数据
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
class UpFollowStatusCheckEvent(
    val player: Player,
    val followData: UpFollowData
) : BukkitProxyEvent() {
    
    /**
     * 获取UP主MID
     * 
     * @return UP主的Bilibili MID
     */
    fun getUpMid(): Long = followData.upMid
    
    /**
     * 获取UP主名称
     * 
     * @return UP主的用户名
     */
    fun getUpName(): String = followData.upName
    
    /**
     * 获取关注者MID
     * 
     * @return 关注者的Bilibili MID
     */
    fun getFollowerMid(): Long = followData.followerMid
    
    /**
     * 检查是否已关注
     * 
     * @return true 如果已关注该UP主
     */
    fun isFollowing(): Boolean = followData.isFollowing
}