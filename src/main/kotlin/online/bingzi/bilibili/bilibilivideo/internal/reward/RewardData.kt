package online.bingzi.bilibili.bilibilivideo.internal.reward

import org.bukkit.Sound


/**
 * 视频奖励配置数据类
 * 
 * 表示单个视频的完整奖励配置。
 * 
 * @param name 视频显示名称
 * @param enabled 是否启用此视频的奖励
 * @param requireCompleteTriple 是否需要完整三连才能获得奖励
 * @param rewards 奖励列表
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class VideoRewardConfig(
    val name: String = "未命名视频",
    val enabled: Boolean = true,
    val requireCompleteTriple: Boolean = true,
    val rewards: List<String> = emptyList()
)

/**
 * 默认奖励配置数据类
 * 
 * @param enabled 是否启用默认奖励
 * @param requireCompleteTriple 是否需要完整三连才能获得奖励
 * @param rewards 默认奖励列表
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class DefaultRewardConfig(
    val enabled: Boolean = true,
    val requireCompleteTriple: Boolean = true,
    val rewards: List<String> = emptyList()
)

/**
 * 奖励系统设置数据类
 * 
 * @param preventDuplicateRewards 是否阻止重复领取奖励
 * @param rewardDelay 奖励发放延迟（毫秒）
 * @param playSound 是否播放音效
 * @param soundType 音效类型
 * @param soundVolume 音效音量
 * @param soundPitch 音效音调
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class RewardSettings(
    val preventDuplicateRewards: Boolean = true,
    val rewardDelay: Long = 1000L,
    val playSound: Boolean = true,
    val soundType: String = "ENTITY_PLAYER_LEVELUP",
    val soundVolume: Float = 1.0f,
    val soundPitch: Float = 1.0f
) {
    /**
     * 获取音效类型
     */
    fun getSoundType(): Sound? {
        return try {
            Sound.valueOf(soundType.uppercase())
        } catch (e: IllegalArgumentException) {
            Sound.ENTITY_PLAYER_LEVELUP
        }
    }
}