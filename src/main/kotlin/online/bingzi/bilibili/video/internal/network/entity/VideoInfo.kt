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