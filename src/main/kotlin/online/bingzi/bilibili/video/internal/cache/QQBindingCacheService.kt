package online.bingzi.bilibili.video.internal.cache

import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * QQ绑定缓存服务
 * 提供基于内存的双向缓存系统，支持UUID→QQ号和QQ号→UUID的快速查询
 */
@Awake(LifeCycle.ENABLE)
object QQBindingCacheService {
    
    /**
     * 缓存条目数据类，包含值和过期时间
     */
    private data class CacheEntry<T>(
        val value: T,
        val expireTime: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
    }
    
    // UUID → QQ号 的缓存
    private val uuidToQQCache = ConcurrentHashMap<UUID, CacheEntry<Long>>()
    
    // QQ号 → UUID 的缓存
    private val qqToUuidCache = ConcurrentHashMap<Long, CacheEntry<UUID>>()
    
    // 缓存统计信息
    private var cacheHits = 0L
    private var cacheMisses = 0L
    
    // TTL清理定时器
    private val ttlCleanupScheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "QQBindingCache-TTL-Cleanup").apply { isDaemon = true }
    }
    
    /**
     * 初始化缓存服务
     */
    fun initialize() {
        if (!CacheConfig.enabled) {
            console().sendWarn("qqBindingCacheDisabled")
            return
        }
        
        // 验证配置有效性
        if (!CacheConfig.validateConfig()) {
            console().sendError("qqBindingCacheConfigInvalid")
            return
        }
        
        console().sendInfo("qqBindingCacheInitializing")
        
        // 启动TTL清理任务，根据TTL配置决定清理频率
        val cleanupIntervalMinutes = maxOf(1L, CacheConfig.ttl / 60 / 4) // TTL的1/4或最少1分钟
        ttlCleanupScheduler.scheduleAtFixedRate(
            { cleanupExpiredEntries() },
            cleanupIntervalMinutes,
            cleanupIntervalMinutes,
            TimeUnit.MINUTES
        )
        
        console().sendInfo("qqBindingCacheTtlCleanupScheduled", cleanupIntervalMinutes.toString())
        
        // 如果启用了预加载，则从数据库加载现有数据
        if (CacheConfig.preloadOnStartup) {
            preloadCache().thenAccept { loadedCount ->
                console().sendInfo("qqBindingCachePreloadComplete", loadedCount.toString())
            }.exceptionally { throwable ->
                console().sendError("qqBindingCachePreloadFailed", throwable.message ?: "Unknown error")
                null
            }
        } else {
            console().sendInfo("qqBindingCacheInitialized")
        }
    }
    
    /**
     * 根据玩家UUID获取QQ号
     * @param playerUuid 玩家UUID
     * @return QQ号，如果未找到或已过期则返回null
     */
    fun getQQByPlayer(playerUuid: UUID): Long? {
        if (!CacheConfig.enabled) {
            return null
        }
        
        val cacheEntry = uuidToQQCache[playerUuid]
        if (cacheEntry != null) {
            if (!cacheEntry.isExpired()) {
                cacheHits++
                return cacheEntry.value
            } else {
                // 缓存已过期，移除
                removeExpiredBinding(playerUuid)
                cacheMisses++
                return null
            }
        } else {
            cacheMisses++
            return null
        }
    }
    
    /**
     * 根据QQ号获取玩家UUID
     * @param qqNumber QQ号
     * @return 玩家UUID，如果未找到或已过期则返回null
     */
    fun getPlayerByQQ(qqNumber: Long): UUID? {
        if (!CacheConfig.enabled) {
            return null
        }
        
        val cacheEntry = qqToUuidCache[qqNumber]
        if (cacheEntry != null) {
            if (!cacheEntry.isExpired()) {
                cacheHits++
                return cacheEntry.value
            } else {
                // 缓存已过期，移除
                removeExpiredBindingByQQ(qqNumber)
                cacheMisses++
                return null
            }
        } else {
            cacheMisses++
            return null
        }
    }
    
    /**
     * 缓存绑定关系
     * @param playerUuid 玩家UUID
     * @param qqNumber QQ号
     */
    fun cacheBinding(playerUuid: UUID, qqNumber: Long) {
        if (!CacheConfig.enabled) {
            return
        }
        
        // 检查是否超过最大缓存大小
        if (uuidToQQCache.size >= CacheConfig.maxSize) {
            console().sendWarn("qqBindingCacheMaxSizeReached", CacheConfig.maxSize.toString())
            // 可以考虑实现LRU清理策略，这里简单地清理一部分
            if (uuidToQQCache.size > CacheConfig.maxSize * 0.9) {
                clearOldestEntries()
            }
        }
        
        // 计算过期时间
        val expireTime = System.currentTimeMillis() + (CacheConfig.ttl * 1000L)
        
        // 移除可能存在的旧绑定
        val oldQQEntry = uuidToQQCache.remove(playerUuid)
        if (oldQQEntry != null) {
            qqToUuidCache.remove(oldQQEntry.value)
        }
        
        val oldUuidEntry = qqToUuidCache.remove(qqNumber)
        if (oldUuidEntry != null) {
            uuidToQQCache.remove(oldUuidEntry.value)
        }
        
        // 添加新绑定（带TTL）
        uuidToQQCache[playerUuid] = CacheEntry(qqNumber, expireTime)
        qqToUuidCache[qqNumber] = CacheEntry(playerUuid, expireTime)
        
        console().sendInfo("qqBindingCacheAdded", playerUuid.toString(), qqNumber.toString())
    }
    
    /**
     * 移除绑定关系
     * @param playerUuid 玩家UUID
     */
    fun removeBinding(playerUuid: UUID) {
        if (!CacheConfig.enabled) {
            return
        }
        
        val qqEntry = uuidToQQCache.remove(playerUuid)
        if (qqEntry != null) {
            qqToUuidCache.remove(qqEntry.value)
            console().sendInfo("qqBindingCacheRemoved", playerUuid.toString(), qqEntry.value.toString())
        }
    }
    
    /**
     * 根据QQ号移除绑定关系
     * @param qqNumber QQ号
     */
    fun removeBindingByQQ(qqNumber: Long) {
        if (!CacheConfig.enabled) {
            return
        }
        
        val uuidEntry = qqToUuidCache.remove(qqNumber)
        if (uuidEntry != null) {
            uuidToQQCache.remove(uuidEntry.value)
            console().sendInfo("qqBindingCacheRemovedByQQ", qqNumber.toString(), uuidEntry.value.toString())
        }
    }
    
    /**
     * 预加载缓存数据
     * @return 加载的记录数
     */
    fun preloadCache(): CompletableFuture<Int> {
        return QQBindingDaoService.getAllActiveBindings().thenApply { bindings ->
            try {
                console().sendInfo("qqBindingCachePreloading")
                
                var loadedCount = 0
                bindings.forEach { binding ->
                    try {
                        val playerUuid = UUID.fromString(binding.playerUuid)
                        val qqNumber = binding.qqNumber.toLong()
                        val expireTime = System.currentTimeMillis() + (CacheConfig.ttl * 1000L)
                        
                        // 直接添加到缓存，不触发日志
                        uuidToQQCache[playerUuid] = CacheEntry(qqNumber, expireTime)
                        qqToUuidCache[qqNumber] = CacheEntry(playerUuid, expireTime)
                        loadedCount++
                    } catch (e: Exception) {
                        console().sendWarn("qqBindingCachePreloadItemFailed", binding.playerUuid, binding.qqNumber, e.message ?: "Unknown error")
                    }
                }
                
                console().sendInfo("qqBindingCachePreloadSuccess", loadedCount.toString())
                loadedCount
            } catch (e: Exception) {
                console().sendError("qqBindingCachePreloadError", e.message ?: "Unknown error")
                0
            }
        }.exceptionally { throwable ->
            console().sendError("qqBindingCachePreloadError", throwable.message ?: "Unknown error")
            0
        }
    }
    
    /**
     * 刷新指定玩家的缓存
     * @param playerUuid 玩家UUID
     * @return 是否成功刷新
     */
    fun refreshCache(playerUuid: UUID): CompletableFuture<Boolean> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid).thenApply { binding ->
            if (binding?.isValidBinding() == true) {
                try {
                    val qqNumber = binding.qqNumber.toLong()
                    cacheBinding(playerUuid, qqNumber)
                    true
                } catch (e: NumberFormatException) {
                    console().sendWarn("qqBindingCacheRefreshInvalidNumber", binding.qqNumber, playerUuid.toString())
                    false
                }
            } else {
                // 如果数据库中没有有效绑定，从缓存中移除
                removeBinding(playerUuid)
                true
            }
        }.exceptionally { throwable ->
            console().sendError("qqBindingCacheRefreshError", playerUuid.toString(), throwable.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 清空所有缓存
     */
    fun clearCache() {
        uuidToQQCache.clear()
        qqToUuidCache.clear()
        cacheHits = 0L
        cacheMisses = 0L
        console().sendInfo("qqBindingCacheCleared")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            totalBindings = uuidToQQCache.size,
            cacheHits = cacheHits,
            cacheMisses = cacheMisses,
            hitRate = if (cacheHits + cacheMisses == 0L) 0.0 else cacheHits.toDouble() / (cacheHits + cacheMisses),
            maxSize = CacheConfig.maxSize,
            enabled = CacheConfig.enabled
        )
    }
    
    /**
     * 清理最旧的缓存条目
     */
    private fun clearOldestEntries() {
        val entriesToRemove = (CacheConfig.maxSize * 0.1).toInt()
        val iterator = uuidToQQCache.entries.iterator()
        var removed = 0
        
        while (iterator.hasNext() && removed < entriesToRemove) {
            val entry = iterator.next()
            iterator.remove()
            qqToUuidCache.remove(entry.value.value)
            removed++
        }
        
        console().sendWarn("qqBindingCacheCleanedOldEntries", removed.toString())
    }
    
    /**
     * 清理过期缓存条目
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val expiredUuids = mutableListOf<UUID>()
        val expiredQQs = mutableListOf<Long>()
        
        // 查找过期的UUID->QQ映射
        uuidToQQCache.forEach { (uuid, entry) ->
            if (entry.expireTime <= now) {
                expiredUuids.add(uuid)
            }
        }
        
        // 查找过期的QQ->UUID映射
        qqToUuidCache.forEach { (qq, entry) ->
            if (entry.expireTime <= now) {
                expiredQQs.add(qq)
            }
        }
        
        // 移除过期条目
        expiredUuids.forEach { uuid ->
            val entry = uuidToQQCache.remove(uuid)
            if (entry != null) {
                qqToUuidCache.remove(entry.value)
            }
        }
        
        expiredQQs.forEach { qq ->
            val entry = qqToUuidCache.remove(qq)
            if (entry != null) {
                uuidToQQCache.remove(entry.value)
            }
        }
        
        val totalExpired = expiredUuids.size + expiredQQs.size
        if (totalExpired > 0) {
            console().sendInfo("qqBindingCacheTtlCleanup", totalExpired.toString())
        }
    }
    
    /**
     * 移除过期的绑定关系（根据UUID）
     */
    private fun removeExpiredBinding(playerUuid: UUID) {
        val entry = uuidToQQCache.remove(playerUuid)
        if (entry != null) {
            qqToUuidCache.remove(entry.value)
        }
    }
    
    /**
     * 移除过期的绑定关系（根据QQ号）
     */
    private fun removeExpiredBindingByQQ(qqNumber: Long) {
        val entry = qqToUuidCache.remove(qqNumber)
        if (entry != null) {
            uuidToQQCache.remove(entry.value)
        }
    }
    
    /**
     * 关闭缓存服务
     */
    fun shutdown() {
        ttlCleanupScheduler.shutdown()
        try {
            if (!ttlCleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                ttlCleanupScheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            ttlCleanupScheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
        console().sendInfo("qqBindingCacheTtlCleanupShutdown")
    }
    
    /**
     * 缓存统计数据类
     */
    data class CacheStats(
        val totalBindings: Int,
        val cacheHits: Long,
        val cacheMisses: Long,
        val hitRate: Double,
        val maxSize: Int,
        val enabled: Boolean
    )
}