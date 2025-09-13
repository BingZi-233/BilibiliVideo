package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * 视频三连状态实体类
 * 
 * 记录玩家对特定视频的三连操作状态（点赞、投币、收藏）。
 * 支持N对N关系：一个玩家可以对多个视频进行三连，一个视频可以被多个玩家三连。
 * 使用联合唯一索引(bvid, mid, player_uuid)确保每个玩家对每个视频只有一条记录。
 * 
 * @property id 主键，自增长ID，可为null用于插入时自动生成
 * @property bvid 视频BV号，Bilibili视频的唯一标识符
 * @property mid Bilibili用户MID，标识执行三连的用户
 * @property playerUuid Minecraft玩家UUID，用于关联游戏内玩家
 * @property isLiked 是否已点赞该视频
 * @property isCoined 是否已投币该视频
 * @property isFavorited 是否已收藏该视频
 * @property createTime 记录创建时间戳（毫秒）
 * @property updateTime 记录最后更新时间戳（毫秒）
 * @property createPlayer 创建记录的玩家名称
 * @property updatePlayer 最后更新记录的玩家名称
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class VideoTripleStatus(
    val id: Long? = null,
    val bvid: String,
    val mid: Long,
    val playerUuid: String,
    val isLiked: Boolean,
    val isCoined: Boolean,
    val isFavorited: Boolean,
    val createTime: Long,
    val updateTime: Long,
    val createPlayer: String,
    val updatePlayer: String
)