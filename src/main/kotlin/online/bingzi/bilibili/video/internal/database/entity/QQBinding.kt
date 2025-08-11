package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * QQ绑定实体
 * 存储玩家的QQ号绑定信息
 */
@DatabaseTable(tableName = "bv_qq_bindings")
data class QQBinding(
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
     * QQ号码
     */
    @DatabaseField(columnName = "qq_number", canBeNull = false, width = 20, uniqueIndex = true)
    var qqNumber: String = "",

    /**
     * QQ昵称（可选）
     */
    @DatabaseField(columnName = "qq_nickname", canBeNull = true, width = 50)
    var qqNickname: String? = null,

    /**
     * 绑定时间戳
     */
    @DatabaseField(columnName = "bind_time")
    var bindTime: Long = System.currentTimeMillis(),

    /**
     * 最后验证时间戳
     */
    @DatabaseField(columnName = "last_verify_time", canBeNull = true)
    var lastVerifyTime: Long? = null,

    /**
     * 绑定状态：ACTIVE(激活), PENDING(待验证), DISABLED(已禁用)
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
    constructor(playerUuid: String, qqNumber: String) : this(
        id = 0,
        playerUuid = playerUuid,
        qqNumber = qqNumber
    )

    /**
     * 绑定状态枚举
     */
    enum class Status(val value: String) {
        ACTIVE("ACTIVE"),
        PENDING("PENDING"),
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
     * 检查绑定是否有效（激活且不是禁用状态）
     */
    fun isValidBinding(): Boolean {
        return isActive && getBindStatusEnum() != Status.DISABLED
    }

    override fun toString(): String {
        return "QQBinding(playerUuid='$playerUuid', qqNumber='$qqNumber', bindStatus='$bindStatus', isActive=$isActive)"
    }
}