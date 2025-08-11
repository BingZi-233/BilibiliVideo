package online.bingzi.bilibili.video.api.event.network.user

import online.bingzi.bilibili.video.api.event.network.BilibiliUserEvent
import online.bingzi.bilibili.video.internal.network.entity.UserVideoListResponse

/**
 * 用户视频列表获取事件
 * 当获取指定用户的上传视频列表时触发
 * 
 * @param uid 用户UID
 * @param page 请求的页码
 * @param pageSize 每页数量
 * @param videoList 视频列表响应，获取失败时为 null
 * @param success 是否获取成功
 * @param errorMessage 错误信息，成功时为 null
 */
class UserVideoListFetchEvent(
    val uid: Long,
    val page: Int,
    val pageSize: Int,
    val videoList: UserVideoListResponse?,
    val success: Boolean,
    val errorMessage: String? = null
) : BilibiliUserEvent()