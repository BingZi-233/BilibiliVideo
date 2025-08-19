package online.bingzi.bilibili.video.internal.cache

/**
 * 缓存键统一管理
 * 提供类型安全的缓存键定义，避免硬编码字符串常量
 * 
 * 主要功能：
 * - 统一的缓存键命名规范
 * - 参数化的缓存键生成
 * - 避免键名冲突和拼写错误
 */
enum class CacheKeys(private val pattern: String) {
    
    // QQ相关缓存键
    RECENT_QQ_NUMBERS("recent_qq_numbers"),
    ACTIVE_QQ_BINDINGS("active_qq_bindings"),
    
    // UP主相关缓存键  
    POPULAR_UPLOADER_UIDS("popular_uploader_uids"),
    MONITORED_UPLOADER_UIDS("monitored_uploader_uids"),
    
    // Bilibili用户相关缓存键
    ACTIVE_BILIBILI_UIDS("active_bilibili_uids"),
    
    // 视频相关缓存键（需要参数的使用key函数）
    RECENT_BV_IDS("recent_bv_ids_{playerUuid}"),
    
    // 命令建议相关缓存键
    COMMAND_SUGGESTIONS("command_suggestions_{type}"),
    
    // 奖励系统相关缓存键  
    REWARD_ELIGIBLE_VIDEOS("reward_eligible_videos_{playerUuid}"),
    PLAYER_REWARD_STATS("player_reward_stats_{playerUuid}");
    
    /**
     * 获取不带参数的缓存键
     */
    fun key(): String = pattern
    
    /**
     * 获取带参数的缓存键
     * 
     * @param params 参数映射，键为占位符名称（不含大括号），值为实际值
     * @return 替换参数后的缓存键
     */
    fun key(vararg params: Pair<String, String>): String {
        var result = pattern
        params.forEach { (placeholder, value) ->
            result = result.replace("{$placeholder}", value)
        }
        return result
    }
    
    /**
     * 获取带单个参数的缓存键（常用的便捷方法）
     * 
     * @param value 参数值
     * @return 替换参数后的缓存键
     */
    fun key(value: String): String {
        // 自动检测第一个占位符并替换
        val placeholderRegex = "\\{([^}]+)}".toRegex()
        val match = placeholderRegex.find(pattern)
        
        return if (match != null) {
            val placeholder = match.groupValues[1]
            key(placeholder to value)
        } else {
            pattern
        }
    }
    
    companion object {
        /**
         * 根据字符串查找对应的缓存键枚举
         * 用于迁移和兼容旧代码
         * 
         * @param keyString 缓存键字符串
         * @return 匹配的枚举值，如果未找到则返回null
         */
        fun findByKey(keyString: String): CacheKeys? {
            return values().find { cacheKey ->
                // 精确匹配
                if (cacheKey.pattern == keyString) return cacheKey
                
                // 模式匹配（对于含有参数的键）
                val regexPattern = cacheKey.pattern.replace(Regex("\\{[^}]+}"), ".*")
                if (keyString.matches(Regex(regexPattern))) return cacheKey
                
                false
            }
        }
        
        /**
         * 检查缓存键是否有效
         * 
         * @param keyString 要检查的缓存键字符串
         * @return 是否为有效的已定义缓存键
         */
        fun isValidKey(keyString: String): Boolean {
            return findByKey(keyString) != null
        }
    }
}