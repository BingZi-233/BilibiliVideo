package online.bingzi.bilibili.video.api.event.network

import taboolib.platform.type.BukkitProxyEvent

/**
 * 网络事件基类
 * 所有网络相关事件的基础类
 *
 * 提供了网络操作的通用事件基础，包括：
 * - 登录相关事件 (BilibiliLoginEvent)
 * - 视频操作事件 (BilibiliVideoEvent)
 * - 用户操作事件 (BilibiliUserEvent)
 * - API 请求事件 (BilibiliApiEvent)
 *
 * @property timestamp 事件触发时间戳
 */
abstract class NetworkEvent(
    val timestamp: Long = System.currentTimeMillis()
) : BukkitProxyEvent()