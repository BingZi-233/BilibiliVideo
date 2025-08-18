package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliCookieDaoService
import online.bingzi.bilibili.video.internal.database.entity.BilibiliBinding
import online.bingzi.bilibili.video.internal.network.BilibiliNetworkManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*

/**
 * Bilibili绑定子命令
 * 处理玩家Bilibili账户绑定相关的操作
 */
object BilibiliBindSubCommand {

    /**
     * Bilibili绑定命令主体
     */
    @CommandBody(permission = "bilibilivideo.command.bilibind")
    val bilibind = subCommand {
        // 显示帮助信息
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("bilibiliBindCommandHelp")
        }
        
        // 绑定Bilibili账户
        literal("bind") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("bilibiliBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 检查玩家是否已经绑定了Bilibili账户
                BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).thenAccept { existingBinding ->
                    if (existingBinding != null && existingBinding.isValidBinding()) {
                        sender.sendWarn("bilibiliBindAlreadyBound", 
                            existingBinding.bilibiliUsername ?: existingBinding.bilibiliUid.toString())
                        return@thenAccept
                    }
                    
                    // 获取当前登录的Cookie
                    BilibiliCookieDaoService.getCookieByPlayer(playerUuid).thenAccept { cookie ->
                        if (cookie == null || !cookie.isValidCookie()) {
                            sender.sendError("bilibiliBindNeedLogin")
                            return@thenAccept
                        }
                        
                        // 获取用户信息
                        BilibiliNetworkManager.getPlayerService(playerUuid.toString()).getCurrentUserInfo().thenAccept { userInfo ->
                            if (userInfo == null) {
                                sender.sendError("bilibiliBindGetUserInfoFailed")
                                return@thenAccept
                            }
                            
                            val bilibiliUid = userInfo.uid
                            
                            // 检查该Bilibili账户是否已被其他人绑定
                            BilibiliBindingDaoService.getBilibiliBindingByUid(bilibiliUid).thenAccept { uidBinding ->
                                if (uidBinding != null && uidBinding.isValidBinding()) {
                                    sender.sendError("bilibiliBindUidAlreadyUsed", bilibiliUid.toString())
                                    return@thenAccept
                                }
                                
                                // 创建新的绑定
                                val newBinding = BilibiliBinding(
                                    playerUuid = playerUuid.toString(),
                                    bilibiliUid = bilibiliUid
                                ).apply {
                                    updateUserInfo(
                                        username = userInfo.username,
                                        nickname = userInfo.username,
                                        avatarUrl = userInfo.avatar,
                                        level = userInfo.level
                                    )
                                    updateLastLoginTime()
                                }
                                
                                // 保存绑定
                                BilibiliBindingDaoService.saveBilibiliBinding(newBinding).thenAccept { success ->
                                    if (success) {
                                        sender.sendInfo("bilibiliBindSuccess", userInfo.username, bilibiliUid.toString())
                                    } else {
                                        sender.sendError("bilibiliBindFailed", bilibiliUid.toString())
                                    }
                                }
                            }
                        }.exceptionally { throwable ->
                            sender.sendError("bilibiliBindGetUserInfoError", throwable.message ?: "Unknown error")
                            null
                        }
                    }
                }
            }
        }
        
        // 解绑Bilibili账户
        literal("unbind") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("bilibiliBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 删除绑定
                BilibiliBindingDaoService.deleteBilibiliBinding(playerUuid).thenAccept { success ->
                    if (success) {
                        sender.sendInfo("bilibiliUnbindSuccess")
                    } else {
                        sender.sendWarn("bilibiliUnbindNoBinding")
                    }
                }
            }
        }
        
        // 查看绑定信息
        literal("info") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("bilibiliBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 获取绑定信息
                BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).thenAccept { binding ->
                    if (binding != null && binding.isValidBinding()) {
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
        
        // 刷新绑定信息
        literal("refresh") {
            execute<ProxyCommandSender> { sender, _, _ ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("bilibiliBindOnlyPlayer")
                    return@execute
                }
                
                val playerUuid = sender.uniqueId
                
                // 获取绑定信息
                BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).thenAccept { binding ->
                    if (binding == null || !binding.isValidBinding()) {
                        sender.sendWarn("bilibiliBindNoBinding")
                        return@thenAccept
                    }
                    
                    // 获取Cookie
                    BilibiliCookieDaoService.getCookieByPlayer(playerUuid).thenAccept { cookie ->
                        if (cookie == null || !cookie.isValidCookie()) {
                            sender.sendError("bilibiliBindNeedLogin")
                            return@thenAccept
                        }
                        
                        // 获取最新用户信息
                        BilibiliNetworkManager.getPlayerService(playerUuid.toString()).getCurrentUserInfo().thenAccept { userInfo ->
                            if (userInfo == null) {
                                sender.sendError("bilibiliBindRefreshFailed")
                                return@thenAccept
                            }
                            
                            // 更新绑定信息
                            binding.updateUserInfo(
                                username = userInfo.username,
                                nickname = userInfo.username,
                                avatarUrl = userInfo.avatar,
                                level = userInfo.level
                            )
                            binding.updateLastLoginTime()
                            
                            // 保存更新
                            BilibiliBindingDaoService.saveBilibiliBinding(binding).thenAccept { success ->
                                if (success) {
                                    sender.sendInfo("bilibiliBindRefreshSuccess", userInfo.username)
                                } else {
                                    sender.sendError("bilibiliBindRefreshSaveFailed")
                                }
                            }
                        }.exceptionally { throwable ->
                            sender.sendError("bilibiliBindRefreshError", throwable.message ?: "Unknown error")
                            null
                        }
                    }
                }
            }
        }
        
        // 管理员解绑命令
        literal("admin-unbind", permission = "bilibilivideo.admin.bilibind") {
            dynamic("target") {
                suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                    listOf("Bilibili UID")
                }
                
                execute<ProxyCommandSender> { sender, _, argument ->
                    // 尝试作为Bilibili UID处理
                    val uid = argument.toLongOrNull()
                    if (uid != null) {
                        BilibiliBindingDaoService.getBilibiliBindingByUid(uid).thenAccept { binding ->
                            if (binding != null) {
                                // 删除绑定
                                BilibiliBindingDaoService.deleteBilibiliBinding(UUID.fromString(binding.playerUuid)).thenAccept { success ->
                                    if (success) {
                                        sender.sendInfo("bilibiliAdminUnbindSuccess", argument)
                                    } else {
                                        sender.sendError("bilibiliAdminUnbindFailed", argument)
                                    }
                                }
                            } else {
                                sender.sendWarn("bilibiliAdminUnbindNotFound", argument)
                            }
                        }
                    } else {
                        sender.sendError("bilibiliAdminUnbindPlayerNotSupported")
                    }
                }
            }
        }
        
        // 管理员查看所有绑定
        literal("admin-list", permission = "bilibilivideo.admin.bilibind") {
            execute<ProxyCommandSender> { sender, _, _ ->
                BilibiliBindingDaoService.getAllActiveBilibiliBindings().thenAccept { bindings ->
                    if (bindings.isEmpty()) {
                        sender.sendInfo("bilibiliAdminListEmpty")
                        return@thenAccept
                    }
                    
                    sender.sendInfo("bilibiliAdminListHeader", bindings.size)
                    bindings.forEach { binding ->
                        sender.sendInfo("bilibiliAdminListItem",
                            binding.playerUuid,
                            binding.bilibiliUsername ?: "未知",
                            binding.bilibiliUid.toString(),
                            if (binding.isExpired()) "已过期" else "正常")
                    }
                }
            }
        }
    }
}