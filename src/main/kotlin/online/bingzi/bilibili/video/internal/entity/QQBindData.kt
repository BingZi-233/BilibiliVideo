package online.bingzi.bilibili.video.internal.entity

import java.time.LocalDateTime
import java.util.UUID

/**
 * QQ绑定数据
 * 
 * 存储玩家与QQ的绑定关系
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
data class QQBindData(
    /**
     * 玩家UUID
     */
    val playerUuid: UUID,
    
    /**
     * 玩家名称
     */
    val playerName: String,
    
    /**
     * QQ号
     */
    val qqNumber: Long,
    
    /**
     * 绑定时间
     */
    val bindTime: LocalDateTime = LocalDateTime.now(),
    
    /**
     * 是否启用
     */
    val enabled: Boolean = true,
    
    /**
     * Bilibili MID（如果已绑定）
     */
    val bilibiliMid: String? = null,
    
    /**
     * 最后使用时间
     */
    val lastUsed: LocalDateTime? = null
)

/**
 * 验证码数据
 * 
 * 临时存储待验证的绑定请求
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
data class VerificationCode(
    /**
     * 验证码
     */
    val code: String,
    
    /**
     * 玩家UUID
     */
    val playerUuid: UUID,
    
    /**
     * 玩家名称
     */
    val playerName: String,
    
    /**
     * 创建时间
     */
    val createTime: LocalDateTime = LocalDateTime.now(),
    
    /**
     * 过期时间
     */
    val expireTime: LocalDateTime,
    
    /**
     * 是否已使用
     */
    var used: Boolean = false
) {
    /**
     * 检查验证码是否已过期
     */
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expireTime)
    }
    
    /**
     * 检查验证码是否有效
     */
    fun isValid(): Boolean {
        return !used && !isExpired()
    }
}

/**
 * QQ绑定结果
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
enum class QQBindResult {
    /**
     * 绑定成功
     */
    SUCCESS,
    
    /**
     * 验证码无效
     */
    CODE_INVALID,
    
    /**
     * 验证码已过期
     */
    CODE_EXPIRED,
    
    /**
     * QQ已绑定其他账号
     */
    ALREADY_BOUND,
    
    /**
     * 玩家不存在
     */
    PLAYER_NOT_FOUND,
    
    /**
     * 数据库错误
     */
    DATABASE_ERROR,
    
    /**
     * 未知错误
     */
    UNKNOWN_ERROR
}