package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.binding.AutoBindingService
import online.bingzi.bilibili.video.internal.database.PlayerQQBindingService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*

/**
 * QQ绑定子命令
 * 处理玩家QQ号绑定相关的操作
 */
object QQBindSubCommand {

    /**
     * QQ绑定命令主体
     */
    @CommandBody(permission = "bilibilivideo.command.qqbind")
    val qqbind = subCommand {
        // 显示帮助信息
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("qqBindCommandHelp")
        }
        
        // 绑定QQ号
        literal("bind") {
            dynamic("qqNumber") {
                suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                    listOf("请输入QQ号码")
                }
                
                restrict<ProxyCommandSender> { _, _, argument ->
                    // 验证QQ号码格式
                    isValidQQNumber(argument)
                }
                
                execute<ProxyCommandSender> { sender, _, argument ->
                    // 检查是否为玩家
                    if (sender !is ProxyPlayer) {
                        sender.sendError("qqBindOnlyPlayer")
                        return@execute
                    }
                    
                    val qqNumber = argument
                    val playerUuid = sender.uniqueId
                    
                    // 检查玩家是否已经绑定了QQ
                    QQBindingDaoService.getQQBindingByPlayer(playerUuid).thenAccept { existingBinding ->
                        if (existingBinding != null && existingBinding.isValidBinding()) {
                            sender.sendWarn("qqBindAlreadyBound", existingBinding.qqNumber)
                            return@thenAccept
                        }
                        
                        // 检查QQ号是否已被其他人绑定
                        QQBindingDaoService.getQQBindingByNumber(qqNumber).thenAccept { qqBinding ->
                            if (qqBinding != null && qqBinding.isValidBinding()) {
                                sender.sendError("qqBindNumberAlreadyUsed", qqNumber)
                                return@thenAccept
                            }
                            
                            // 创建新的绑定
                            val newBinding = QQBinding(
                                playerUuid = playerUuid.toString(),
                                qqNumber = qqNumber
                            )
                            
                            // 保存绑定
                            QQBindingDaoService.saveQQBinding(newBinding).thenAccept { success ->
                                if (success) {
                                    sender.sendInfo("qqBindSuccess", qqNumber)
                                } else {
                                    sender.sendError("qqBindFailed", qqNumber)
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 解绑QQ号
        literal("unbind") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("qqBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 删除绑定
                PlayerQQBindingService.unbindPlayerQQ(playerUuid).thenAccept { success ->
                    if (success) {
                        sender.sendInfo("qqUnbindSuccess")
                    } else {
                        sender.sendWarn("qqUnbindNoBinding")
                    }
                }
            }
        }
        
        // 查看绑定信息
        literal("info") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("qqBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 获取绑定信息
                PlayerQQBindingService.getPlayerQQBinding(playerUuid).thenAccept { binding ->
                    if (binding != null && binding.isValidBinding()) {
                        sender.sendInfo("qqBindInfo", binding.qqNumber, binding.bindTime)
                        if (binding.qqNickname != null) {
                            sender.sendInfo("qqBindInfoNickname", binding.qqNickname!!)
                        }
                    } else {
                        sender.sendWarn("qqBindNoBinding")
                    }
                }
            }
        }
        
        // 自动绑定
        literal("auto") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("qqBindOnlyPlayer")
                    return@execute
                }
                
                // 启动自动绑定流程
                AutoBindingService.startVerification(sender).thenAccept { verificationCode ->
                    if (verificationCode != null) {
                        // 验证码已生成并发送指引消息
                        console().sendInfo("qqAutoBindStartedForPlayer", sender.name, verificationCode)
                    }
                }.exceptionally { throwable ->
                    sender.sendError("qqAutoBindStartFailed", throwable.message ?: "Unknown error")
                    null
                }
            }
        }
        
        // 取消自动绑定
        literal("cancel") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("qqBindOnlyPlayer")
                    return@execute
                }
                
                AutoBindingService.cancelVerification(sender)
            }
        }
        
        // 查看验证状态
        literal("status") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("qqBindOnlyPlayer")
                    return@execute
                }
                
                val verification = AutoBindingService.getVerificationStatus(sender)
                if (verification != null) {
                    val remainingTime = (verification.expireTime - System.currentTimeMillis()) / 1000 / 60
                    sender.sendInfo("qqAutoBindStatus", 
                        verification.createTime.toString(),
                        remainingTime.toString(),
                        verification.attempts.toString())
                } else {
                    sender.sendInfo("qqAutoBindNoActiveVerification")
                }
            }
        }
        
        // 管理员解绑命令
        literal("admin-unbind", permission = "bilibilivideo.admin.qqbind") {
            dynamic("target") {
                suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                    listOf("QQ号码")
                }
                
                execute<ProxyCommandSender> { sender, _, argument ->
                    // 尝试作为QQ号处理
                    if (isValidQQNumber(argument)) {
                        QQBindingDaoService.getQQBindingByNumber(argument).thenAccept { binding ->
                            if (binding != null) {
                                // 删除绑定
                                QQBindingDaoService.deleteQQBinding(UUID.fromString(binding.playerUuid)).thenAccept { success ->
                                    if (success) {
                                        sender.sendInfo("qqAdminUnbindSuccess", argument)
                                    } else {
                                        sender.sendError("qqAdminUnbindFailed", argument)
                                    }
                                }
                            } else {
                                sender.sendWarn("qqAdminUnbindNotFound", argument)
                            }
                        }
                    } else {
                        sender.sendError("qqAdminUnbindPlayerNotSupported")
                    }
                }
            }
        }
    }

    /**
     * 验证QQ号码格式
     */
    private fun isValidQQNumber(qqNumber: String): Boolean {
        // QQ号码规则：5-11位数字，不以0开头
        return qqNumber.matches(Regex("^[1-9]\\d{4,10}$"))
    }
}