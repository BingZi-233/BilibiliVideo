package online.bingzi.bilibili.video.internal.network.entity

/**
 * 包含视频信息和三连状态的结果类
 * @param videoInfo 视频信息
 * @param tripleStatus 三连状态（如果已登录）
 */
data class VideoWithTripleStatus(
    val videoInfo: VideoInfo,
    val tripleStatus: TripleActionStatus?
)