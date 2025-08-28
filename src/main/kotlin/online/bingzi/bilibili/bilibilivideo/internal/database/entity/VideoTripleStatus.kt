package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * 视频三连状态实体类
 * 记录玩家对某个视频的点赞、投币、收藏状态
 * 支持N对N关系：一个玩家可以有多个视频记录，一个视频可以被多个玩家三连
 */
data class VideoTripleStatus(
    val id: Long? = null,      // 主键，自增
    val bvid: String,          // 视频BV号
    val mid: Long,             // B站MID
    val playerUuid: String,    // 玩家UUID
    val isLiked: Boolean,      // 是否点赞
    val isCoined: Boolean,     // 是否投币
    val isFavorited: Boolean,  // 是否收藏
    val createTime: Long,      // 创建时间戳
    val updateTime: Long,      // 更新时间戳
    val createPlayer: String,  // 创建玩家名
    val updatePlayer: String   // 更新玩家名
)