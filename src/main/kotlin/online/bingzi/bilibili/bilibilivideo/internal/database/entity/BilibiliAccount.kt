package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * Bilibili账户信息实体类
 * 
 * 存储Bilibili用户的完整认证信息，包括Cookie和刷新令牌。
 * 基于二维码登录API的响应结构设计，支持Cookie自动刷新机制。
 * 
 * @property mid Bilibili用户MID（主键，即DedeUserID）
 * @property nickname Bilibili用户昵称
 * @property sessdata SESSDATA Cookie，用于身份验证
 * @property buvid3 buvid3 Cookie，设备标识符，用于风控检测
 * @property biliJct bili_jct Cookie，CSRF令牌，用于API请求验证
 * @property refreshToken 刷新令牌，用于Cookie过期后的自动刷新
 * @property createTime 记录创建时间戳（毫秒）
 * @property updateTime 记录最后更新时间戳（毫秒）
 * @property createPlayer 创建记录的玩家名称
 * @property updatePlayer 最后更新记录的玩家名称
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class BilibiliAccount(
    val mid: Long,
    val nickname: String,
    val sessdata: String,
    val buvid3: String,
    val biliJct: String,
    val refreshToken: String,
    val createTime: Long,
    val updateTime: Long,
    val createPlayer: String,
    val updatePlayer: String
)