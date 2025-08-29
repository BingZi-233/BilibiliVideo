package online.bingzi.bilibili.bilibilivideo.internal.session

import online.bingzi.bilibili.bilibilivideo.internal.database.service.DatabaseService
import org.bukkit.entity.Player
import taboolib.common.platform.function.submitAsync
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object SessionManager {
    
    private val sessions = ConcurrentHashMap<UUID, LoginSession>()
    
    fun getSession(player: Player): LoginSession? {
        val session = sessions[player.uniqueId]
        
        if (session != null) {
            if (session.isExpired()) {
                sessions.remove(player.uniqueId)
                return null
            }
            session.updateActiveTime()
            return session
        }
        
        return null
    }
    
    fun createSession(
        player: Player,
        mid: Long,
        nickname: String,
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String
    ): LoginSession {
        val session = LoginSession(
            playerUuid = player.uniqueId,
            playerName = player.name,
            mid = mid,
            nickname = nickname,
            sessdata = sessdata,
            buvid3 = buvid3,
            biliJct = biliJct,
            refreshToken = refreshToken,
            loginTime = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis()
        )
        
        sessions[player.uniqueId] = session
        return session
    }
    
    fun removeSession(player: Player) {
        sessions.remove(player.uniqueId)
    }
    
    fun isPlayerLoggedIn(player: Player): Boolean {
        return getSession(player) != null
    }
    
    fun getPlayerMid(player: Player): Long? {
        return getSession(player)?.mid
    }
    
    fun loadSessionFromDatabase(player: Player, callback: (LoginSession?) -> Unit) {
        submitAsync {
            DatabaseService.getPlayerMid(player.uniqueId.toString()) { mid ->
                if (mid != null) {
                    DatabaseService.getBilibiliAccount(mid) { account ->
                        if (account != null) {
                            val session = LoginSession(
                                playerUuid = player.uniqueId,
                                playerName = player.name,
                                mid = account.mid,
                                nickname = account.nickname,
                                sessdata = account.sessdata,
                                buvid3 = account.buvid3,
                                biliJct = account.biliJct,
                                refreshToken = account.refreshToken,
                                loginTime = account.createTime,
                                lastActiveTime = System.currentTimeMillis()
                            )
                            
                            sessions[player.uniqueId] = session
                            callback(session)
                        } else {
                            callback(null)
                        }
                    }
                } else {
                    callback(null)
                }
            }
        }
    }
    
    fun clearExpiredSessions() {
        val expiredUuids = sessions.entries
            .filter { it.value.isExpired() }
            .map { it.key }
        
        expiredUuids.forEach { uuid ->
            sessions.remove(uuid)
        }
    }
    
    fun getActiveSessionCount(): Int {
        clearExpiredSessions()
        return sessions.size
    }
}