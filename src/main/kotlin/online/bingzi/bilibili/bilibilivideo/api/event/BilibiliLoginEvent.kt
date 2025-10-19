package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * Bilibili登录事件
 * 
 * 当玩家成功登录Bilibili账户时触发此事件。
 * 其他插件可以监听此事件来获取玩家登录信息或执行相关操作。
 * 
 * @param player 登录的玩家
 * @param session 登录会话信息
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
class BilibiliLoginEvent(
    val player: Player,
    val session: LoginSession
) : BukkitProxyEvent() {
    
    /**
     * 获取用户MID
     * 
     * @return Bilibili用户MID
     */
    fun getMid(): Long = session.mid
    
    /**
     * 获取用户昵称
     * 
     * @return Bilibili用户昵称
     */
    fun getNickname(): String = session.nickname
    
    /**
     * 获取登录时间
     * 
     * @return 登录时间戳（毫秒）
     */
    fun getLoginTime(): Long = session.loginTime
}