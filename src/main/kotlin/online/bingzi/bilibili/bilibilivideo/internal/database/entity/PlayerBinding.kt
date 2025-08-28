package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * 玩家-MID绑定实体类
 * 表示 Minecraft 玩家与 Bilibili MID 的一对一绑定关系
 */
data class PlayerBinding(
    val playerUuid: String,    // 玩家UUID
    val mid: Long,             // B站MID (即DedeUserID)
    val createTime: Long,      // 创建时间戳
    val updateTime: Long,      // 更新时间戳
    val createPlayer: String,  // 创建玩家名
    val updatePlayer: String   // 更新玩家名
)