package online.bingzi.bilibili.video.api.event.database.player

/**
 * 玩家数据删除事件
 * 当删除玩家数据时触发
 * 
 * @param playerUuid 玩家UUID
 * @param success 是否删除成功
 * @param errorMessage 错误信息，成功时为 null
 */
class PlayerDataDeleteEvent(
    override val playerUuid: String,
    val success: Boolean,
    val errorMessage: String? = null
) : PlayerDataEvent()