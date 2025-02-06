package online.bingzi.bilibili.video.internal.cache

import com.github.benmanes.caffeine.cache.Caffeine
import online.bingzi.bilibili.video.internal.database.Database.Companion.getPlayerDataContainer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * BvCache类
 * 该类用于创建和管理Bv缓存。它使用Caffeine库来实现缓存功能，提供快速存取Bv相关的数据。
 * 缓存的主要功能是减少对数据库的频繁访问，提高性能，并且支持缓存过期和刷新机制。
 */
val bvCache = Caffeine.newBuilder()
    // 设置缓存的最大条目数为100
    .maximumSize(100)
    // 设置在写入后5分钟内刷新缓存
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    // 构建缓存，指定键值对的类型为<Pair<UUID, String>, Boolean>
    .build<Pair<UUID, String>, Boolean> {
        // 该lambda函数用于计算给定键的值
        // 从UUID和String构成的Pair中获取PlayerDataContainer，如果获取成功则转换为Boolean类型
        // 如果获取失败则返回false
        it.first.getPlayerDataContainer(it.second)?.toBooleanStrictOrNull() ?: false
    }