package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * UP主监控配置实体
 * 存储需要监控的UP主信息
 */
@DatabaseTable(tableName = "uploader_config")
class UploaderConfig {
    /**
     * UP主UID（主键）
     */
    @DatabaseField(id = true, columnName = "uploader_uid")
    var uploaderUid: Long = 0

    /**
     * UP主名称
     */
    @DatabaseField(columnName = "uploader_name", canBeNull = false)
    var uploaderName: String = ""

    /**
     * 是否启用监控
     */
    @DatabaseField(columnName = "is_enabled")
    var isEnabled: Boolean = true

    /**
     * 上次同步时间
     */
    @DatabaseField(columnName = "last_sync_time")
    var lastSyncTime: Long = 0

    /**
     * 同步间隔（小时）
     */
    @DatabaseField(columnName = "sync_interval_hours")
    var syncIntervalHours: Int = 24

    /**
     * 创建时间
     */
    @DatabaseField(columnName = "created_at")
    var createdAt: Long = System.currentTimeMillis()

    /**
     * 更新时间
     */
    @DatabaseField(columnName = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    /**
     * 无参构造函数（OrmLite要求）
     */
    constructor()

    /**
     * 带参构造函数
     */
    constructor(uploaderUid: Long, uploaderName: String, syncIntervalHours: Int = 24) {
        this.uploaderUid = uploaderUid
        this.uploaderName = uploaderName
        this.syncIntervalHours = syncIntervalHours
    }

    /**
     * 更新同步时间
     */
    fun updateSyncTime() {
        this.lastSyncTime = System.currentTimeMillis()
        this.updatedAt = System.currentTimeMillis()
    }

    /**
     * 检查是否需要同步
     */
    fun needsSync(): Boolean {
        if (!isEnabled) return false
        val currentTime = System.currentTimeMillis()
        val intervalMillis = syncIntervalHours * 60 * 60 * 1000L
        return currentTime - lastSyncTime >= intervalMillis
    }
}