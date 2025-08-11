package online.bingzi.bilibili.video.internal.database.entity

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * UP主视频信息实体
 * 存储UP主的视频BV号和相关信息
 */
@DatabaseTable(tableName = "uploader_videos")
class UploaderVideo {
    /**
     * 自增主键ID
     */
    @DatabaseField(generatedId = true, columnName = "id")
    var id: Long = 0

    /**
     * UP主UID
     */
    @DatabaseField(columnName = "uploader_uid", canBeNull = false, index = true)
    var uploaderUid: Long = 0

    /**
     * UP主名称
     */
    @DatabaseField(columnName = "uploader_name", canBeNull = false)
    var uploaderName: String = ""

    /**
     * 视频BV号
     */
    @DatabaseField(columnName = "bv_id", canBeNull = false, unique = true, index = true)
    var bvId: String = ""

    /**
     * 视频标题
     */
    @DatabaseField(columnName = "title", canBeNull = false)
    var title: String = ""

    /**
     * 视频描述
     */
    @DatabaseField(columnName = "description", columnDefinition = "TEXT")
    var description: String = ""

    /**
     * 视频发布时间（时间戳）
     */
    @DatabaseField(columnName = "publish_time")
    var publishTime: Long = 0

    /**
     * 视频时长（秒）
     */
    @DatabaseField(columnName = "duration")
    var duration: Int = 0

    /**
     * 播放量
     */
    @DatabaseField(columnName = "view_count")
    var viewCount: Long = 0

    /**
     * 点赞数
     */
    @DatabaseField(columnName = "like_count")
    var likeCount: Long = 0

    /**
     * 投币数
     */
    @DatabaseField(columnName = "coin_count")
    var coinCount: Long = 0

    /**
     * 收藏数
     */
    @DatabaseField(columnName = "favorite_count")
    var favoriteCount: Long = 0

    /**
     * 分享数
     */
    @DatabaseField(columnName = "share_count")
    var shareCount: Long = 0

    /**
     * 弹幕数
     */
    @DatabaseField(columnName = "danmaku_count")
    var danmakuCount: Long = 0

    /**
     * 视频封面URL
     */
    @DatabaseField(columnName = "cover_url")
    var coverUrl: String = ""

    /**
     * 记录创建时间
     */
    @DatabaseField(columnName = "created_at")
    var createdAt: Long = System.currentTimeMillis()

    /**
     * 记录更新时间
     */
    @DatabaseField(columnName = "updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    /**
     * 是否有效（用于软删除）
     */
    @DatabaseField(columnName = "is_active")
    var isActive: Boolean = true

    /**
     * 无参构造函数（OrmLite要求）
     */
    constructor()

    /**
     * 带参构造函数
     */
    constructor(
        uploaderUid: Long,
        uploaderName: String,
        bvId: String,
        title: String,
        publishTime: Long = 0
    ) {
        this.uploaderUid = uploaderUid
        this.uploaderName = uploaderName
        this.bvId = bvId
        this.title = title
        this.publishTime = publishTime
    }

    /**
     * 更新记录时间
     */
    fun updateTimestamp() {
        this.updatedAt = System.currentTimeMillis()
    }

    /**
     * 更新视频统计信息
     */
    fun updateStats(
        viewCount: Long,
        likeCount: Long,
        coinCount: Long,
        favoriteCount: Long,
        shareCount: Long,
        danmakuCount: Long
    ) {
        this.viewCount = viewCount
        this.likeCount = likeCount
        this.coinCount = coinCount
        this.favoriteCount = favoriteCount
        this.shareCount = shareCount
        this.danmakuCount = danmakuCount
        updateTimestamp()
    }
}