package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * Bilibili账户信息实体类
 * 存储B站用户的认证信息，包括Cookie和刷新令牌
 */
data class BilibiliAccount(
    val mid: Long,             // B站MID (主键)
    val nickname: String,      // B站昵称
    val sessdata: String,      // SESSDATA Cookie
    val buvid3: String,        // buvid3 Cookie (设备标识)
    val biliJct: String,       // bili_jct Cookie (CSRF token)
    val refreshToken: String,  // 刷新令牌
    val createTime: Long,      // 创建时间戳
    val updateTime: Long,      // 更新时间戳
    val createPlayer: String,  // 创建玩家名
    val updatePlayer: String   // 更新玩家名
)