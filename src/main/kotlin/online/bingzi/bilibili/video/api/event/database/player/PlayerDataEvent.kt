package online.bingzi.bilibili.video.api.event.database.player

import online.bingzi.bilibili.video.api.event.database.DatabaseEvent

/**
 * 玩家数据事件基类
 * 所有玩家数据相关事件的基础类
 */
abstract class PlayerDataEvent : DatabaseEvent() {
    abstract val playerUuid: String
}