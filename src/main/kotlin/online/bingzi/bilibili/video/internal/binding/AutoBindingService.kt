package online.bingzi.bilibili.video.internal.binding

import online.bingzi.bilibili.video.internal.database.PlayerQQBindingService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 自动绑定服务
 * 提供基于验证码的QQ自动绑定功能
 */
object AutoBindingService {
    
    /**
     * 启动玩家的验证码绑定流程
     * @param player 玩家
     * @return CompletableFuture<String?> 验证码，如果启动失败返回null
     */
    fun startVerification(player: ProxyPlayer): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            val playerUuid = player.uniqueId
            val playerName = player.name
            
            try {
                // 检查玩家是否已经绑定QQ
                val hasBinding = PlayerQQBindingService.hasValidQQBinding(playerUuid).get()
                if (hasBinding) {
                    player.sendWarn("qqAutoBindAlreadyBound")
                    return@supplyAsync null
                }
                
                // 检查玩家是否已有活跃验证码
                val existingVerification = VerificationCodeService.getPlayerVerification(playerUuid)
                if (existingVerification != null) {
                    val remainingTime = (existingVerification.expireTime - System.currentTimeMillis()) / 1000 / 60
                    player.sendWarn("qqAutoBindVerificationActive", remainingTime.toString())
                    return@supplyAsync null
                }
                
                // 生成验证码
                val verificationCode = VerificationCodeService.generateCode(playerUuid, playerName)
                if (verificationCode == null) {
                    player.sendError("qqAutoBindGenerateCodeFailed")
                    console().sendError("qqAutoBindGenerateCodeSystemError", playerName)
                    return@supplyAsync null
                }
                
                // 发送验证码指引消息
                player.sendInfo("qqAutoBindCodeGenerated", verificationCode)
                player.sendInfo("qqAutoBindInstructions")
                
                console().sendInfo("qqAutoBindStarted", playerName, verificationCode)
                verificationCode
                
            } catch (e: Exception) {
                val errorMessage = "启动验证流程时出错: ${e.message}"
                player.sendError("qqAutoBindStartFailed", errorMessage)
                console().sendError("qqAutoBindStartSystemError", playerName, e.message ?: "Unknown error", e.javaClass.simpleName)
                null
            }
        }.exceptionally { throwable ->
            val errorMessage = "验证流程异常: ${throwable.message}"
            player.sendError("qqAutoBindStartFailed", errorMessage)
            console().sendError("qqAutoBindStartException", player.name, throwable.message ?: "Unknown error", throwable.javaClass.simpleName)
            null
        }
    }
    
    /**
     * 完成绑定操作
     * @param verificationCode 验证码
     * @param qqNumber QQ号
     * @return CompletableFuture<Boolean> 是否成功完成绑定
     */
    fun completeBinding(verificationCode: String, qqNumber: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 验证验证码
                val verificationResult = VerificationCodeService.verifyCode(verificationCode, qqNumber)
                
                when (verificationResult) {
                    is VerificationCodeService.VerificationResult.Success -> {
                        val playerUuid = verificationResult.playerUuid
                        val playerName = verificationResult.playerName
                        
                        console().sendInfo("qqAutoBindVerificationSuccess", playerName, verificationCode, qqNumber.toString())
                        
                        try {
                            // 检查QQ号是否已被其他玩家绑定
                            val existingBinding = QQBindingDaoService.getQQBindingByNumber(qqNumber.toString()).get()
                            if (existingBinding?.isValidBinding() == true && existingBinding.playerUuid != playerUuid.toString()) {
                                console().sendWarn("qqAutoBindQQAlreadyBound", qqNumber.toString(), existingBinding.playerUuid)
                                QQVerificationHandler.notifyBindingFailure(qqNumber, "该QQ号已被其他玩家绑定")
                                return@supplyAsync false
                            }
                            
                            // 执行绑定
                            val bindingSuccess = PlayerQQBindingService.bindPlayerQQ(playerUuid, qqNumber).get()
                            
                            if (bindingSuccess) {
                                console().sendInfo("qqAutoBindCompleted", playerName, qqNumber.toString())
                                
                                // 通知QQ验证处理器绑定成功
                                QQVerificationHandler.notifyBindingSuccess(qqNumber, playerName)
                                
                                return@supplyAsync true
                            } else {
                                console().sendError("qqAutoBindSaveFailed", playerName, qqNumber.toString())
                                
                                // 通知QQ验证处理器绑定失败
                                QQVerificationHandler.notifyBindingFailure(qqNumber, "保存绑定信息失败，请稍后重试")
                                
                                return@supplyAsync false
                            }
                        } catch (bindingException: Exception) {
                            console().sendError("qqAutoBindBindingProcessError", playerName, qqNumber.toString(), 
                                bindingException.message ?: "Unknown error", bindingException.javaClass.simpleName)
                            QQVerificationHandler.notifyBindingFailure(qqNumber, "绑定过程出现异常: ${bindingException.message}")
                            return@supplyAsync false
                        }
                    }
                    
                    VerificationCodeService.VerificationResult.CodeNotFound -> {
                        console().sendWarn("qqAutoBindCodeNotFound", verificationCode)
                        QQVerificationHandler.notifyBindingFailure(qqNumber, "验证码不存在或已过期，请重新获取")
                        return@supplyAsync false
                    }
                    
                    VerificationCodeService.VerificationResult.Expired -> {
                        console().sendWarn("qqAutoBindCodeExpired", verificationCode)
                        QQVerificationHandler.notifyBindingFailure(qqNumber, "验证码已过期，请重新获取")
                        return@supplyAsync false
                    }
                    
                    VerificationCodeService.VerificationResult.TooManyAttempts -> {
                        console().sendWarn("qqAutoBindTooManyAttempts", verificationCode)
                        QQVerificationHandler.notifyBindingFailure(qqNumber, "尝试次数过多，请稍后重新获取验证码")
                        return@supplyAsync false
                    }
                    
                    VerificationCodeService.VerificationResult.QQTooManyAttempts -> {
                        console().sendWarn("qqAutoBindQQTooManyAttempts", qqNumber.toString())
                        QQVerificationHandler.notifyBindingFailure(qqNumber, "您的QQ号尝试次数过多，请稍后再试")
                        return@supplyAsync false
                    }
                    
                    is VerificationCodeService.VerificationResult.QQBlocked -> {
                        console().sendWarn("qqAutoBindQQBlocked", qqNumber.toString(), verificationResult.remainingMinutes.toString())
                        QQVerificationHandler.notifyBindingFailure(qqNumber, "您的QQ号已被暂时阻断 ${verificationResult.remainingMinutes} 分钟，请稍后再试")
                        return@supplyAsync false
                    }
                }
            } catch (e: Exception) {
                console().sendError("qqAutoBindCompleteSystemError", verificationCode, qqNumber.toString(), 
                    e.message ?: "Unknown error", e.javaClass.simpleName)
                QQVerificationHandler.notifyBindingFailure(qqNumber, "系统异常，请稍后重试: ${e.message}")
                return@supplyAsync false
            }
        }.exceptionally { throwable ->
            console().sendError("qqAutoBindCompleteException", verificationCode, qqNumber.toString(), 
                throwable.message ?: "Unknown error", throwable.javaClass.simpleName)
            QQVerificationHandler.notifyBindingFailure(qqNumber, "绑定过程异常: ${throwable.message}")
            false
        }
    }
    
    /**
     * 取消玩家的验证流程
     * @param player 玩家
     * @return 是否成功取消
     */
    fun cancelVerification(player: ProxyPlayer): Boolean {
        val success = VerificationCodeService.cancelVerification(player.uniqueId)
        
        if (success) {
            player.sendInfo("qqAutoBindCancelled")
            console().sendInfo("qqAutoBindCancelledByPlayer", player.name)
        } else {
            player.sendWarn("qqAutoBindNothingToCancel")
        }
        
        return success
    }
    
    /**
     * 获取玩家的验证状态
     * @param player 玩家
     * @return 验证信息，如果没有活跃验证则返回null
     */
    fun getVerificationStatus(player: ProxyPlayer): VerificationCodeService.VerificationInfo? {
        return VerificationCodeService.getPlayerVerification(player.uniqueId)
    }
    
    /**
     * 处理来自QQ的验证码消息
     * 这个方法由QQVerificationHandler调用
     * @param qqNumber QQ号
     * @param message 消息内容
     * @return 是否成功处理
     */
    fun handleQQVerificationMessage(qqNumber: Long, message: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            // 提取验证码（假设消息就是验证码）
            val verificationCode = message.trim()
            
            // 验证验证码格式
            if (!isValidVerificationCode(verificationCode)) {
                console().sendWarn("qqAutoBindInvalidCodeFormat", qqNumber.toString(), verificationCode)
                return@supplyAsync false
            }
            
            console().sendInfo("qqAutoBindReceivedCode", qqNumber.toString(), verificationCode)
            
            // 完成绑定
            completeBinding(verificationCode, qqNumber).get()
        }.exceptionally { throwable ->
            console().sendError("qqAutoBindHandleMessageError", qqNumber.toString(), message, throwable.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * 验证验证码格式是否有效（支持字母数字混合格式）
     */
    private fun isValidVerificationCode(code: String): Boolean {
        // 4-10位字母数字混合（允许纯数字以兼容旧格式）
        return code.matches(Regex("^[A-Z0-9]{4,10}$")) || code.matches(Regex("^\\d{4,10}$"))
    }
    
    /**
     * 获取自动绑定统计信息
     */
    fun getStats(): AutoBindingStats {
        val verificationStats = VerificationCodeService.getStats()
        
        return AutoBindingStats(
            activeVerifications = verificationStats.activeVerifications,
            totalPlayersWithVerifications = verificationStats.totalPlayersWithVerifications
        )
    }
    
    /**
     * 自动绑定统计信息
     */
    data class AutoBindingStats(
        val activeVerifications: Int,
        val totalPlayersWithVerifications: Int
    )
}