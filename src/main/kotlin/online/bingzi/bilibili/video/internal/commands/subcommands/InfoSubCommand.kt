package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.cache.QQBindingCacheService
import online.bingzi.bilibili.video.internal.database.PlayerQQBindingService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 信息查看子命令
 * 提供统一的绑定信息查看功能
 */
object InfoSubCommand {

    /**
     * 信息查看命令主体
     */
    @CommandBody(permission = "bilibilivideo.command.info")
    val info = subCommand {
        // 显示帮助信息
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("infoCommandHelp")
        }
        
        // 查看所有绑定信息
        literal("all") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("infoOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 获取QQ绑定信息
                PlayerQQBindingService.getPlayerQQBinding(playerUuid).thenAccept { qqBinding ->
                    // 获取Bilibili绑定信息
                    BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).thenAccept { bilibiliBinding ->
                        sender.sendInfo("infoAllHeader")
                        
                        // QQ绑定信息
                        if (qqBinding?.isValidBinding() == true) {
                            sender.sendInfo("infoQQBound", qqBinding.qqNumber)
                            qqBinding.qqNickname?.let {
                                sender.sendInfo("infoQQNickname", it)
                            }
                        } else {
                            sender.sendInfo("infoQQNotBound")
                        }
                        
                        // Bilibili绑定信息
                        if (bilibiliBinding?.isValidBinding() == true) {
                            sender.sendInfo("infoBilibiliBound", 
                                bilibiliBinding.bilibiliUsername ?: "未知",
                                bilibiliBinding.bilibiliUid.toString())
                            
                            bilibiliBinding.userLevel?.let {
                                sender.sendInfo("infoBilibiliLevel", it)
                            }
                            
                            if (bilibiliBinding.isExpired()) {
                                sender.sendWarn("infoBilibiliExpired")
                            }
                        } else {
                            sender.sendInfo("infoBilibiliNotBound")
                        }
                        
                        // 绑定状态总结
                        val qqBound = qqBinding?.isValidBinding() == true
                        val bilibiliBound = bilibiliBinding?.isValidBinding() == true
                        
                        when {
                            qqBound && bilibiliBound -> sender.sendInfo("infoStatusComplete")
                            qqBound -> sender.sendInfo("infoStatusPartialQQ")
                            bilibiliBound -> sender.sendInfo("infoStatusPartialBilibili")
                            else -> sender.sendInfo("infoStatusNone")
                        }
                    }
                }
            }
        }
        
        // 查看QQ绑定信息
        literal("qq") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("infoOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                PlayerQQBindingService.getPlayerQQBinding(playerUuid).thenAccept { binding ->
                    if (binding?.isValidBinding() == true) {
                        sender.sendInfo("qqBindInfo", binding.qqNumber, binding.bindTime)
                        binding.qqNickname?.let {
                            sender.sendInfo("qqBindInfoNickname", it)
                        }
                    } else {
                        sender.sendWarn("qqBindNoBinding")
                    }
                }
            }
        }
        
        // 查看Bilibili绑定信息
        literal("bilibili") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender !is ProxyPlayer) {
                    sender.sendError("infoOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).thenAccept { binding ->
                    if (binding?.isValidBinding() == true) {
                        sender.sendInfo("bilibiliBindInfo", 
                            binding.bilibiliUsername ?: "未知",
                            binding.bilibiliUid.toString(),
                            binding.bindTime)
                        
                        binding.userLevel?.let {
                            sender.sendInfo("bilibiliBindInfoLevel", it)
                        }
                        
                        if (binding.isExpired()) {
                            sender.sendWarn("bilibiliBindInfoExpired")
                        }
                    } else {
                        sender.sendWarn("bilibiliBindNoBinding")
                    }
                }
            }
        }
        
        // 查看缓存统计（管理员功能）
        literal("cache", permission = "bilibilivideo.admin.cache") {
            execute<ProxyCommandSender> { sender, _, _ ->
                val stats = QQBindingCacheService.getCacheStats()
                
                sender.sendInfo("infoCacheHeader")
                sender.sendInfo("infoCacheEnabled", stats.enabled.toString())
                sender.sendInfo("infoCacheTotalBindings", stats.totalBindings.toString())
                sender.sendInfo("infoCacheHits", stats.cacheHits.toString())
                sender.sendInfo("infoCacheMisses", stats.cacheMisses.toString())
                sender.sendInfo("infoCacheHitRate", String.format("%.2f%%", stats.hitRate * 100))
                sender.sendInfo("infoCacheMaxSize", stats.maxSize.toString())
            }
        }
        
        // 刷新缓存（管理员功能）
        literal("refresh-cache", permission = "bilibilivideo.admin.cache") {
            execute<ProxyCommandSender> { sender, _, _ ->
                if (sender is ProxyPlayer) {
                    PlayerQQBindingService.refreshCache(sender.uniqueId).thenAccept { success ->
                        if (success) {
                            sender.sendInfo("infoCacheRefreshSuccess")
                        } else {
                            sender.sendError("infoCacheRefreshFailed")
                        }
                    }
                } else {
                    sender.sendError("infoRefreshCacheOnlyPlayer")
                }
            }
        }
    }
}