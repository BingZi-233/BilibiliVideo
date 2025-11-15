package online.bingzi.bilibili.video.internal.repository

import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import online.bingzi.bilibili.video.internal.entity.Credential
import online.bingzi.bilibili.video.internal.entity.Credentials
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.dsl.update
import org.ktorm.entity.find
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList

/**
 * B 站凭证仓储。
 */
internal object CredentialRepository {

    private val db get() = DatabaseFactory.database()

    private val credentials get() = db.sequenceOf(Credentials)

    fun findByLabel(label: String): Credential? {
        return credentials.find { it.label eq label }
    }

    fun findAll(): List<Credential> {
        return credentials.toList()
    }

    fun insert(
        label: String,
        sessData: String,
        biliJct: String,
        bilibiliMid: Long?,
        buvid3: String?,
        accessKey: String?,
        refreshToken: String?,
        status: Int = 1,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = createdAt,
        expiredAt: Long? = null,
        lastUsedAt: Long? = null
    ): Int {
        return db.insert(Credentials) {
            set(it.label, label)
            set(it.sessData, sessData)
            set(it.biliJct, biliJct)
            set(it.bilibiliMid, bilibiliMid)
            set(it.buvid3, buvid3)
            set(it.accessKey, accessKey)
            set(it.refreshToken, refreshToken)
            set(it.status, status)
            set(it.createdAt, createdAt)
            set(it.updatedAt, updatedAt)
            set(it.expiredAt, expiredAt)
            set(it.lastUsedAt, lastUsedAt)
        }
    }

    fun findByBilibiliMid(mid: Long): Credential? {
        return credentials.find { it.bilibiliMid eq mid }
    }

    fun updateStatusAndUsage(
        label: String,
        status: Int,
        expiredAt: Long?,
        lastUsedAt: Long?
    ): Int {
        return db.update(Credentials) {
            set(it.status, status)
            set(it.expiredAt, expiredAt)
            set(it.lastUsedAt, lastUsedAt)
            set(it.updatedAt, System.currentTimeMillis())
            where {
                it.label eq label
            }
        }
    }

    fun updateTokensByMid(
        bilibiliMid: Long,
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        refreshToken: String?,
        status: Int = 1
    ): Int {
        return db.update(Credentials) {
            set(it.sessData, sessData)
            set(it.biliJct, biliJct)
            set(it.bilibiliMid, bilibiliMid)
            set(it.buvid3, buvid3)
            set(it.accessKey, accessKey)
            set(it.refreshToken, refreshToken)
            set(it.status, status)
            set(it.updatedAt, System.currentTimeMillis())
            where {
                it.bilibiliMid eq bilibiliMid
            }
        }
    }
}
