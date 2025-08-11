package online.bingzi.bilibili.video.api.event.database.binding

import online.bingzi.bilibili.video.api.event.database.DatabaseEvent

/**
 * 绑定事件基类
 * 所有绑定相关事件的基础类
 */
abstract class BindingEvent : DatabaseEvent() {
    abstract val playerUuid: String
}