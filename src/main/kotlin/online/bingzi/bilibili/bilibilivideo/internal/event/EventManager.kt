package online.bingzi.bilibili.bilibilivideo.internal.event

import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLoginEvent
import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLogoutEvent
import online.bingzi.bilibili.bilibilivideo.api.event.UpFollowStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.api.event.VideoTripleStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player

/**
 * 事件管理器
 * 
 * 统一管理和触发插件的自定义事件，为其他插件提供监听接口。
 * 封装事件创建和调用逻辑，确保事件参数的正确传递。
 * 
 * 支持的事件类型：
 * - Bilibili登录/登出事件
 * - 视频三连状态检查事件
 * - UP主关注状态检查事件
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object EventManager {
    
    /**
     * 触发视频三连状态检查事件
     * 
     * 当玩家检查视频三连状态时调用此方法。
     * 
     * @param player 执行检查的玩家
     * @param tripleData 视频三连数据
     */
    fun callVideoTripleStatusCheckEvent(player: Player, tripleData: VideoTripleData) {
        VideoTripleStatusCheckEvent(player, tripleData).call()
    }
    
    /**
     * 触发UP主关注状态检查事件
     * 
     * 当玩家检查UP主关注状态时调用此方法。
     * 
     * @param player 执行检查的玩家
     * @param followData UP主关注数据
     */
    fun callUpFollowStatusCheckEvent(player: Player, followData: UpFollowData) {
        UpFollowStatusCheckEvent(player, followData).call()
    }
    
    /**
     * 触发Bilibili登录事件
     * 
     * 当玩家成功登录Bilibili账户时调用此方法。
     * 
     * @param player 登录的玩家
     * @param session 登录会话信息
     */
    fun callBilibiliLoginEvent(player: Player, session: LoginSession) {
        BilibiliLoginEvent(player, session).call()
    }
    
    /**
     * 触发Bilibili登出事件
     * 
     * 当玩家退出登录Bilibili账户时调用此方法。
     * 
     * @param player 登出的玩家
     * @param previousSession 之前的登录会话信息
     */
    fun callBilibiliLogoutEvent(player: Player, previousSession: LoginSession) {
        BilibiliLogoutEvent(player, previousSession).call()
    }
}