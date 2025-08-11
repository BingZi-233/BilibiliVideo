package online.bingzi.bilibili.video.api.event.database.binding

/**
 * QQ绑定创建事件
 * 当玩家绑定QQ账号时触发
 * 
 * @param playerUuid 玩家UUID
 * @param qqNumber QQ号码
 * @param success 是否绑定成功
 * @param errorMessage 错误信息，成功时为 null
 */
class QQBindingCreateEvent(
    override val playerUuid: String,
    val qqNumber: String,
    val success: Boolean,
    val errorMessage: String? = null
) : BindingEvent()