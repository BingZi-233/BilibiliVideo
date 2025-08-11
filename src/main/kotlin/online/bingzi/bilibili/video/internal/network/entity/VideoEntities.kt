package online.bingzi.bilibili.video.internal.network.entity

/**
 * 视频信息
 */
data class VideoInfo(
    val aid: Long,              // AV 号
    val bvid: String,           // BV 号
    val title: String,          // 标题
    val description: String?,   // 描述
    val cover: String?,         // 封面
    val publishTime: Long?,     // 发布时间戳
    val duration: Int?,         // 时长（秒）
    val stats: VideoStats,      // 统计信息
    val uploader: UploaderInfo  // UP主信息
)

/**
 * 视频统计信息
 */
data class VideoStats(
    val view: Long,      // 播放量
    val danmaku: Long,   // 弹幕数
    val reply: Long,     // 评论数
    val favorite: Long,  // 收藏数
    val coin: Long,      // 投币数
    val share: Long,     // 分享数
    val like: Long       // 点赞数
)

/**
 * UP主信息
 */
data class UploaderInfo(
    val uid: Long,        // UID
    val name: String,     // 用户名
    val avatar: String?   // 头像
)

/**
 * 一键三连状态
 */
data class TripleActionStatus(
    val aid: Long,         // 视频 AV 号
    val liked: Boolean,    // 是否已点赞
    val coined: Boolean,   // 是否已投币
    val favorited: Boolean // 是否已收藏
)

/**
 * 一键三连操作结果
 */
data class TripleActionResult(
    val liked: Boolean,    // 点赞是否成功
    val coined: Boolean,   // 投币是否成功
    val favorited: Boolean // 收藏是否成功
)

/**
 * 包含视频信息和三连状态的结果类
 * @param videoInfo 视频信息
 * @param tripleStatus 三连状态（如果已登录）
 */
data class VideoWithTripleStatus(
    val videoInfo: VideoInfo,
    val tripleStatus: TripleActionStatus?
)