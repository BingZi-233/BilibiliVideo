package online.bingzi.bilibili.bilibilivideo.internal.reward

import org.bukkit.Material
import org.bukkit.Sound

/**
 * 奖励配置数据类
 * 
 * 表示单个奖励配置项，支持多种奖励类型。
 * 
 * @param type 奖励类型：command, item, message
 * @param command 命令类型奖励的命令内容（支持{player}占位符）
 * @param material 物品类型奖励的材料类型
 * @param amount 物品数量
 * @param displayName 物品显示名称（支持颜色代码）
 * @param lore 物品描述列表（支持颜色代码）
 * @param message 消息类型奖励的消息内容
 * @param description 奖励描述（用于日志记录）
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class RewardData(
    val type: String,
    val command: String? = null,
    val material: String? = null,
    val amount: Int = 1,
    val displayName: String? = null,
    val lore: List<String>? = null,
    val message: String? = null,
    val description: String? = null
) {
    /**
     * 验证奖励配置是否有效
     */
    fun isValid(): Boolean {
        return when (type.lowercase()) {
            "command" -> !command.isNullOrBlank()
            "item" -> {
                if (material.isNullOrBlank()) return false
                try {
                    Material.valueOf(material.uppercase())
                    amount > 0
                } catch (e: IllegalArgumentException) {
                    false
                }
            }
            "message" -> !message.isNullOrBlank()
            else -> false
        }
    }
    
    /**
     * 获取物品材料类型
     */
    fun getMaterialType(): Material? {
        return if (type == "item" && material != null) {
            try {
                Material.valueOf(material.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        } else {
            null
        }
    }
}

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
    val rewards: List<RewardData> = emptyList()
) {
    /**
     * 获取有效的奖励列表
     */
    fun getValidRewards(): List<RewardData> {
        return rewards.filter { it.isValid() }
    }
}

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
    val rewards: List<RewardData> = emptyList()
) {
    /**
     * 获取有效的奖励列表
     */
    fun getValidRewards(): List<RewardData> {
        return rewards.filter { it.isValid() }
    }
}

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