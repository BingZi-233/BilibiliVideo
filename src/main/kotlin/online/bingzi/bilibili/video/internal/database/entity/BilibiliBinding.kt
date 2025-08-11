package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * Bilibili绑定实体
 * 存储玩家的Bilibili账号绑定信息
 */
@DatabaseTable(tableName = "bv_bilibili_bindings")
data class BilibiliBinding(
    /**
     * 自增ID作为主键
     */
    @DatabaseField(generatedId = true, columnName = "id")
    var id: Long = 0,

    /**
     * 玩家UUID，外键关联
     */
    @DatabaseField(columnName = "player_uuid", canBeNull = false, width = 36, index = true)
    var playerUuid: String = "",

    /**
     * Bilibili用户ID
     */
    @DatabaseField(columnName = "bilibili_uid", canBeNull = false, uniqueIndex = true)
    var bilibiliUid: Long = 0,

    /**
     * Bilibili用户名
     */
    @DatabaseField(columnName = "bilibili_username", canBeNull = true, width = 50)
    var bilibiliUsername: String? = null,

    /**
     * Bilibili昵称
     */
    @DatabaseField(columnName = "bilibili_nickname", canBeNull = true, width = 100)
    var bilibiliNickname: String? = null,

    /**
     * 用户头像URL
     */
    @DatabaseField(columnName = "avatar_url", canBeNull = true, width = 255)
    var avatarUrl: String? = null,

    /**
     * 用户等级
     */
    @DatabaseField(columnName = "user_level", canBeNull = true)
    var userLevel: Int? = null,

    /**
     * 绑定时间戳
     */
    @DatabaseField(columnName = "bind_time")
    var bindTime: Long = System.currentTimeMillis(),

    /**
     * 最后登录时间戳
     */
    @DatabaseField(columnName = "last_login_time", canBeNull = true)
    var lastLoginTime: Long? = null,

    /**
     * 最后验证时间戳
     */
    @DatabaseField(columnName = "last_verify_time", canBeNull = true)
    var lastVerifyTime: Long? = null,

    /**
     * 绑定状态：ACTIVE(激活), EXPIRED(已过期), INVALID(无效), DISABLED(已禁用)
     */
    @DatabaseField(columnName = "bind_status", width = 10)
    var bindStatus: String = "ACTIVE",

    /**
     * 是否激活状态，默认为true
     */
    @DatabaseField(columnName = "is_active")
    var isActive: Boolean = true
) {
    /**
     * OrmLite需要的无参构造函数
     */
    constructor() : this(0)

    /**
     * 便捷构造函数
     */
    constructor(playerUuid: String, bilibiliUid: Long) : this(
        id = 0,
        playerUuid = playerUuid,
        bilibiliUid = bilibiliUid
    )

    /**
     * 绑定状态枚举
     */
    enum class Status(val value: String) {
        ACTIVE("ACTIVE"),
        EXPIRED("EXPIRED"),
        INVALID("INVALID"),
        DISABLED("DISABLED");

        companion object {
            fun fromValue(value: String): Status {
                return values().find { it.value == value } ?: ACTIVE
            }
        }
    }

    /**
     * 获取绑定状态枚举
     */
    fun getBindStatusEnum(): Status = Status.fromValue(bindStatus)

    /**
     * 设置绑定状态
     */
    fun setBindStatus(status: Status) {
        bindStatus = status.value
    }

    /**
     * 更新最后登录时间
     */
    fun updateLastLoginTime() {
        lastLoginTime = System.currentTimeMillis()
    }

    /**
     * 更新最后验证时间
     */
    fun updateLastVerifyTime() {
        lastVerifyTime = System.currentTimeMillis()
    }

    /**
     * 更新激活状态
     */
    fun updateActiveStatus(active: Boolean) {
        isActive = active
    }

    /**
     * 更新用户信息
     */
    fun updateUserInfo(username: String?, nickname: String?, avatarUrl: String?, level: Int?) {
        this.bilibiliUsername = username
        this.bilibiliNickname = nickname
        this.avatarUrl = avatarUrl
        this.userLevel = level
        updateLastVerifyTime()
    }

    /**
     * 检查绑定是否有效（激活且不是禁用状态）
     */
    fun isValidBinding(): Boolean {
        return isActive && getBindStatusEnum() == Status.ACTIVE
    }

    /**
     * 检查绑定是否已过期（根据最后登录时间判断）
     */
    fun isExpired(expireDays: Int = 30): Boolean {
        val lastLogin = lastLoginTime ?: bindTime
        val expireTime = System.currentTimeMillis() - (expireDays * 24 * 60 * 60 * 1000L)
        return lastLogin < expireTime
    }

    override fun toString(): String {
        return "BilibiliBinding(playerUuid='$playerUuid', bilibiliUid=$bilibiliUid, bilibiliUsername='$bilibiliUsername', bindStatus='$bindStatus', isActive=$isActive)"
    }
}