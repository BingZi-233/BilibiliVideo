package online.bingzi.bilibili.bilibilivideo.internal.session

import java.util.UUID

/**
 * Bilibili登录会话实体类
 * 
 * 表示玩家的Bilibili登录会话信息，包含认证凭据和会话状态。
 * 会话具有24小时的有效期，支持活跃时间更新和有效性检查。
 * 用于在玩家游戏过程中维持Bilibili API的访问权限。
 * 
 * @property playerUuid Minecraft玩家UUID
 * @property playerName Minecraft玩家名称
 * @property mid Bilibili用户MID
 * @property nickname Bilibili用户昵称
 * @property sessdata SESSDATA Cookie值
 * @property buvid3 buvid3 Cookie值，设备标识
 * @property biliJct bili_jct Cookie值，CSRF令牌
 * @property refreshToken 刷新令牌，用于Cookie更新
 * @property loginTime 登录时间戳（毫秒）
 * @property lastActiveTime 最后活跃时间戳（毫秒），可变属性
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
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
        /** 会话超时时间：24小时（毫秒） */
        const val SESSION_TIMEOUT_MILLIS = 24 * 60 * 60 * 1000L
    }
    
    /**
     * 检查会话是否已过期
     * 
     * 根据最后活跃时间和超时配置判断会话是否有效。
     * 
     * @return true 如果会话已过期，false 否则
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() - lastActiveTime > SESSION_TIMEOUT_MILLIS
    }
    
    /**
     * 更新会话活跃时间
     * 
     * 将最后活跃时间设置为当前时间戳，延长会话有效期。
     * 通常在用户进行API操作时调用。
     */
    fun updateActiveTime() {
        lastActiveTime = System.currentTimeMillis()
    }
    
    /**
     * 检查会话是否有效
     * 
     * 综合检查Cookie内容和会话过期状态。
     * 要求sessdata和buvid3不为空白，且会话未过期。
     * 
     * @return true 如果会话有效且可用于API调用，false 否则
     */
    fun isValid(): Boolean {
        return sessdata.isNotBlank() && buvid3.isNotBlank() && !isExpired()
    }
}