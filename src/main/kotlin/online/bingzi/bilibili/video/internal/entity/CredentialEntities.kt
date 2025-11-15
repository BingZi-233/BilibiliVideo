package online.bingzi.bilibili.video.internal.entity

import online.bingzi.bilibili.video.internal.DATABASE_TABLE_PREFIX
import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * B 站登录凭证信息。
 */
internal interface Credential : Entity<Credential> {

    companion object : Entity.Factory<Credential>()

    var id: Long?
    var label: String
    var sessData: String
    var biliJct: String
    var bilibiliMid: Long?
    var buvid3: String?
    var accessKey: String?
    var refreshToken: String?
    var status: Int
    var createdAt: Long
    var updatedAt: Long
    var expiredAt: Long?
    var lastUsedAt: Long?
}

internal object Credentials : Table<Credential>(DATABASE_TABLE_PREFIX + "credential") {

    val id = long("id").primaryKey().bindTo(Credential::id)
    val label = varchar("label").bindTo(Credential::label)
    val sessData = varchar("sessdata").bindTo(Credential::sessData)
    val biliJct = varchar("bili_jct").bindTo(Credential::biliJct)
    val bilibiliMid = long("bilibili_mid").bindTo(Credential::bilibiliMid)
    val buvid3 = varchar("buvid3").bindTo(Credential::buvid3)
    val accessKey = varchar("access_key").bindTo(Credential::accessKey)
    val refreshToken = varchar("refresh_token").bindTo(Credential::refreshToken)
    val status = int("status").bindTo(Credential::status)
    val createdAt = long("created_at").bindTo(Credential::createdAt)
    val updatedAt = long("updated_at").bindTo(Credential::updatedAt)
    val expiredAt = long("expired_at").bindTo(Credential::expiredAt)
    val lastUsedAt = long("last_used_at").bindTo(Credential::lastUsedAt)
}
