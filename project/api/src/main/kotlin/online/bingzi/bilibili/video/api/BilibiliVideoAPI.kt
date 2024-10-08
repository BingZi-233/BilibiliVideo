package online.bingzi.bilibili.video.api

import online.bingzi.bilibili.video.internal.cache.Cache
import online.bingzi.bilibili.video.internal.entity.BindEntity
import online.bingzi.bilibili.video.internal.entity.CookieEntity
import online.bingzi.bilibili.video.internal.entity.ReceiveEntity
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Bilibili video API
 * <p>
 * API 入口
 *
 * @constructor Create empty Bilibili video API
 *
 * @author BingZi-233
 * @since 2.0.0
 */
object BilibiliVideoAPI {
    /**
     * Get player bind entity
     * <p>
     * 获取玩家绑定数据
     *
     * @param playerUUID Player UUID
     * @return BindEntity
     * @see BindEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerBindEntity(playerUUID: UUID): CompletableFuture<BindEntity> {
        return Cache.bindCache.get(playerUUID)
    }

    /**
     * Get player cookie entity
     * <p>
     * 获取玩家Cookie数据
     *
     * @param playerUUID Player UUID
     * @return CookieEntity
     * @see CookieEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerCookieEntity(playerUUID: UUID): CompletableFuture<CookieEntity> {
        return Cache.cookieCache.get(playerUUID)
    }

    /**
     * Get player receive entity
     * <p>
     * 获取玩家领取数据
     *
     * @param playerUUID Player UUID
     * @return ReceiveEntity
     * @see ReceiveEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntity(playerUUID: UUID): CompletableFuture<ReceiveEntity> {
        return Cache.receiveCache.get(playerUUID)
    }
}