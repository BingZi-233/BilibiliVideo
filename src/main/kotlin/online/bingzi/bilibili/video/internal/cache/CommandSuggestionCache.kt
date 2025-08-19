package online.bingzi.bilibili.video.internal.cache

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 命令建议缓存系统
 * 提供基于TTL的缓存机制，支持异步数据获取和优雅降级
 * 
 * 主要功能：
 * - TTL缓存机制，自动过期清理
 * - 线程安全的并发访问
 * - 统一的缓存管理API
 * - 支持命令参数建议的快速响应
 */
@Awake
object CommandSuggestionCache {
    
    /**
     * 配置文件
     */
    @Config("config.yml")
    lateinit var config: Configuration
    
    /**
     * 缓存TTL时间（分钟）- 从配置文件读取
     */
    private val ttlMinutes: Long
        get() = config.getLong("command-suggestion.cache-ttl-minutes", 5L)
    
    /**
     * 内部缓存存储
     */
    private val cache = ConcurrentHashMap<String, CachedSuggestion>()
    
    /**
     * 缓存项数据结构
     * 包含建议数据和时间戳用于TTL检查
     */
    data class CachedSuggestion(
        val data: List<String>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * 检查缓存是否已过期
         */
        fun isExpired(): Boolean = 
            System.currentTimeMillis() - timestamp > TimeUnit.MINUTES.toMillis(ttlMinutes)
    }
    
    /**
     * 获取缓存的建议数据
     * 
     * @param key 缓存键
     * @return 建议列表，如果不存在或已过期则返回null
     */
    fun get(key: String): List<String>? {
        val cached = cache[key] ?: return null
        
        return if (cached.isExpired()) {
            // 自动清理过期缓存
            cache.remove(key)
            null
        } else {
            cached.data
        }
    }
    
    /**
     * 存储建议数据到缓存
     * 
     * @param key 缓存键
     * @param data 建议数据列表
     */
    fun put(key: String, data: List<String>) {
        cache[key] = CachedSuggestion(data)
    }
    
    /**
     * 手动使指定键的缓存失效
     * 
     * @param key 缓存键
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }
    
    /**
     * 清理所有缓存
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * 清理过期的缓存项
     * 内部维护方法，定期执行垃圾清理
     */
    private fun cleanupExpired() {
        val now = System.currentTimeMillis()
        val ttlMillis = TimeUnit.MINUTES.toMillis(ttlMinutes)
        
        cache.entries.removeIf { (_, value) ->
            now - value.timestamp > ttlMillis
        }
    }
    
    /**
     * 获取当前缓存状态信息
     * 用于调试和监控
     */
    fun getCacheStats(): Map<String, Any> {
        val now = System.currentTimeMillis()
        val ttlMillis = TimeUnit.MINUTES.toMillis(ttlMinutes)
        
        var activeCount = 0
        var expiredCount = 0
        
        cache.values.forEach { cached ->
            if (now - cached.timestamp > ttlMillis) {
                expiredCount++
            } else {
                activeCount++
            }
        }
        
        return mapOf(
            "totalEntries" to cache.size,
            "activeEntries" to activeCount,
            "expiredEntries" to expiredCount,
            "ttlMinutes" to ttlMinutes
        )
    }
    
    /**
     * 插件启用阶段进行清理任务初始化
     */
    @Awake(LifeCycle.ENABLE)
    fun enableCleanupTask() {
        // TabooLib会在适当的时候进行垃圾清理
        // 这里不需要额外的定时任务，依赖get方法中的懒加载清理机制
    }
}