package online.bingzi.bilibili.video.api.event.database

/**
 * 数据库初始化事件
 * 当数据库初始化时触发
 * 
 * @param databaseType 数据库类型（如 SQLite, MySQL）
 * @param success 是否初始化成功
 * @param errorMessage 错误信息，成功时为 null
 */
class DatabaseInitializeEvent(
    val databaseType: String,
    val success: Boolean,
    val errorMessage: String? = null
) : DatabaseEvent()