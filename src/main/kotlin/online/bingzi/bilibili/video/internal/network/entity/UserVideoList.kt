package online.bingzi.bilibili.video.internal.network.entity

/**
 * 用户上传视频列表响应
 * 
 * @param videos 视频列表
 * @param total 总视频数量
 * @param pages 总页数
 * @param currentPage 当前页码
 * @param pageSize 每页数量
 */
data class UserVideoListResponse(
    val videos: List<UserVideoInfo>,
    val total: Long,
    val pages: Int,
    val currentPage: Int,
    val pageSize: Int
)

/**
 * 用户视频信息（简化版本）
 * 专门用于视频列表展示
 * 
 * @param aid 视频AV号
 * @param bvid 视频BV号
 * @param title 视频标题
 * @param description 视频描述
 * @param cover 封面图片URL
 * @param duration 视频时长（秒）
 * @param publishTime 发布时间戳
 * @param videoStats 视频统计数据
 * @param author 作者信息（UID和用户名）
 */
data class UserVideoInfo(
    val aid: Long,
    val bvid: String,
    val title: String,
    val description: String?,
    val cover: String?,
    val duration: Int?,
    val publishTime: Long?,
    val videoStats: SimpleVideoStats,
    val author: VideoAuthor
)

/**
 * 简化的视频统计数据
 * 用于视频列表展示
 * 
 * @param view 播放数
 * @param danmaku 弹幕数
 * @param reply 评论数
 * @param favorite 收藏数
 * @param coin 投币数
 * @param share 分享数
 * @param like 点赞数
 */
data class SimpleVideoStats(
    val view: Long,
    val danmaku: Long,
    val reply: Long,
    val favorite: Long,
    val coin: Long,
    val share: Long,
    val like: Long
)

/**
 * 视频作者信息（简化版本）
 * 
 * @param uid 用户UID
 * @param name 用户名
 * @param avatar 头像URL
 */
data class VideoAuthor(
    val uid: Long,
    val name: String,
    val avatar: String?
)