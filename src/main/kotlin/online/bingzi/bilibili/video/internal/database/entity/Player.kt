package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

/**
 * 玩家基础信息实体
 * 存储玩家的基本信息和状态
 */
@DatabaseTable(tableName = "bv_players")
data class Player(
    /**
     * 玩家UUID，作为主键
     */
    @DatabaseField(id = true, columnName = "player_uuid")
    var playerUuid: String = "",

    /**
     * 玩家名称（最后已知的）
     */
    @DatabaseField(columnName = "player_name", width = 16)
    var playerName: String = "",

    /**
     * 首次绑定时间戳
     */
    @DatabaseField(columnName = "first_bind_time")
    var firstBindTime: Long = System.currentTimeMillis(),

    /**
     * 最后活跃时间戳
     */
    @DatabaseField(columnName = "last_active_time")
    var lastActiveTime: Long = System.currentTimeMillis(),

    /**
     * 是否激活状态，默认为true
     */
    @DatabaseField(columnName = "is_active")
    var isActive: Boolean = true,

    /**
     * 备注信息
     */
    @DatabaseField(columnName = "remark", canBeNull = true, width = 255)
    var remark: String? = null
) {
    /**
     * OrmLite需要的无参构造函数
     */
    constructor() : this("")

    /**
     * 根据UUID创建实体的构造函数
     */
    constructor(playerUuid: UUID, playerName: String = "") : this(playerUuid.toString(), playerName)

    /**
     * 获取玩家UUID对象
     */
    fun getPlayerUuidAsUuid(): UUID = UUID.fromString(playerUuid)

    /**
     * 设置玩家UUID
     */
    fun setPlayerUuid(uuid: UUID) {
        playerUuid = uuid.toString()
    }

    /**
     * 更新最后活跃时间
     */
    fun updateLastActiveTime() {
        lastActiveTime = System.currentTimeMillis()
    }

    /**
     * 更新激活状态
     */
    fun updateActiveStatus(active: Boolean) {
        isActive = active
    }

    override fun toString(): String {
        return "Player(playerUuid='$playerUuid', playerName='$playerName', isActive=$isActive)"
    }
}