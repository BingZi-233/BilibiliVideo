package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * UP主关注状态实体类
 * 记录玩家对UP主的关注状态
 * 支持N对N关系：一个玩家可以关注多个UP主，一个UP主可以被多个玩家关注
 */
data class UpFollowStatus(
    val id: Long? = null,      // 主键，自增
    val upMid: Long,           // UP主MID
    val followerMid: Long,     // 关注者MID
    val playerUuid: String,    // 玩家UUID
    val isFollowing: Boolean,  // 是否关注
    val createTime: Long,      // 创建时间戳
    val updateTime: Long,      // 更新时间戳
    val createPlayer: String,  // 创建玩家名
    val updatePlayer: String   // 更新玩家名
)