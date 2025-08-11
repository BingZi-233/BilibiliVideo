package online.bingzi.bilibili.video.api.event.database.player

/**
 * 玩家数据更新事件
 * 当更新玩家数据时触发
 * 
 * @param playerUuid 玩家UUID
 * @param fieldName 更新的字段名称
 * @param oldValue 旧值
 * @param newValue 新值
 * @param success 是否更新成功
 * @param errorMessage 错误信息，成功时为 null
 */
class PlayerDataUpdateEvent(
    override val playerUuid: String,
    val fieldName: String,
    val oldValue: Any?,
    val newValue: Any?,
    val success: Boolean,
    val errorMessage: String? = null
) : PlayerDataEvent()