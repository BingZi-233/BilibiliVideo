package online.bingzi.bilibili.video.api

import online.bingzi.bilibili.video.internal.cache.Cache
import online.bingzi.bilibili.video.internal.entity.BindEntity
import online.bingzi.bilibili.video.internal.entity.CookieEntity
import online.bingzi.bilibili.video.internal.entity.ReceiveEntity
import java.util.*

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
     * @return [BindEntity]
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerBindEntity(playerUUID: UUID): BindEntity? {
        return Cache.bindCache.get(playerUUID)
    }

    /**
     * Get player cookie entity
     * <p>
     * 获取玩家Cookie数据
     *
     * @param playerUUID Player UUID
     * @return [CookieEntity]
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerCookieEntity(playerUUID: UUID): CookieEntity? {
        return Cache.cookieCache.get(playerUUID)
    }

    /**
     * Get player receive entity
     * <p>
     * 获取玩家领取数据
     *
     * @param id Player UUID
     * @return [ReceiveEntity]
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntityById(id: UUID): ReceiveEntity? {
        return Cache.receiveCache.get(id)
    }

    /**
     * Get player receive entity by player UUID
     * <p>
     * 通过玩家UUID获取玩家领取数据
     *
     * @param playerUUID Player UUID
     * @return List
     * @see ReceiveEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntityByPlayerUUID(playerUUID: UUID): List<ReceiveEntity> {
        return ReceiveEntity.getDao().queryForMatchingArgs(ReceiveEntity(playerUUID = playerUUID))
    }

    /**
     * Check player receive entity by mid
     * <p>
     * 通过MID检查玩家领取数据
     * true - 已领取
     * false - 未领取
     *
     * @param bilibiliMid Bilibili MID
     * @param bilibiliBv Bilibili BV
     * @return Boolean
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun checkPlayerReceiveEntityByMidAndBv(bilibiliMid: String, bilibiliBv: String): Boolean {
        return ReceiveEntity.getDao().queryForMatchingArgs(ReceiveEntity(bilibiliMid = bilibiliMid, bilibiliBv = bilibiliBv)).isNotEmpty()
    }

    /**
     * Get player receive entity by mid
     * <p>
     * 通过MID获取玩家领取数据
     *
     * @param bilibiliMid Bilibili MID
     * @return List
     * @see ReceiveEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntityByMid(bilibiliMid: String): List<ReceiveEntity> {
        return ReceiveEntity.getDao().queryForMatchingArgs(ReceiveEntity(bilibiliMid = bilibiliMid))
    }

    /**
     * Get player receive entity by bv
     * <p>
     * 通过BV获取玩家领取数据
     *
     * @param bilibiliBv Bilibili BV
     * @return List
     * @see ReceiveEntity
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntityByBv(bilibiliBv: String): List<ReceiveEntity> {
        return ReceiveEntity.getDao().queryForMatchingArgs(ReceiveEntity(bilibiliBv = bilibiliBv))
    }

    /**
     * Get player receive entity by mid and bv
     * <p>
     * 通过MID和BV获取玩家领取数据
     *
     * @param bilibiliMid Bilibili MID
     * @param bilibiliBv Bilibili BV
     * @return [ReceiveEntity]
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun getPlayerReceiveEntityByMidAndBv(bilibiliMid: String, bilibiliBv: String): ReceiveEntity? {
        return ReceiveEntity.getDao().queryForMatchingArgs(ReceiveEntity(bilibiliMid = bilibiliMid, bilibiliBv = bilibiliBv)).firstOrNull()
    }

    /**
     * Set player receive entity by player UUID and bv
     * <p>
     * 通过玩家UUID和BV设置玩家领取数据
     *
     * @param playerUUID Player UUID
     * @param playerName Player Name
     * @param bilibiliBv Bilibili BV
     * @param bilibiliMid Bilibili MID
     *
     * @author BingZi-233
     * @since 2.0.0
     */
    fun setPlayerReceiveEntityByPlayerUUIDAndBv(playerUUID: UUID, playerName: String, bilibiliBv: String, bilibiliMid: String) {
        val receiveEntity = ReceiveEntity(
            playerUUID = playerUUID,
            playerName = playerName,
            bilibiliBv = bilibiliBv,
            bilibiliMid = bilibiliMid
        )
        receiveEntity.create()
    }
}
