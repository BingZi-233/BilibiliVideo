package online.bingzi.bilibili.bilibilivideo.internal.session

import java.util.UUID

data class LoginSession(
    val playerUuid: UUID,
    val playerName: String,
    val mid: Long,
    val nickname: String,
    val sessdata: String,
    val buvid3: String,
    val biliJct: String,
    val refreshToken: String,
    val loginTime: Long,
    var lastActiveTime: Long
) {
    companion object {
        const val SESSION_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000L // 24小时
    }
    
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - lastActiveTime > SESSION_TIMEOUT_MILLIS
    }
    
    fun updateActiveTime() {
        lastActiveTime = System.currentTimeMillis()
    }
    
    fun isValid(): Boolean {
        return sessdata.isNotBlank() && buvid3.isNotBlank() && !isExpired()
    }
}