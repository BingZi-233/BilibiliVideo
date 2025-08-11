package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * Bilibili Cookie详情实体
 * 存储Bilibili登录相关的Cookie信息
 */
@DatabaseTable(tableName = "bv_bilibili_cookies")
data class BilibiliCookie(
    /**
     * 自增ID作为主键
     */
    @DatabaseField(generatedId = true, columnName = "id")
    var id: Long = 0,

    /**
     * 关联的Bilibili绑定ID
     */
    @DatabaseField(columnName = "bilibili_binding_id", canBeNull = false, index = true)
    var bilibiliBindingId: Long = 0,

    /**
     * 玩家UUID，冗余字段便于查询
     */
    @DatabaseField(columnName = "player_uuid", canBeNull = false, width = 36, index = true)
    var playerUuid: String = "",

    /**
     * SESSDATA - 会话数据
     */
    @DatabaseField(columnName = "sess_data", canBeNull = true, width = 100)
    var sessData: String? = null,

    /**
     * bili_jct - CSRF Token
     */
    @DatabaseField(columnName = "bili_jct", canBeNull = true, width = 100)
    var biliJct: String? = null,

    /**
     * DedeUserID - 用户ID
     */
    @DatabaseField(columnName = "dede_user_id", canBeNull = true, width = 50)
    var dedeUserId: String? = null,

    /**
     * DedeUserID__ckMd5 - 用户ID校验码
     */
    @DatabaseField(columnName = "dede_user_id_ckmd5", canBeNull = true, width = 100)
    var dedeUserIdCkMd5: String? = null,

    /**
     * sid - 会话ID
     */
    @DatabaseField(columnName = "sid", canBeNull = true, width = 100)
    var sid: String? = null,

    /**
     * buvid3 - 浏览器唯一标识
     */
    @DatabaseField(columnName = "buvid3", canBeNull = true, width = 100)
    var buvid3: String? = null,

    /**
     * buvid4 - 浏览器唯一标识v4
     */
    @DatabaseField(columnName = "buvid4", canBeNull = true, width = 100)
    var buvid4: String? = null,

    /**
     * uuid - 通用唯一标识符
     */
    @DatabaseField(columnName = "uuid", canBeNull = true, width = 100)
    var uuid: String? = null,

    /**
     * bfe_id - 前端标识
     */
    @DatabaseField(columnName = "bfe_id", canBeNull = true, width = 100)
    var bfeId: String? = null,

    /**
     * b_nut - B站坚果标识
     */
    @DatabaseField(columnName = "b_nut", canBeNull = true, width = 50)
    var bNut: String? = null,

    /**
     * b_lsid - B站本地会话ID
     */
    @DatabaseField(columnName = "b_lsid", canBeNull = true, width = 100)
    var bLsid: String? = null,

    /**
     * 原始Cookie字符串（完整备份）
     */
    @DatabaseField(columnName = "raw_cookies", canBeNull = true, dataType = com.j256.ormlite.field.DataType.LONG_STRING)
    var rawCookies: String? = null,

    /**
     * Cookie创建时间戳
     */
    @DatabaseField(columnName = "create_time")
    var createTime: Long = System.currentTimeMillis(),

    /**
     * Cookie更新时间戳
     */
    @DatabaseField(columnName = "update_time")
    var updateTime: Long = System.currentTimeMillis(),

    /**
     * Cookie过期时间戳（可选）
     */
    @DatabaseField(columnName = "expire_time", canBeNull = true)
    var expireTime: Long? = null,

    /**
     * 最后使用时间戳
     */
    @DatabaseField(columnName = "last_used_time", canBeNull = true)
    var lastUsedTime: Long? = null,

    /**
     * Cookie状态：VALID(有效), EXPIRED(已过期), INVALID(无效)
     */
    @DatabaseField(columnName = "cookie_status", width = 10)
    var cookieStatus: String = "VALID",

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
    constructor(bilibiliBindingId: Long, playerUuid: String) : this(
        id = 0,
        bilibiliBindingId = bilibiliBindingId,
        playerUuid = playerUuid
    )

    /**
     * Cookie状态枚举
     */
    enum class Status(val value: String) {
        VALID("VALID"),
        EXPIRED("EXPIRED"),
        INVALID("INVALID");

        companion object {
            fun fromValue(value: String): Status {
                return values().find { it.value == value } ?: VALID
            }
        }
    }

    /**
     * 获取Cookie状态枚举
     */
    fun getCookieStatusEnum(): Status = Status.fromValue(cookieStatus)

    /**
     * 设置Cookie状态
     */
    fun setCookieStatus(status: Status) {
        cookieStatus = status.value
        updateTime = System.currentTimeMillis()
    }

    /**
     * 更新最后使用时间
     */
    fun updateLastUsedTime() {
        lastUsedTime = System.currentTimeMillis()
        updateTime = System.currentTimeMillis()
    }

    /**
     * 更新激活状态
     */
    fun updateActiveStatus(active: Boolean) {
        isActive = active
        updateTime = System.currentTimeMillis()
    }

    /**
     * 检查Cookie是否有效
     */
    fun isValidCookie(): Boolean {
        return isActive && getCookieStatusEnum() == Status.VALID && !isExpiredByTime()
    }

    /**
     * 根据过期时间检查是否已过期
     */
    fun isExpiredByTime(): Boolean {
        return expireTime?.let { it < System.currentTimeMillis() } ?: false
    }

    /**
     * 检查Cookie是否长时间未使用
     */
    fun isUnusedForDays(days: Int): Boolean {
        val lastUsed = lastUsedTime ?: createTime
        val threshold = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        return lastUsed < threshold
    }

    /**
     * 获取完整的Cookie字符串用于HTTP请求
     */
    fun toCookieString(): String {
        val cookies = mutableListOf<String>()

        sessData?.let { cookies.add("SESSDATA=$it") }
        biliJct?.let { cookies.add("bili_jct=$it") }
        dedeUserId?.let { cookies.add("DedeUserID=$it") }
        dedeUserIdCkMd5?.let { cookies.add("DedeUserID__ckMd5=$it") }
        sid?.let { cookies.add("sid=$it") }
        buvid3?.let { cookies.add("buvid3=$it") }
        buvid4?.let { cookies.add("buvid4=$it") }
        uuid?.let { cookies.add("uuid=$it") }
        bfeId?.let { cookies.add("bfe_id=$it") }
        bNut?.let { cookies.add("b_nut=$it") }
        bLsid?.let { cookies.add("b_lsid=$it") }

        return cookies.joinToString("; ")
    }

    /**
     * 从Cookie字符串解析并更新字段
     */
    fun fromCookieString(cookieString: String) {
        rawCookies = cookieString

        // 解析Cookie字符串
        cookieString.split(";").forEach { cookie ->
            val parts = cookie.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0].trim()
                val value = parts[1].trim()

                when (name) {
                    "SESSDATA" -> sessData = value
                    "bili_jct" -> biliJct = value
                    "DedeUserID" -> dedeUserId = value
                    "DedeUserID__ckMd5" -> dedeUserIdCkMd5 = value
                    "sid" -> sid = value
                    "buvid3" -> buvid3 = value
                    "buvid4" -> buvid4 = value
                    "uuid" -> uuid = value
                    "bfe_id" -> bfeId = value
                    "b_nut" -> bNut = value
                    "b_lsid" -> bLsid = value
                }
            }
        }

        updateTime = System.currentTimeMillis()
    }

    /**
     * 检查是否包含必要的Cookie字段
     */
    fun hasEssentialCookies(): Boolean {
        return !sessData.isNullOrBlank() &&
                !biliJct.isNullOrBlank() &&
                !dedeUserId.isNullOrBlank()
    }

    override fun toString(): String {
        return "BilibiliCookie(playerUuid='$playerUuid', bilibiliBindingId=$bilibiliBindingId, cookieStatus='$cookieStatus', isActive=$isActive, hasEssential=${hasEssentialCookies()})"
    }
}