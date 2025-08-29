package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.internal.event.EventManager
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import org.bukkit.entity.Player
import taboolib.platform.util.sendError
import taboolib.platform.util.sendInfo

object LogoutCommandHandler {
    
    fun handleLogout(player: Player) {
        // 检查玩家是否已登录
        val session = SessionManager.getSession(player)
        if (session == null) {
            player.sendError("commandsLogoutNotLoggedIn")
            return
        }
        
        player.sendInfo("commandsLogoutLoggingOut")
        
        // 获取用户昵称用于提示
        val nickname = session.nickname
        
        // 触发BilibiliLogoutEvent事件
        EventManager.callBilibiliLogoutEvent(player, session)
        
        // 清除内存中的会话缓存
        SessionManager.removeSession(player)
        
        // 取消正在进行的登录任务（如果有的话）
        LoginCommandHandler.cancelLogin(player)
        
        player.sendInfo("commandsLogoutSuccess", "nickname" to nickname)
        player.sendInfo("commandsLogoutNote")
    }
}