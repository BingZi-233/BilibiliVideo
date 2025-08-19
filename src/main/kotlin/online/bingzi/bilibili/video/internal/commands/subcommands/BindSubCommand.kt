package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.cache.CommandSuggestionService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliCookieDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.BilibiliBinding
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import online.bingzi.bilibili.video.internal.network.BilibiliNetworkManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*

/**
 * 账户绑定相关子命令
 * 处理 Bilibili 和 QQ 账户绑定功能
 */
object BindSubCommand {
    
    /**
     * 绑定主命令 - /bilibilivideo bind
     */
    val main = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("bindCommandHelp")
        }
    }
    
    /**
     * Bilibili 绑定相关命令
     */
    
    /**
     * 绑定 Bilibili 账户 - /bilibilivideo bind bilibili
     */
    val bilibili = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("bindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * 解绑 Bilibili 账户 - /bilibilivideo bind bilibili unbind
     */
    val bilibiliUnbind = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("bindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * 查看 Bilibili 绑定信息 - /bilibilivideo bind bilibili info
     */
    val bilibiliInfo = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("bindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * 刷新 Bilibili 绑定信息 - /bilibilivideo bind bilibili refresh
     */
    val bilibiliRefresh = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("bindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * QQ 绑定相关命令
     */
    
    /**
     * 绑定 QQ 号码 - /bilibilivideo bind qq <number>
     */
    val qq = subCommand {
        dynamic("qqNumber") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取最近绑定的QQ号码作为建议
                try {
                    CommandSuggestionService.getQQBindingSuggestions(10).get()
                } catch (e: Exception) {
                    // 降级处理：提供格式示例
                    listOf("1234567890", "9876543210", "请输入QQ号码")
                }
            }
            
            restrict<ProxyCommandSender> { _, _, argument ->
                // 验证QQ号码格式
                isValidQQNumber(argument)
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 检查是否为玩家
                if (sender !is ProxyPlayer) {
                    sender.sendError("bindOnlyPlayer")
                    return@execute
                }
                
                val qqNumber = argument
                val playerUuid = sender.uniqueId
                
                submit(async = true) {
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
    }
    
    /**
     * 解绑 QQ 号码 - /bilibilivideo bind qq unbind
     */
    val qqUnbind = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("qqBindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * 查看 QQ 绑定信息 - /bilibilivideo bind qq info
     */
    val qqInfo = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            // 检查是否为玩家
            if (sender !is ProxyPlayer) {
                sender.sendError("qqBindOnlyPlayer")
                return@execute
            }
            
            val playerUuid = sender.uniqueId
            
            submit(async = true) {
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
    }
    
    /**
     * 管理员命令 - 解绑指定账户
     */
    val adminUnbind = subCommand {
        dynamic("type") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                listOf("bilibili", "qq")
            }
            
            dynamic("target") {
                suggestion<ProxyCommandSender>(uncheck = true) { sender, context ->
                    val type = context["type"]
                    when (type) {
                        "bilibili" -> try {
                            CommandSuggestionService.getBilibiliUidSuggestions(10).get()
                        } catch (e: Exception) {
                            listOf("Bilibili UID")
                        }
                        "qq" -> try {
                            CommandSuggestionService.getActiveQQBindingSuggestions(10).get()
                        } catch (e: Exception) {
                            listOf("QQ号码")
                        }
                        else -> listOf("请先选择类型")
                    }
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val type = context["type"]
                    val target = argument
                    
                    submit(async = true) {
                        when (type) {
                            "bilibili" -> {
                                // 尝试作为Bilibili UID处理
                                val uid = target.toLongOrNull()
                                if (uid != null) {
                                    BilibiliBindingDaoService.getBilibiliBindingByUid(uid).thenAccept { binding ->
                                        if (binding != null) {
                                            // 删除绑定
                                            BilibiliBindingDaoService.deleteBilibiliBinding(UUID.fromString(binding.playerUuid)).thenAccept { success ->
                                                if (success) {
                                                    sender.sendInfo("bilibiliAdminUnbindSuccess", target)
                                                } else {
                                                    sender.sendError("bilibiliAdminUnbindFailed", target)
                                                }
                                            }
                                        } else {
                                            sender.sendWarn("bilibiliAdminUnbindNotFound", target)
                                        }
                                    }
                                } else {
                                    sender.sendError("bilibiliAdminUnbindPlayerNotSupported")
                                }
                            }
                            "qq" -> {
                                // 尝试作为QQ号处理
                                if (isValidQQNumber(target)) {
                                    QQBindingDaoService.getQQBindingByNumber(target).thenAccept { binding ->
                                        if (binding != null) {
                                            // 删除绑定
                                            QQBindingDaoService.deleteQQBinding(UUID.fromString(binding.playerUuid)).thenAccept { success ->
                                                if (success) {
                                                    sender.sendInfo("qqAdminUnbindSuccess", target)
                                                } else {
                                                    sender.sendError("qqAdminUnbindFailed", target)
                                                }
                                            }
                                        } else {
                                            sender.sendWarn("qqAdminUnbindNotFound", target)
                                        }
                                    }
                                } else {
                                    sender.sendError("qqAdminUnbindPlayerNotSupported")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 管理员命令 - 查看所有绑定
     */
    val adminList = subCommand {
        dynamic("type", optional = true) {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                listOf("bilibili", "qq")
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val type = argument
                
                submit(async = true) {
                    when (type) {
                        "bilibili" -> {
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
                        "qq" -> {
                            // TODO: 实现 QQ 绑定列表查询
                            sender.sendInfo("qqAdminListNotImplemented")
                        }
                        else -> {
                            sender.sendError("bindAdminListInvalidType")
                        }
                    }
                }
            }
        }
        
        // 无参数时显示所有类型的绑定
        execute<ProxyCommandSender> { sender, _, _ ->
            submit(async = true) {
                BilibiliBindingDaoService.getAllActiveBilibiliBindings().thenAccept { bilibiliBindings ->
                    sender.sendInfo("bindAdminListHeader", "Bilibili", bilibiliBindings.size)
                    bilibiliBindings.take(10).forEach { binding ->
                        sender.sendInfo("bilibiliAdminListItem",
                            binding.playerUuid,
                            binding.bilibiliUsername ?: "未知",
                            binding.bilibiliUid.toString(),
                            if (binding.isExpired()) "已过期" else "正常")
                    }
                    
                    if (bilibiliBindings.size > 10) {
                        sender.sendInfo("bindAdminListMore", (bilibiliBindings.size - 10).toString())
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