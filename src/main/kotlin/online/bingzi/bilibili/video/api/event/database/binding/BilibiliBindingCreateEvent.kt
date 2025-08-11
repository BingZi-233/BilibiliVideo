package online.bingzi.bilibili.video.api.event.database.binding

/**
 * Bilibili绑定创建事件
 * 当玩家绑定Bilibili账号时触发
 * 
 * @param playerUuid 玩家UUID
 * @param bilibiliUid Bilibili UID
 * @param success 是否绑定成功
 * @param errorMessage 错误信息，成功时为 null
 */
class BilibiliBindingCreateEvent(
    override val playerUuid: String,
    val bilibiliUid: Long,
    val success: Boolean,
    val errorMessage: String? = null
) : BindingEvent()