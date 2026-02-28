package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.database.SqliteWriteExecutor
import online.bingzi.bilibili.video.internal.entity.BoundAccount
import online.bingzi.bilibili.video.internal.entity.BoundAccounts
import online.bingzi.bilibili.video.internal.entity.toBoundAccount
import org.ktorm.dsl.*

/**
 * 玩家与 B 站账号绑定仓储。
 *
 * 注意：该对象不持有任何 Bukkit / 玩家实例，只处理纯数据，避免内存泄漏。
 */
internal object BoundAccountRepository {

    private val db get() = DatabaseFactory.database()

    fun findByPlayerUuid(playerUuid: String, includeInactive: Boolean = false): BoundAccount? {
        val active = db.from(BoundAccounts)
            .select()
            .where { (BoundAccounts.playerUuid eq playerUuid) and (BoundAccounts.status eq 1) }
            .map { it.toBoundAccount() }
            .firstOrNull()
        if (active != null || !includeInactive) {
            return active
        }
        return db.from(BoundAccounts)
            .select()
            .where { BoundAccounts.playerUuid eq playerUuid }
            .map { it.toBoundAccount() }
            .firstOrNull()
    }

    fun findByBilibiliMid(bilibiliMid: Long, includeInactive: Boolean = false): BoundAccount? {
        val active = db.from(BoundAccounts)
            .select()
            .where { (BoundAccounts.bilibiliMid eq bilibiliMid) and (BoundAccounts.status eq 1) }
            .map { it.toBoundAccount() }
            .firstOrNull()
        if (active != null || !includeInactive) {
            return active
        }
        return db.from(BoundAccounts)
            .select()
            .where { BoundAccounts.bilibiliMid eq bilibiliMid }
            .map { it.toBoundAccount() }
            .firstOrNull()
    }

    fun findByPlayerName(playerName: String, includeInactive: Boolean = false): BoundAccount? {
        val active = db.from(BoundAccounts)
            .select()
            .where { (BoundAccounts.playerName eq playerName) and (BoundAccounts.status eq 1) }
            .map { it.toBoundAccount() }
            .firstOrNull()
        if (active != null || !includeInactive) {
            return active
        }
        return db.from(BoundAccounts)
            .select()
            .where { BoundAccounts.playerName eq playerName }
            .map { it.toBoundAccount() }
            .firstOrNull()
    }

    fun insert(
        playerUuid: String,
        playerName: String,
        bilibiliMid: Long,
        bilibiliName: String,
        status: Int = 1,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt
    ): Int = SqliteWriteExecutor.executeWrite {
        db.insert(BoundAccounts) {
            set(it.playerUuid, playerUuid)
            set(it.playerName, playerName)
            set(it.bilibiliMid, bilibiliMid)
            set(it.bilibiliName, bilibiliName)
            set(it.status, status)
            set(it.createdAt, createdAt)
            set(it.updatedAt, updatedAt)
        }
    }

    fun updateStatusByPlayerUuid(playerUuid: String, status: Int): Int = SqliteWriteExecutor.executeWrite {
        db.update(BoundAccounts) {
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
    ): Int = SqliteWriteExecutor.executeWrite {
        db.update(BoundAccounts) {
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
