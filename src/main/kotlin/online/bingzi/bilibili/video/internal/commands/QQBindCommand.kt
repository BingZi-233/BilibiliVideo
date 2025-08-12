package online.bingzi.bilibili.video.internal.commands

import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*

/**
 * QQ绑定命令
 * 用于管理玩家的QQ号码绑定
 */
@CommandHeader(
    name = "qqbind",
    aliases = ["qq", "bindqq"],
    description = "QQ号码绑定管理",
    permission = "bilibilivideo.command.qqbind"
)
object QQBindCommand {

    @CommandBody
    val main = mainCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("qqBindCommandHelp")
        }
    }

    /**
     * 绑定QQ号码
     */
    @CommandBody
    val bind = subCommand {
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

    /**
     * 解绑QQ号码
     */
    @CommandBody
    val unbind = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("qqBindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            // 删除绑定
            QQBindingDaoService.deleteQQBinding(playerUuid).thenAccept { success ->
                if (success) {
                    sender.sendInfo("qqUnbindSuccess")
                } else {
                    sender.sendWarn("qqUnbindNoBinding")
                }
            }
        }
    }

    /**
     * 查看绑定信息
     */
    @CommandBody
    val info = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("qqBindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            // 获取绑定信息
            QQBindingDaoService.getQQBindingByPlayer(playerUuid).thenAccept { binding ->
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

    /**
     * 管理员解绑命令
     */
    @CommandBody(aliases = ["admin-unbind", "adminunbind"], permission = "bilibilivideo.admin.qqbind")
    val adminUnbind = subCommand {
        dynamic("target") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                listOf("玩家名称或QQ号码")
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
                    // 作为玩家名称处理
                    sender.sendError("qqAdminUnbindPlayerNotSupported")
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