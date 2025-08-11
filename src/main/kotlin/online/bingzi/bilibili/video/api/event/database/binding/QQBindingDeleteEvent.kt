package online.bingzi.bilibili.video.api.event.database.binding

/**
 * QQ绑定删除事件
 * 当玩家解绑QQ账号时触发
 * 
 * @param playerUuid 玩家UUID
 * @param qqNumber QQ号码
 * @param success 是否解绑成功
 * @param errorMessage 错误信息，成功时为 null
 */
class QQBindingDeleteEvent(
    override val playerUuid: String,
    val qqNumber: String,
    val success: Boolean,
    val errorMessage: String? = null
) : BindingEvent()