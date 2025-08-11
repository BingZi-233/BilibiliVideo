package online.bingzi.bilibili.video.api.event.database.player

/**
 * 玩家数据创建事件
 * 当创建新的玩家数据记录时触发
 * 
 * @param playerUuid 玩家UUID
 * @param playerName 玩家名称
 * @param success 是否创建成功
 * @param errorMessage 错误信息，成功时为 null
 */
class PlayerDataCreateEvent(
    override val playerUuid: String,
    val playerName: String,
    val success: Boolean,
    val errorMessage: String? = null
) : PlayerDataEvent()