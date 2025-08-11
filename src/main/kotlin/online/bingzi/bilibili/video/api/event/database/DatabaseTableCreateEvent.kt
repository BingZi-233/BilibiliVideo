package online.bingzi.bilibili.video.api.event.database

/**
 * 数据表创建事件
 * 当创建数据表时触发
 * 
 * @param tableName 表名
 * @param success 是否创建成功
 * @param errorMessage 错误信息，成功时为 null
 */
class DatabaseTableCreateEvent(
    val tableName: String,
    val success: Boolean,
    val errorMessage: String? = null
) : DatabaseEvent()