package online.bingzi.bilibili.video.api.event.reward

import online.bingzi.bilibili.video.internal.database.entity.VideoRewardRecord
import taboolib.platform.type.BukkitProxyEvent

/**
 * 奖励领取事件
 * 在玩家成功或失败领取奖励时触发
 */
class RewardClaimEvent(
    /**
     * 玩家UUID
     */
    val playerUuid: String,
    
    /**
     * 视频BV号
     */
    val bvId: String,
    
    /**
     * UP主UID
     */
    val uploaderUid: Long,
    
    /**
     * 是否成功
     */
    val success: Boolean,
    
    /**
     * 奖励记录（成功时）
     */
    val rewardRecord: VideoRewardRecord? = null,
    
    /**
     * 错误信息（失败时）
     */
    val errorMessage: String? = null,
    
    /**
     * 奖励内容描述
     */
    val rewardContent: String? = null,
    
    /**
     * 事件时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
) : BukkitProxyEvent() {

    /**
     * 是否是成功事件
     */
    fun isSuccess(): Boolean = success

    /**
     * 是否是失败事件
     */
    fun isFailure(): Boolean = !success

    /**
     * 获取事件描述
     */
    fun getDescription(): String {
        return if (success) {
            "玩家 $playerUuid 成功领取视频 $bvId 的奖励"
        } else {
            "玩家 $playerUuid 领取视频 $bvId 的奖励失败：${errorMessage ?: "未知错误"}"
        }
    }
}