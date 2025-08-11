package online.bingzi.bilibili.video.api.event.database

/**
 * 数据库连接事件
 * 当数据库连接建立或断开时触发
 * 
 * @param connected 是否已连接
 * @param databaseInfo 数据库信息（如连接字符串）
 * @param errorMessage 错误信息，成功时为 null
 */
class DatabaseConnectionEvent(
    val connected: Boolean,
    val databaseInfo: String,
    val errorMessage: String? = null
) : DatabaseEvent()