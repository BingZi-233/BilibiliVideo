package online.bingzi.bilibili.video.api.event.database.binding

/**
 * Bilibili绑定删除事件
 * 当玩家解绑Bilibili账号时触发
 * 
 * @param playerUuid 玩家UUID
 * @param bilibiliUid Bilibili UID
 * @param success 是否解绑成功
 * @param errorMessage 错误信息，成功时为 null
 */
class BilibiliBindingDeleteEvent(
    override val playerUuid: String,
    val bilibiliUid: Long,
    val success: Boolean,
    val errorMessage: String? = null
) : BindingEvent()