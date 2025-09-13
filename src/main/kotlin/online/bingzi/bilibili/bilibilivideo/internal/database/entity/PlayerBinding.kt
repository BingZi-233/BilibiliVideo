package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * 玩家MID绑定实体类
 * 
 * 表示Minecraft玩家与Bilibili MID的一对一绑定关系。
 * 确保每个Minecraft玩家只能绑定一个Bilibili账户，
 * 每个Bilibili账户也只能被一个Minecraft玩家绑定。
 * 
 * @property playerUuid Minecraft玩家的UUID字符串
 * @property mid Bilibili用户MID（即DedeUserID）
 * @property createTime 记录创建时间戳（毫秒）
 * @property updateTime 记录最后更新时间戳（毫秒）
 * @property createPlayer 创建记录的玩家名称
 * @property updatePlayer 最后更新记录的玩家名称
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class PlayerBinding(
    val playerUuid: String,
    val mid: Long,
    val createTime: Long,
    val updateTime: Long,
    val createPlayer: String,
    val updatePlayer: String
)