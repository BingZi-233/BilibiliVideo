package online.bingzi.bilibili.video.internal.cache

import com.github.benmanes.caffeine.cache.Caffeine
import online.bingzi.bilibili.video.internal.config.OneBotConfig
import online.bingzi.bilibili.video.internal.entity.VerificationCode
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 验证码缓存
 * 
 * 管理QQ绑定验证码的生成、存储和验证
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object VerificationCodeCache {
    
    /**
     * 验证码缓存
     * key: 验证码
     * value: 验证码数据
     */
    private val codeCache = Caffeine.newBuilder()
        .expireAfterWrite(OneBotConfig.codeExpire.toLong(), TimeUnit.SECONDS)
        .build<String, VerificationCode>()
    
    /**
     * 玩家验证码缓存
     * key: 玩家UUID
     * value: 验证码
     */
    private val playerCodeCache = Caffeine.newBuilder()
        .expireAfterWrite(OneBotConfig.codeExpire.toLong(), TimeUnit.SECONDS)
        .build<UUID, String>()
    
    /**
     * 冷却时间缓存
     * key: 玩家UUID
     * value: 下次可申请时间
     */
    private val cooldownCache = Caffeine.newBuilder()
        .expireAfterWrite(OneBotConfig.bindCooldown.toLong(), TimeUnit.SECONDS)
        .build<UUID, LocalDateTime>()
    
    /**
     * 生成验证码
     * 
     * @param playerUuid 玩家UUID
     * @param playerName 玩家名称
     * @return 验证码，如果在冷却中则返回null
     */
    fun generateCode(playerUuid: UUID, playerName: String): String? {
        // 检查冷却时间
        val cooldownTime = cooldownCache.getIfPresent(playerUuid)
        if (cooldownTime != null && LocalDateTime.now().isBefore(cooldownTime)) {
            return null
        }
        
        // 移除旧的验证码
        playerCodeCache.getIfPresent(playerUuid)?.let { oldCode ->
            codeCache.invalidate(oldCode)
        }
        
        // 生成新验证码
        val code = generateRandomCode()
        val expireTime = LocalDateTime.now().plusSeconds(OneBotConfig.codeExpire.toLong())
        
        val verificationCode = VerificationCode(
            code = code,
            playerUuid = playerUuid,
            playerName = playerName,
            expireTime = expireTime
        )
        
        // 存储验证码
        codeCache.put(code, verificationCode)
        playerCodeCache.put(playerUuid, code)
        
        // 设置冷却时间
        cooldownCache.put(playerUuid, LocalDateTime.now().plusSeconds(OneBotConfig.bindCooldown.toLong()))
        
        return code
    }
    
    /**
     * 验证并获取验证码数据
     * 
     * @param code 验证码
     * @return 验证码数据，如果无效则返回null
     */
    fun verifyAndGet(code: String): VerificationCode? {
        val verificationCode = codeCache.getIfPresent(code)
        
        if (verificationCode == null || !verificationCode.isValid()) {
            return null
        }
        
        // 标记为已使用
        verificationCode.used = true
        
        // 从缓存中移除
        codeCache.invalidate(code)
        playerCodeCache.invalidate(verificationCode.playerUuid)
        
        return verificationCode
    }
    
    /**
     * 获取玩家的验证码
     * 
     * @param playerUuid 玩家UUID
     * @return 验证码，如果不存在则返回null
     */
    fun getPlayerCode(playerUuid: UUID): String? {
        return playerCodeCache.getIfPresent(playerUuid)
    }
    
    /**
     * 检查玩家是否在冷却中
     * 
     * @param playerUuid 玩家UUID
     * @return 是否在冷却中
     */
    fun isInCooldown(playerUuid: UUID): Boolean {
        val cooldownTime = cooldownCache.getIfPresent(playerUuid)
        return cooldownTime != null && LocalDateTime.now().isBefore(cooldownTime)
    }
    
    /**
     * 获取剩余冷却时间（秒）
     * 
     * @param playerUuid 玩家UUID
     * @return 剩余冷却时间，如果不在冷却中则返回0
     */
    fun getCooldownRemaining(playerUuid: UUID): Long {
        val cooldownTime = cooldownCache.getIfPresent(playerUuid)
        if (cooldownTime == null || LocalDateTime.now().isAfter(cooldownTime)) {
            return 0
        }
        
        return java.time.Duration.between(LocalDateTime.now(), cooldownTime).seconds
    }
    
    /**
     * 清理过期的验证码
     */
    fun cleanupExpired() {
        codeCache.cleanUp()
        playerCodeCache.cleanUp()
        cooldownCache.cleanUp()
    }
    
    /**
     * 清空所有缓存
     */
    fun clearAll() {
        codeCache.invalidateAll()
        playerCodeCache.invalidateAll()
        cooldownCache.invalidateAll()
    }
    
    /**
     * 生成随机验证码
     * 
     * @return 随机验证码
     */
    private fun generateRandomCode(): String {
        val length = OneBotConfig.codeLength
        val chars = "0123456789"
        
        return buildString {
            repeat(length) {
                append(chars[Random.nextInt(chars.length)])
            }
        }
    }
}