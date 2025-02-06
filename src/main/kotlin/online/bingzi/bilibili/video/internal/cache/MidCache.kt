package online.bingzi.bilibili.video.internal.cache

import com.github.benmanes.caffeine.cache.Caffeine
import online.bingzi.bilibili.video.internal.database.Database.Companion.getPlayerDataContainer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 该对象用于缓存MID（用户唯一标识符）。
 * Mid缓存使用Caffeine库来实现，具有最大缓存大小和刷新策略。
 * 缓存中的数据会在写入后5分钟内自动刷新。
 */
val midCache = Caffeine.newBuilder()
    // 设置缓存的最大数量为100
    .maximumSize(100)
    // 设置在写入后5分钟内自动刷新缓存
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    // 构建缓存并定义缓存的加载逻辑
    .build<UUID, String> {
        // 当缓存中不存在指定的key时，从数据库中获取对应的玩家数据（mid）
        it.getPlayerDataContainer("mid")
    }

/**
 * 该对象用于缓存用户名（Uname）。
 * Uname缓存同样使用Caffeine库来实现，具有与MID缓存相同的最大大小和刷新策略。
 * 缓存中的数据也会在写入后5分钟内自动刷新。
 */
val unameCache = Caffeine.newBuilder()
    // 设置缓存的最大数量为100
    .maximumSize(100)
    // 设置在写入后5分钟内自动刷新缓存
    .refreshAfterWrite(5, TimeUnit.MINUTES)
    // 构建缓存并定义缓存的加载逻辑
    .build<UUID, String> {
        // 当缓存中不存在指定的key时，从数据库中获取对应的玩家数据（uname）
        it.getPlayerDataContainer("uname")
    }