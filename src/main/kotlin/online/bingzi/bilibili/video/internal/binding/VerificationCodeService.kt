package online.bingzi.bilibili.video.internal.binding

import online.bingzi.bilibili.video.internal.cache.CacheConfig
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 验证码服务
 * 管理QQ自动绑定过程中的验证码生成、验证和过期处理
 */
object VerificationCodeService {
    
    // 验证码存储：验证码 -> 验证信息
    private val verificationCodes = ConcurrentHashMap<String, VerificationInfo>()
    
    // 玩家活跃验证映射：玩家UUID -> 验证码
    private val playerActiveVerifications = ConcurrentHashMap<UUID, String>()
    
    // QQ号的尝试记录：QQ号 -> 尝试信息
    private val qqAttemptRecords = ConcurrentHashMap<Long, QQAttemptRecord>()
    
    // 定时清理器
    private val cleanupScheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "VerificationCode-Cleanup").apply { isDaemon = true }
    }
    
    // 字母数字字符集（去除容易混淆的字符）
    private val alphanumericChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    
    /**
     * QQ号尝试记录
     */
    private data class QQAttemptRecord(
        var attemptCount: Int = 0,
        var lastAttemptTime: Long = System.currentTimeMillis(),
        var blockedUntil: Long = 0L
    ) {
        fun isBlocked(): Boolean = System.currentTimeMillis() < blockedUntil
        fun canAttempt(): Boolean = !isBlocked() && attemptCount < CacheConfig.maxVerificationAttempts * 2 // QQ号的尝试次数是验证码的2倍
        
        fun recordAttempt() {
            attemptCount++
            lastAttemptTime = System.currentTimeMillis()
            
            // 如果尝试次数过多，设置阻断时间
            if (attemptCount >= CacheConfig.maxVerificationAttempts) {
                // 第一次超限阻断5分钟，之后每次翻倍，最多阻断1小时
                val blockMinutes = minOf(5 * (1 shl (attemptCount - CacheConfig.maxVerificationAttempts)), 60)
                blockedUntil = System.currentTimeMillis() + (blockMinutes * 60 * 1000L)
            }
        }
        
        fun reset() {
            attemptCount = 0
            lastAttemptTime = System.currentTimeMillis()
            blockedUntil = 0L
        }
    }
    
    /**
     * 验证信息数据类
     */
    data class VerificationInfo(
        val playerUuid: UUID,
        val playerName: String,
        val createTime: Long,
        val expireTime: Long,
        var attempts: Int = 0
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() > expireTime
        fun canAttempt(): Boolean = attempts < CacheConfig.maxVerificationAttempts
        fun incrementAttempts() { attempts++ }
    }
    
    init {
        // 启动定时清理任务，每分钟清理一次过期验证码
        cleanupScheduler.scheduleAtFixedRate(
            { cleanupExpiredCodes() }, 
            1, 1, TimeUnit.MINUTES
        )
        
        console().sendInfo("verificationCodeServiceInitialized")
    }
    
    /**
     * 为玩家生成验证码
     * @param playerUuid 玩家UUID
     * @param playerName 玩家名称
     * @return 生成的验证码，如果玩家已有活跃验证码则返回null
     */
    fun generateCode(playerUuid: UUID, playerName: String): String? {
        // 检查玩家是否已有活跃验证码
        val existingCode = playerActiveVerifications[playerUuid]
        if (existingCode != null) {
            val existingInfo = verificationCodes[existingCode]
            if (existingInfo != null && !existingInfo.isExpired()) {
                console().sendWarn("verificationCodeAlreadyActive", playerName, existingCode)
                return null
            } else {
                // 清理过期的验证码
                cleanupPlayerVerification(playerUuid)
            }
        }
        
        // 生成新的验证码
        val code = generateUniqueCode()
        val expireTime = System.currentTimeMillis() + (CacheConfig.verificationExpireMinutes * 60 * 1000L)
        
        val verificationInfo = VerificationInfo(
            playerUuid = playerUuid,
            playerName = playerName,
            createTime = System.currentTimeMillis(),
            expireTime = expireTime
        )
        
        // 存储验证码信息
        verificationCodes[code] = verificationInfo
        playerActiveVerifications[playerUuid] = code
        
        console().sendInfo("verificationCodeGenerated", playerName, code, CacheConfig.verificationExpireMinutes.toString())
        return code
    }
    
    /**
     * 验证验证码（包含防暴力破解机制）
     * @param code 验证码
     * @param qqNumber QQ号
     * @return 验证结果，包含是否成功和相关信息
     */
    fun verifyCode(code: String, qqNumber: Long): VerificationResult {
        // 检查QQ号是否被阻断
        val qqRecord = qqAttemptRecords.getOrPut(qqNumber) { QQAttemptRecord() }
        
        if (qqRecord.isBlocked()) {
            val remainingBlockTime = (qqRecord.blockedUntil - System.currentTimeMillis()) / 1000 / 60
            console().sendWarn("verificationCodeQQBlocked", qqNumber.toString(), remainingBlockTime.toString())
            return VerificationResult.QQBlocked(remainingBlockTime.toInt())
        }
        
        if (!qqRecord.canAttempt()) {
            console().sendWarn("verificationCodeQQTooManyAttempts", qqNumber.toString(), qqRecord.attemptCount.toString())
            return VerificationResult.QQTooManyAttempts
        }
        
        val verificationInfo = verificationCodes[code]
            ?: run {
                // 记录失败尝试
                qqRecord.recordAttempt()
                console().sendWarn("verificationCodeNotFoundWithQQ", code, qqNumber.toString())
                return VerificationResult.CodeNotFound
            }
        
        // 检查验证码是否过期
        if (verificationInfo.isExpired()) {
            qqRecord.recordAttempt()
            cleanupCode(code)
            console().sendWarn("verificationCodeExpiredWithQQ", code, qqNumber.toString())
            return VerificationResult.Expired
        }
        
        // 检查验证码尝试次数
        if (!verificationInfo.canAttempt()) {
            qqRecord.recordAttempt()
            cleanupCode(code)
            console().sendWarn("verificationCodeTooManyAttemptsWithQQ", code, qqNumber.toString())
            return VerificationResult.TooManyAttempts
        }
        
        // 增加尝试次数
        verificationInfo.incrementAttempts()
        
        console().sendInfo("verificationCodeAttempt", verificationInfo.playerName, code, qqNumber.toString(), verificationInfo.attempts.toString())
        
        // 验证成功
        val result = VerificationResult.Success(verificationInfo.playerUuid, verificationInfo.playerName, qqNumber)
        
        // 清理验证码和重置QQ记录
        cleanupCode(code)
        qqRecord.reset()
        
        console().sendInfo("verificationCodeSuccess", verificationInfo.playerName, code, qqNumber.toString())
        return result
    }
    
    /**
     * 取消玩家的验证码
     * @param playerUuid 玩家UUID
     * @return 是否成功取消
     */
    fun cancelVerification(playerUuid: UUID): Boolean {
        val code = playerActiveVerifications[playerUuid] ?: return false
        cleanupCode(code)
        console().sendInfo("verificationCodeCancelled", playerUuid.toString())
        return true
    }
    
    /**
     * 获取玩家当前的验证码信息
     * @param playerUuid 玩家UUID
     * @return 验证信息，如果没有活跃验证码则返回null
     */
    fun getPlayerVerification(playerUuid: UUID): VerificationInfo? {
        val code = playerActiveVerifications[playerUuid] ?: return null
        val info = verificationCodes[code] ?: return null
        
        if (info.isExpired()) {
            cleanupCode(code)
            return null
        }
        
        return info
    }
    
    /**
     * 获取所有活跃验证码的统计信息
     */
    fun getStats(): VerificationStats {
        cleanupExpiredCodes() // 先清理过期验证码
        
        return VerificationStats(
            activeVerifications = verificationCodes.size,
            totalPlayersWithVerifications = playerActiveVerifications.size
        )
    }
    
    /**
     * 生成唯一验证码（字母数字混合格式）
     */
    private fun generateUniqueCode(): String {
        val codeLength = CacheConfig.verificationCodeLength
        var code: String
        
        do {
            // 生成字母数字混合验证码
            code = (1..codeLength).map { 
                alphanumericChars[Random.nextInt(alphanumericChars.length)]
            }.joinToString("")
        } while (verificationCodes.containsKey(code))
        
        return code
    }
    
    /**
     * 清理指定验证码
     */
    private fun cleanupCode(code: String) {
        val info = verificationCodes.remove(code)
        if (info != null) {
            playerActiveVerifications.remove(info.playerUuid)
        }
    }
    
    /**
     * 清理指定玩家的验证码
     */
    private fun cleanupPlayerVerification(playerUuid: UUID) {
        val code = playerActiveVerifications.remove(playerUuid)
        if (code != null) {
            verificationCodes.remove(code)
        }
    }
    
    /**
     * 清理所有过期验证码和过期的QQ阻断记录
     */
    private fun cleanupExpiredCodes() {
        val now = System.currentTimeMillis()
        val expiredCodes = mutableListOf<String>()
        val expiredQQBlocks = mutableListOf<Long>()
        
        // 清理过期验证码
        verificationCodes.forEach { (code, info) ->
            if (info.expireTime <= now) {
                expiredCodes.add(code)
            }
        }
        
        expiredCodes.forEach { code ->
            cleanupCode(code)
        }
        
        // 清理过期的QQ阻断记录
        qqAttemptRecords.forEach { (qq, record) ->
            if (!record.isBlocked() && (now - record.lastAttemptTime) > (24 * 60 * 60 * 1000L)) { // 24小时未活动则清理
                expiredQQBlocks.add(qq)
            }
        }
        
        expiredQQBlocks.forEach { qq ->
            qqAttemptRecords.remove(qq)
        }
        
        if (expiredCodes.isNotEmpty()) {
            console().sendInfo("verificationCodeCleanupCompleted", expiredCodes.size.toString())
        }
        
        if (expiredQQBlocks.isNotEmpty()) {
            console().sendInfo("verificationCodeQQRecordsCleanup", expiredQQBlocks.size.toString())
        }
    }
    
    /**
     * 关闭验证码服务
     */
    fun shutdown() {
        cleanupScheduler.shutdown()
        verificationCodes.clear()
        playerActiveVerifications.clear()
        qqAttemptRecords.clear()
        console().sendInfo("verificationCodeServiceShutdown")
    }
    
    /**
     * 验证结果密封类
     */
    sealed class VerificationResult {
        object CodeNotFound : VerificationResult()
        object Expired : VerificationResult()
        object TooManyAttempts : VerificationResult()
        object QQTooManyAttempts : VerificationResult()
        data class QQBlocked(val remainingMinutes: Int) : VerificationResult()
        data class Success(val playerUuid: UUID, val playerName: String, val qqNumber: Long) : VerificationResult()
    }
    
    /**
     * 验证统计信息
     */
    data class VerificationStats(
        val activeVerifications: Int,
        val totalPlayersWithVerifications: Int
    )
}