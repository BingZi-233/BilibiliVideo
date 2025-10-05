package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.internal.bilibili.api.VideoApi
import online.bingzi.bilibili.bilibilivideo.internal.database.service.DatabaseService
import online.bingzi.bilibili.bilibilivideo.internal.event.EventManager
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import org.bukkit.entity.Player
import taboolib.platform.util.sendInfo
import taboolib.platform.util.sendWarn
import taboolib.platform.util.sendError

object TripleStatusCommandHandler {
    
    fun handleTripleStatus(player: Player, bvid: String) {
        // 检查玩家是否已登录
        val session = SessionManager.getSession(player)
        if (session == null) {
            player.sendError("commandsTripleNotLoggedIn")
            return
        }
        
        player.sendInfo("commandsTripleQuerying", bvid)
        
        // 调用API查询三连状态
        VideoApi.getTripleStatus(
            bvid = bvid,
            sessdata = session.sessdata,
            buvid3 = session.buvid3
        ) { tripleData ->
            if (tripleData != null) {
                // 更新数据，添加玩家信息
                val updatedTripleData = tripleData.copy(
                    playerUuid = player.uniqueId.toString(),
                    mid = session.mid
                )
                
                // 保存到数据库
                DatabaseService.saveVideoTripleStatus(
                    bvid = updatedTripleData.bvid,
                    mid = updatedTripleData.mid,
                    playerUuid = updatedTripleData.playerUuid,
                    isLiked = updatedTripleData.isLiked,
                    isCoined = updatedTripleData.coinCount > 0,
                    isFavorited = updatedTripleData.isFavorited,
                    playerName = player.name
                ) { success ->
                    if (success) {
                        // 发送结果给玩家
                        player.sendInfo("commandsTripleQueryComplete")
                        player.sendInfo("commandsTripleVideo", bvid)
                        player.sendInfo("commandsTripleStatus", updatedTripleData.getStatusMessage())
                        
                        if (updatedTripleData.hasTripleAction()) {
                            player.sendInfo("commandsTripleCompleted")
                        } else {
                            player.sendWarn("commandsTripleIncomplete")
                        }
                        
                        // 触发VideoTripleStatusCheckEvent事件
                        EventManager.callVideoTripleStatusCheckEvent(player, updatedTripleData)
                    } else {
                        player.sendError("commandsTripleSaveFailed")
                    }
                }
            } else {
                player.sendError("commandsTripleQueryFailed")
            }
        }
    }
}
