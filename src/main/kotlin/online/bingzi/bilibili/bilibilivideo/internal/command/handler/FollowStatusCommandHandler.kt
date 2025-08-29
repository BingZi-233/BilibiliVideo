package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.api.UserApi
import online.bingzi.bilibili.bilibilivideo.internal.database.service.DatabaseService
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import org.bukkit.entity.Player
import taboolib.platform.util.sendError
import taboolib.platform.util.sendInfo
import taboolib.platform.util.sendWarn

object FollowStatusCommandHandler {
    
    fun handleFollowStatus(player: Player, mid: Long) {
        // 检查玩家是否已登录
        val session = SessionManager.getSession(player)
        if (session == null) {
            player.sendError("commandsFollowNotLoggedIn")
            return
        }
        
        player.sendInfo("commandsFollowQuerying", mid)
        
        // 调用API查询关注状态
        UserApi.getFollowStatus(
            upMid = mid,
            sessdata = session.sessdata
        ) { followData ->
            if (followData != null) {
                // 更新数据，添加玩家信息
                val updatedFollowData = followData.copy(
                    followerMid = session.mid,
                    playerUuid = player.uniqueId.toString()
                )
                
                // 保存到数据库
                DatabaseService.saveUpFollowStatus(
                    upMid = updatedFollowData.upMid,
                    followerMid = updatedFollowData.followerMid,
                    playerUuid = updatedFollowData.playerUuid,
                    isFollowing = updatedFollowData.isFollowing,
                    playerName = player.name
                ) { success ->
                    if (success) {
                        // 发送结果给玩家
                        player.sendInfo("commandsFollowQueryComplete")
                        player.sendInfo("commandsFollowUpmaster", updatedFollowData.upName, updatedFollowData.upMid)
                        player.sendInfo("commandsFollowStatus", updatedFollowData.getStatusMessage())
                        
                        if (updatedFollowData.isFollowing) {
                            player.sendInfo("commandsFollowFollowing")
                        } else {
                            player.sendWarn("commandsFollowNotFollowing")
                        }
                        
                        // TODO: 触发UpFollowStatusCheckEvent事件
                        // EventManager.callUpFollowStatusCheckEvent(player, updatedFollowData)
                    } else {
                        player.sendError("commandsFollowSaveFailed")
                    }
                }
            } else {
                player.sendError("commandsFollowQueryFailed")
            }
        }
    }
}