package online.bingzi.bilibili.bilibilivideo.internal.config

import online.bingzi.bilibili.bilibilivideo.internal.reward.DefaultRewardConfig
import online.bingzi.bilibili.bilibilivideo.internal.reward.RewardSettings
import online.bingzi.bilibili.bilibilivideo.internal.reward.VideoRewardConfig
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 设置配置管理类
 * 
 * 负责管理setting.yml配置文件，包括默认奖励配置和特定BV号奖励配置。
 * 使用TabooLib Configuration模块自动管理配置文件。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object SettingConfig {
    
    /** 配置文件实例 */
    @Config("setting.yml")
    lateinit var config: Configuration
        private set

    /**
     * 获取默认奖励配置
     * 
     * @return DefaultRewardConfig 默认奖励配置对象
     */
    fun getDefaultRewardConfig(): DefaultRewardConfig {
        val section = config.getConfigurationSection("default-reward") ?: return DefaultRewardConfig()
        
        val enabled = section.getBoolean("enabled", true)
        val requireCompleteTriple = section.getBoolean("require-complete-triple", true)
        val rewardsList = section.getStringList("rewards")
        
        val rewards = rewardsList
        
        return DefaultRewardConfig(
            enabled = enabled,
            requireCompleteTriple = requireCompleteTriple,
            rewards = rewards
        )
    }
    
    /**
     * 获取视频奖励配置
     * 
     * @param bvid BV号
     * @return VideoRewardConfig? 视频奖励配置对象，如果未配置则返回null
     */
    fun getVideoRewardConfig(bvid: String): VideoRewardConfig? {
        val videosSection = config.getConfigurationSection("videos") ?: return null
        val videoSection = videosSection.getConfigurationSection(bvid) ?: return null
        
        val name = videoSection.getString("name") ?: "未命名视频"
        val enabled = videoSection.getBoolean("enabled", true)
        val requireCompleteTriple = videoSection.getBoolean("require-complete-triple", true)
        val rewardsList = videoSection.getStringList("rewards")
        
        val rewards = rewardsList
        
        return VideoRewardConfig(
            name = name,
            enabled = enabled,
            requireCompleteTriple = requireCompleteTriple,
            rewards = rewards
        )
    }
    
    /**
     * 获取所有配置的BV号列表
     * 
     * @return List<String> BV号列表
     */
    fun getAllConfiguredBvids(): List<String> {
        val videosSection = config.getConfigurationSection("videos") ?: return emptyList()
        return videosSection.getKeys(false).toList()
    }
    
    /**
     * 获取奖励系统设置
     * 
     * @return RewardSettings 奖励系统设置对象
     */
    fun getRewardSettings(): RewardSettings {
        val section = config.getConfigurationSection("settings") ?: return RewardSettings()
        
        return RewardSettings(
            preventDuplicateRewards = section.getBoolean("prevent-duplicate-rewards", true),
            rewardDelay = section.getLong("reward-delay", 1000L),
            playSound = section.getBoolean("play-sound", true),
            soundType = section.getString("sound-type", "ENTITY_PLAYER_LEVELUP") ?: "ENTITY_PLAYER_LEVELUP",
            soundVolume = section.getDouble("sound-volume", 1.0).toFloat(),
            soundPitch = section.getDouble("sound-pitch", 1.0).toFloat()
        )
    }
    
    /**
     * 检查BV号是否已配置
     * 
     * @param bvid BV号
     * @return Boolean 是否已配置
     */
    fun isVideoConfigured(bvid: String): Boolean {
        val videosSection = config.getConfigurationSection("videos") ?: return false
        return videosSection.contains(bvid)
    }
    
    /**
     * 获取BV号对应的奖励配置
     * 如果未配置特定BV号，返回默认奖励配置
     * 
     * @param bvid BV号
     * @return Pair<VideoRewardConfig?, DefaultRewardConfig?> 
     *         第一个元素是特定视频配置，第二个元素是默认配置
     */
    fun getRewardConfigForBvid(bvid: String): Pair<VideoRewardConfig?, DefaultRewardConfig?> {
        val videoConfig = getVideoRewardConfig(bvid)
        val defaultConfig = if (videoConfig == null) getDefaultRewardConfig() else null
        
        return Pair(videoConfig, defaultConfig)
    }
    
    /**
     * 重载配置文件
     */
    fun reloadConfig() {
        config.reload()
    }
}