package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.entity.BoundAccount
import online.bingzi.bilibili.video.internal.entity.BoundAccounts
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf

/**
 * 玩家与 B 站账号绑定仓储。
 *
 * 注意：该对象不持有任何 Bukkit / 玩家实例，只处理纯数据，避免内存泄漏。
 */
internal object BoundAccountRepository {

    private val db get() = DatabaseFactory.database()

    private val boundAccounts get() = db.sequenceOf(BoundAccounts)

    fun findByPlayerUuid(playerUuid: String, includeInactive: Boolean = false): BoundAccount? {
        val active = boundAccounts.find { (it.playerUuid eq playerUuid) and (it.status eq 1) }
        if (active != null || !includeInactive) {
            return active
        }
        return boundAccounts.find { it.playerUuid eq playerUuid }
    }

    fun findByBilibiliMid(bilibiliMid: Long, includeInactive: Boolean = false): BoundAccount? {
        val active = boundAccounts.find { (it.bilibiliMid eq bilibiliMid) and (it.status eq 1) }
        if (active != null || !includeInactive) {
            return active
        }
        return boundAccounts.find { it.bilibiliMid eq bilibiliMid }
    }

    fun findByPlayerName(playerName: String, includeInactive: Boolean = false): BoundAccount? {
        val active = boundAccounts.find { (it.playerName eq playerName) and (it.status eq 1) }
        if (active != null || !includeInactive) {
            return active
        }
        return boundAccounts.find { it.playerName eq playerName }
    }

    fun insert(
        playerUuid: String,
        playerName: String,
        bilibiliMid: Long,
        bilibiliName: String,
        status: Int = 1,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Int {
        return db.insert(BoundAccounts) {
            set(it.playerUuid, playerUuid)
            set(it.playerName, playerName)
            set(it.bilibiliMid, bilibiliMid)
            set(it.bilibiliName, bilibiliName)
            set(it.status, status)
            set(it.createdAt, createdAt)
            set(it.updatedAt, updatedAt)
        }
    }

    fun updateStatusByPlayerUuid(playerUuid: String, status: Int): Int {
        return db.update(BoundAccounts) {
            set(it.status, status)
            set(it.updatedAt, System.currentTimeMillis())
            where {
                it.playerUuid eq playerUuid
            }
        }
    }

    fun updateBinding(
        playerUuid: String,
        playerName: String,
        bilibiliMid: Long,
        bilibiliName: String,
        status: Int = 1
    ): Int {
        return db.update(BoundAccounts) {
            set(it.playerName, playerName)
            set(it.bilibiliMid, bilibiliMid)
            set(it.bilibiliName, bilibiliName)
            set(it.status, status)
            set(it.updatedAt, System.currentTimeMillis())
            where {
                it.playerUuid eq playerUuid
            }
        }
    }
}
