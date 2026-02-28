package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.dsl.QueryRowSet
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * B 站登录凭证信息。
 */
internal data class Credential(
    val id: Long? = null,
    val label: String,
    val sessData: String,
    val biliJct: String,
    val bilibiliMid: Long? = null,
    val buvid3: String? = null,
    val accessKey: String? = null,
    val refreshToken: String? = null,
    val status: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val expiredAt: Long? = null,
    val lastUsedAt: Long? = null
)

internal object Credentials : Table<Nothing>(DATABASE_TABLE_PREFIX + "credential") {

    val id = long("id").primaryKey()
    val label = varchar("label")
    val sessData = varchar("sessdata")
    val biliJct = varchar("bili_jct")
    val bilibiliMid = long("bilibili_mid")
    val buvid3 = varchar("buvid3")
    val accessKey = varchar("access_key")
    val refreshToken = varchar("refresh_token")
    val status = int("status")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val expiredAt = long("expired_at")
    val lastUsedAt = long("last_used_at")
}

internal fun QueryRowSet.toCredential() = Credential(
    id = this[Credentials.id],
    label = this[Credentials.label]!!,
    sessData = this[Credentials.sessData]!!,
    biliJct = this[Credentials.biliJct]!!,
    bilibiliMid = this[Credentials.bilibiliMid],
    buvid3 = this[Credentials.buvid3],
    accessKey = this[Credentials.accessKey],
    refreshToken = this[Credentials.refreshToken],
    status = this[Credentials.status]!!,
    createdAt = this[Credentials.createdAt]!!,
    updatedAt = this[Credentials.updatedAt]!!,
    expiredAt = this[Credentials.expiredAt],
    lastUsedAt = this[Credentials.lastUsedAt]
)
