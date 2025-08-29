package online.bingzi.bilibili.bilibilivideo.internal.event

import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLoginEvent
import online.bingzi.bilibili.bilibilivideo.api.event.BilibiliLogoutEvent
import online.bingzi.bilibili.bilibilivideo.api.event.UpFollowStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.api.event.VideoTripleStatusCheckEvent
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import online.bingzi.bilibili.bilibilivideo.internal.session.LoginSession
import org.bukkit.entity.Player

object EventManager {
    
    fun callVideoTripleStatusCheckEvent(player: Player, tripleData: VideoTripleData) {
        VideoTripleStatusCheckEvent(player, tripleData).call()
    }
    
    fun callUpFollowStatusCheckEvent(player: Player, followData: UpFollowData) {
        UpFollowStatusCheckEvent(player, followData).call()
    }
    
    fun callBilibiliLoginEvent(player: Player, session: LoginSession) {
        BilibiliLoginEvent(player, session).call()
    }
    
    fun callBilibiliLogoutEvent(player: Player, previousSession: LoginSession) {
        BilibiliLogoutEvent(player, previousSession).call()
    }
}