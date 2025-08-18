package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo

/**
 * 信息查看子命令
 * 提供统一的绑定信息查看功能
 */
object InfoSubCommand {

    /**
     * 信息查看命令
     * /bilibili info
     */
    @CommandBody
    val info = subCommand {
        literal("info")
        execute<ProxyPlayer> { player, _, _ ->
            player.sendInfo("infoCommandProcessing")
            
            val playerUuid = player.uniqueId.toString()
            
            // 异步获取绑定信息
            val qqBindingFuture = QQBindingDaoService.getQQBindingByPlayer(player.uniqueId)
            val bilibiliBindingFuture = BilibiliBindingDaoService.getBilibiliBindingByPlayer(player.uniqueId)
            
            // 等待所有结果
            try {
                val qqBinding = qqBindingFuture.get()
                val bilibiliBinding = bilibiliBindingFuture.get()
                
                player.sendInfo("infoHeader")
                
                if (qqBinding != null) {
                    player.sendInfo("infoQQBinding", qqBinding.qqNumber)
                } else {
                    player.sendInfo("infoQQNotBound")
                }
                
                if (bilibiliBinding != null) {
                    player.sendInfo("infoBilibiliBinding", bilibiliBinding.bilibiliUid)
                } else {
                    player.sendInfo("infoBilibiliNotBound")
                }
                
            } catch (e: Exception) {
                player.sendError("infoQueryError", e.message ?: "未知错误")
            }
        }
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendError("infoPlayerOnly")
        }
    }
}