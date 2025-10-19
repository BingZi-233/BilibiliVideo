package online.bingzi.bilibili.bilibilivideo.api.event

import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player
import taboolib.platform.type.BukkitProxyEvent

/**
 * Bilibili登出事件
 * 
 * 当玩家退出登录Bilibili账户时触发此事件。
 * 其他插件可以监听此事件来处理用户登出后的清理工作。
 * 
 * @param player 登出的玩家
 * @param previousSession 之前的登录会话信息
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
class BilibiliLogoutEvent(
    val player: Player,
    val previousSession: LoginSession
) : BukkitProxyEvent() {
    
    /**
     * 获取用户MID
     * 
     * @return 已登出的Bilibili用户MID
     */
    fun getMid(): Long = previousSession.mid
    
    /**
     * 获取用户昵称
     * 
     * @return 已登出的Bilibili用户昵称
     */
    fun getNickname(): String = previousSession.nickname
    
    /**
     * 获取会话持续时间
     * 
     * @return 从登录到登出的时间长度（毫秒）
     */
    fun getSessionDuration(): Long = 
        System.currentTimeMillis() - previousSession.loginTime
}