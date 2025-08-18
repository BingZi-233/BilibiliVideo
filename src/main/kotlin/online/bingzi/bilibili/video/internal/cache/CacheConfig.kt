package online.bingzi.bilibili.video.internal.cache

import taboolib.common.platform.function.console
import taboolib.module.configuration.Configuration
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 缓存配置类
 * 管理QQ绑定缓存系统的相关配置
 */
object CacheConfig {
    
    private lateinit var config: Configuration
    
    /**
     * 初始化缓存配置
     */
    fun initialize(configuration: Configuration) {
        config = configuration
        console().sendInfo("qqBindingCacheConfigLoaded")
    }
    
    /**
     * 是否启用缓存
     */
    val enabled: Boolean
        get() = config.getBoolean("binding.cache.enabled", true)
    
    /**
     * 缓存TTL（秒）
     */
    val ttl: Long
        get() = config.getLong("binding.cache.ttl", 3600)
    
    /**
     * 最大缓存条目数
     */
    val maxSize: Int
        get() = config.getInt("binding.cache.maxSize", 10000)
    
    /**
     * 启动时是否预加载缓存
     */
    val preloadOnStartup: Boolean
        get() = config.getBoolean("binding.cache.preloadOnStartup", true)
    
    /**
     * 验证码长度
     */
    val verificationCodeLength: Int
        get() = config.getInt("binding.verification.codeLength", 6)
    
    /**
     * 验证码过期时间（分钟）
     */
    val verificationExpireMinutes: Int
        get() = config.getInt("binding.verification.expireMinutes", 5)
    
    /**
     * 最大验证尝试次数
     */
    val maxVerificationAttempts: Int
        get() = config.getInt("binding.verification.maxAttempts", 3)
    
    /**
     * 验证配置是否有效
     */
    fun validateConfig(): Boolean {
        if (ttl <= 0) {
            console().sendWarn("qqBindingCacheConfigInvalidTtl", ttl.toString())
            return false
        }
        
        if (maxSize <= 0) {
            console().sendWarn("qqBindingCacheConfigInvalidMaxSize", maxSize.toString())
            return false
        }
        
        if (verificationCodeLength < 4 || verificationCodeLength > 10) {
            console().sendWarn("qqBindingCacheConfigInvalidCodeLength", verificationCodeLength.toString())
            return false
        }
        
        if (verificationExpireMinutes <= 0) {
            console().sendWarn("qqBindingCacheConfigInvalidExpire", verificationExpireMinutes.toString())
            return false
        }
        
        return true
    }
    
    /**
     * 打印配置信息
     */
    fun printConfig() {
        console().sendInfo("qqBindingCacheConfigInfo", 
            enabled.toString(),
            ttl.toString(), 
            maxSize.toString(),
            preloadOnStartup.toString(),
            verificationCodeLength.toString(),
            verificationExpireMinutes.toString(),
            maxVerificationAttempts.toString()
        )
    }
}