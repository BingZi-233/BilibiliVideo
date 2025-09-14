package online.bingzi.bilibili.bilibilivideo.internal.manager

import online.bingzi.bilibili.bilibilivideo.internal.config.SettingConfig
import online.bingzi.bilibili.bilibilivideo.internal.reward.DefaultRewardConfig
import online.bingzi.bilibili.bilibilivideo.internal.reward.VideoRewardConfig

/**
 * BV号管理类
 * 
 * 负责管理配置文件中的BV号列表，提供BV号相关的查询和管理功能。
 * 主要用于命令补全和奖励配置获取。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object BvManager {
    
    /**
     * 获取所有配置的BV号列表
     * 
     * 从配置文件中获取所有已配置的BV号，用于命令补全功能。
     * 
     * @return List<String> BV号列表，如果没有配置则返回空列表
     */
    fun getAllBvids(): List<String> {
        return try {
            SettingConfig.getAllConfiguredBvids()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 获取启用的BV号列表
     * 
     * 只返回在配置中启用的BV号，过滤掉被禁用的视频。
     * 
     * @return List<String> 启用的BV号列表
     */
    fun getEnabledBvids(): List<String> {
        return getAllBvids().filter { bvid ->
            val config = SettingConfig.getVideoRewardConfig(bvid)
            config?.enabled ?: false
        }
    }
    
    /**
     * 检查BV号是否已配置
     * 
     * @param bvid BV号
     * @return Boolean 是否在配置文件中存在
     */
    fun isConfigured(bvid: String): Boolean {
        return SettingConfig.isVideoConfigured(bvid)
    }
    
    /**
     * 获取BV号的奖励配置
     * 
     * 如果BV号有专门配置，返回该配置；否则返回默认配置。
     * 
     * @param bvid BV号
     * @return Pair<VideoRewardConfig?, DefaultRewardConfig?> 
     *         第一个元素是特定视频配置，第二个元素是默认配置
     */
    fun getRewardConfig(bvid: String): Pair<VideoRewardConfig?, DefaultRewardConfig?> {
        return SettingConfig.getRewardConfigForBvid(bvid)
    }
    
    /**
     * 获取BV号的显示名称
     * 
     * @param bvid BV号
     * @return String 视频显示名称，如果未配置则返回BV号本身
     */
    fun getVideoName(bvid: String): String {
        val config = SettingConfig.getVideoRewardConfig(bvid)
        return config?.name ?: bvid
    }
    
    /**
     * 检查BV号是否启用奖励
     * 
     * @param bvid BV号
     * @return Boolean 是否启用奖励
     */
    fun isRewardEnabled(bvid: String): Boolean {
        val videoConfig = SettingConfig.getVideoRewardConfig(bvid)
        if (videoConfig != null) {
            return videoConfig.enabled
        }
        
        // 如果没有特定配置，检查默认配置
        val defaultConfig = SettingConfig.getDefaultRewardConfig()
        return defaultConfig.enabled
    }
    
    /**
     * 检查BV号是否需要完整三连
     * 
     * @param bvid BV号
     * @return Boolean 是否需要完整三连（点赞+投币+收藏）
     */
    fun requiresCompleteTriple(bvid: String): Boolean {
        val videoConfig = SettingConfig.getVideoRewardConfig(bvid)
        if (videoConfig != null) {
            return videoConfig.requireCompleteTriple
        }
        
        // 如果没有特定配置，检查默认配置
        val defaultConfig = SettingConfig.getDefaultRewardConfig()
        return defaultConfig.requireCompleteTriple
    }
    
    /**
     * 获取有奖励的BV号数量
     * 
     * @return Int 配置了奖励的BV号数量
     */
    fun getRewardEnabledBvidCount(): Int {
        return getAllBvids().count { isRewardEnabled(it) }
    }
    
    /**
     * 验证BV号格式是否正确
     * 
     * @param bvid BV号字符串
     * @return Boolean 是否为有效的BV号格式
     */
    fun isValidBvid(bvid: String): Boolean {
        // BV号格式：BV + 10位大小写字母和数字（不包含0、I、O、l）
        return bvid.matches(Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$"))
    }
    
    /**
     * 添加动态BV号到建议列表
     * 
     * 当玩家对未配置的BV号执行三连后，可以将其添加到建议列表中。
     * 这个功能可以帮助管理员了解哪些视频需要配置奖励。
     * 
     * @param bvid BV号
     * @param playerName 执行三连的玩家名
     */
    fun addDynamicBvid(bvid: String, playerName: String) {
        // 这里可以实现动态BV号记录功能
        // 例如记录到文件或数据库中，供管理员参考
        // 目前先预留接口
    }
}