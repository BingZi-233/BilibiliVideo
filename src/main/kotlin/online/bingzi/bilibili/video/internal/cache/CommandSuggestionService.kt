package online.bingzi.bilibili.video.internal.cache

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.UploaderVideoDaoService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 命令参数建议服务
 * 封装各种数据源的建议获取逻辑，提供统一的缓存式建议API
 * 
 * 主要功能：
 * - 统一的数据获取接口
 * - 异步处理和超时控制  
 * - 优雅降级机制
 * - 基于TTL的缓存管理
 */
@Awake
object CommandSuggestionService {
    
    /**
     * 配置文件
     */
    @Config("config.yml")
    lateinit var config: Configuration
    
    /**
     * 响应超时时间（毫秒）- 从配置文件读取
     */
    private val responseTimeoutMs: Long
        get() = config.getLong("command-suggestion.response-timeout-ms", 100L)
    
    /**
     * 最大建议数量 - 从配置文件读取
     */
    private val maxSuggestions: Int
        get() = config.getInt("command-suggestion.max-suggestions", 10)
    
    /**
     * QQ绑定建议 - 获取最近绑定的QQ号码
     * 
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getQQBindingSuggestions(limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.RECENT_QQ_NUMBERS.key()) {
            QQBindingDaoService.getRecentQQNumbers(limit)
        }
    }
    
    /**
     * UP主UID建议 - 获取热门或已监控的UP主UID
     * 
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getUploaderUidSuggestions(limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.POPULAR_UPLOADER_UIDS.key()) {
            UploaderVideoDaoService.getPopularUploaderUids(limit)
        }
    }
    
    /**
     * 已监控UP主UID建议 - 获取当前监控中的UP主
     * 
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getMonitoredUploaderSuggestions(limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.MONITORED_UPLOADER_UIDS.key()) {
            UploaderVideoDaoService.getAllConfigs().thenApply { configs ->
                configs.take(limit).map { "${it.uploaderUid} (${it.uploaderName})" }
            }
        }
    }
    
    /**
     * BV号建议 - 从监控的UP主视频中获取
     * 
     * @param playerUuid 玩家UUID
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getBvIdSuggestions(playerUuid: UUID, limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.RECENT_BV_IDS.key("playerUuid" to playerUuid.toString())) {
            UploaderVideoDaoService.getRecentVideosForReward(playerUuid, limit)
                .thenApply { videos -> 
                    videos.map { "${it.bvId} (${it.title?.take(20) ?: "未知"})" }
                }
        }
    }
    
    /**
     * Bilibili UID建议 - 获取活跃的绑定用户
     * 
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getBilibiliUidSuggestions(limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.ACTIVE_BILIBILI_UIDS.key()) {
            BilibiliBindingDaoService.getAllActiveBilibiliBindings()
                .thenApply { bindings -> 
                    bindings.take(limit).map { "${it.bilibiliUid} (${it.bilibiliUsername ?: "未知"})" }
                }
        }
    }
    
    /**
     * 活跃QQ绑定建议 - 用于管理员操作
     * 
     * @param limit 建议数量限制
     * @return 建议列表的Future
     */
    fun getActiveQQBindingSuggestions(limit: Int = maxSuggestions): CompletableFuture<List<String>> {
        return getCachedOrFetch(CacheKeys.ACTIVE_QQ_BINDINGS.key()) {
            QQBindingDaoService.getAllActiveQQBindings()
                .thenApply { bindings -> 
                    bindings.take(limit).map { "${it.qqNumber} (${it.qqNickname ?: "未知"})" }
                }
        }
    }
    
    /**
     * 通用缓存获取方法
     * 实现缓存优先策略，包含超时控制和优雅降级
     * 
     * @param cacheKey 缓存键
     * @param fetcher 数据获取函数
     * @return 建议列表
     */
    fun getCachedOrFetch(
        cacheKey: String,
        fetcher: () -> CompletableFuture<List<String>>
    ): CompletableFuture<List<String>> {
        // 首先尝试从缓存获取
        val cachedData = CommandSuggestionCache.get(cacheKey)
        if (cachedData != null) {
            return CompletableFuture.completedFuture(cachedData)
        }
        
        // 缓存未命中，异步获取数据
        return fetchWithTimeout(cacheKey, fetcher)
    }
    
    /**
     * 带超时控制的数据获取
     * 
     * @param cacheKey 缓存键
     * @param fetcher 数据获取函数
     * @return 建议列表的Future
     */
    private fun fetchWithTimeout(
        cacheKey: String,
        fetcher: () -> CompletableFuture<List<String>>
    ): CompletableFuture<List<String>> {
        val future = CompletableFuture<List<String>>()
        
        submit(async = true) {
            try {
                val dataFuture = fetcher()
                val result = dataFuture.get(responseTimeoutMs, TimeUnit.MILLISECONDS)
                
                // 限制建议数量并缓存结果
                val limitedResult = result.take(maxSuggestions)
                CommandSuggestionCache.put(cacheKey, limitedResult)
                
                future.complete(limitedResult)
            } catch (e: Exception) {
                // 超时或其他异常，使用优雅降级
                handleFetchError(cacheKey, e)
                future.complete(emptyList())
            }
        }
        
        return future
    }
    
    /**
     * 处理数据获取错误
     * 记录错误并进行适当的降级处理
     * 
     * @param cacheKey 缓存键
     * @param error 错误信息
     */
    private fun handleFetchError(cacheKey: String, error: Exception) {
        console().sendWarn("commandSuggestionDataError", cacheKey, error.message ?: "Unknown error")
        
        // 对于某些关键缓存，可以提供默认值
        when {
            cacheKey.contains("qq_numbers") -> {
                // 从配置文件获取QQ号示例
                val examples = config.getStringList("command-suggestion.fallback-examples.qq-numbers")
                CommandSuggestionCache.put(cacheKey, examples)
            }
            cacheKey.contains("uploader_uids") -> {
                // 从配置文件获取UP主UID示例
                val examples = config.getStringList("command-suggestion.fallback-examples.uploader-uids")
                CommandSuggestionCache.put(cacheKey, examples)
            }
            cacheKey.contains("bv_ids") -> {
                // 从配置文件获取BV号示例
                val examples = config.getStringList("command-suggestion.fallback-examples.bv-numbers")
                CommandSuggestionCache.put(cacheKey, examples)
            }
        }
    }
    
    /**
     * 获取降级BV号示例数据
     * 用于命令建议的降级处理
     * 
     * @return BV号格式示例列表
     */
    fun getFallbackBvExamples(): List<String> {
        return try {
            config.getStringList("command-suggestion.fallback-examples.bv-numbers")
        } catch (e: Exception) {
            // 配置获取失败时的最后降级
            listOf("BV1xx411c7mD", "BV1xx411c7mE", "请输入BV号")
        }
    }
    
    /**
     * 批量预热缓存
     * 在适当的时机预加载常用的建议数据
     */
    fun preloadCache() {
        submit(async = true) {
            try {
                // 预加载热门数据
                getQQBindingSuggestions()
                getUploaderUidSuggestions()
                getBilibiliUidSuggestions()
            } catch (e: Exception) {
                console().sendWarn("commandSuggestionCachePreloadError", e.message ?: "Unknown error")
            }
        }
    }
    
    /**
     * 清理指定类型的缓存
     * 
     * @param type 缓存类型前缀
     */
    fun invalidateCacheByType(type: String) {
        CommandSuggestionCache.clear() // 简单实现，清理所有缓存
        console().sendWarn("commandSuggestionCacheCleared")
    }
    
    /**
     * 插件启用时进行初始化
     */
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        // 延迟预热缓存，避免启动时的性能影响
        submit(delay = 200L, async = true) {
            preloadCache()
        }
    }
}