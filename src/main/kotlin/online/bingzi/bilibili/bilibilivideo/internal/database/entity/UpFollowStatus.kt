package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * UP主关注状态实体类
 * 
 * 记录玩家对UP主的关注状态，支持关注关系的跟踪和管理。
 * 支持N对N关系：一个玩家可以关注多个UP主，一个UP主可以被多个玩家关注。
 * 使用联合唯一索引(up_mid, follower_mid, player_uuid)确保每个关注关系只有一条记录。
 * 
 * @property id 主键，自增长ID，可为null用于插入时自动生成
 * @property upMid UP主的Bilibili MID
 * @property followerMid 关注者的Bilibili MID
 * @property playerUuid Minecraft玩家UUID，用于关联游戏内玩家
 * @property isFollowing 是否正在关注该UP主
 * @property createTime 记录创建时间戳（毫秒）
 * @property updateTime 记录最后更新时间戳（毫秒）
 * @property createPlayer 创建记录的玩家名称
 * @property updatePlayer 最后更新记录的玩家名称
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class UpFollowStatus(
    val id: Long? = null,
    val upMid: Long,
    val followerMid: Long,
    val playerUuid: String,
    val isFollowing: Boolean,
    val createTime: Long,
    val updateTime: Long,
    val createPlayer: String,
    val updatePlayer: String
)