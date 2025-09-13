package online.bingzi.bilibili.bilibilivideo.internal.command

import online.bingzi.bilibili.bilibilivideo.internal.command.handler.FollowStatusCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LoginCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.LogoutCommandHandler
import online.bingzi.bilibili.bilibilivideo.internal.command.handler.TripleStatusCommandHandler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo

/**
 * Bilibili插件主命令类
 * 
 * 使用TabooLib CommandHelper模块实现的声明式命令系统。
 * 提供玩家Bilibili账户管理和状态查询功能。
 * 支持权限管理和多级子命令结构。
 * 
 * 主要命令：
 * - /bili login [玩家] - 登录Bilibili账户
 * - /bili logout [玩家] - 登出Bilibili账户
 * - /bili triple <BV号> [玩家] - 查询视频三连状态
 * - /bili follow <MID> [玩家] - 查询UP主关注状态
 * 
 * 权限说明：
 * - bilibili.use: 基础使用权限（默认true）
 * - bilibili.admin: 管理员权限，可操作其他玩家
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
@CommandHeader(
    name = "bili",
    aliases = ["bilibili", "bl"],
    description = "Bilibili相关命令",
    permission = "bilibili.use",
    permissionDefault = PermissionDefault.TRUE
)
object BilibiliCommand {

    @CommandBody
    val main = mainCommand {
        createHelper()
    }

    @CommandBody
    val login = subCommand {
        dynamic("player", optional = true) {
            suggestion<ProxyCommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 确定目标玩家
                val targetPlayer = if (argument != null) {
                    // 指定了玩家，需要admin权限
                    if (!sender.hasPermission("bilibili.admin")) {
                        sender.sendError("noPermissionForOthers")
                        return@execute
                    }
                    Bukkit.getPlayer(argument) ?: run {
                        sender.sendError("playerNotFound", "player" to argument)
                        return@execute
                    }
                } else {
                    // 未指定玩家，使用执行者
                    if (sender !is ProxyPlayer) {
                        sender.sendError("commonPlayerOnly")
                        return@execute
                    }
                    sender.cast<Player>()
                }
                
                // 执行命令
                LoginCommandHandler.handleLogin(targetPlayer)
                
                // 如果是为他人执行，给管理员反馈
                if (argument != null && sender is ProxyPlayer && sender.cast<Player>() != targetPlayer) {
                    sender.sendInfo("executedForPlayer", "player" to targetPlayer.name)
                }
            }
        }
    }

    @CommandBody
    val logout = subCommand {
        dynamic("player", optional = true) {
            suggestion<ProxyCommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 确定目标玩家
                val targetPlayer = if (argument != null) {
                    // 指定了玩家，需要admin权限
                    if (!sender.hasPermission("bilibili.admin")) {
                        sender.sendError("noPermissionForOthers")
                        return@execute
                    }
                    Bukkit.getPlayer(argument) ?: run {
                        sender.sendError("playerNotFound", "player" to argument)
                        return@execute
                    }
                } else {
                    // 未指定玩家，使用执行者
                    if (sender !is ProxyPlayer) {
                        sender.sendError("commonPlayerOnly")
                        return@execute
                    }
                    sender.cast<Player>()
                }
                
                // 执行命令
                LogoutCommandHandler.handleLogout(targetPlayer)
                
                // 如果是为他人执行，给管理员反馈
                if (argument != null && sender is ProxyPlayer && sender.cast<Player>() != targetPlayer) {
                    sender.sendInfo("executedForPlayer", "player" to targetPlayer.name)
                }
            }
        }
    }

    @CommandBody
    val triple = subCommand {
        dynamic("bvid") {
            restrict<ProxyCommandSender> { sender, _, argument ->
                if (!isValidBvid(argument)) {
                    sender.sendError("commandsTripleInvalidBvid")
                    false
                } else {
                    true
                }
            }
            
            dynamic("player", optional = true) {
                suggestion<ProxyCommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val bvid = context.argument(-1)
                    
                    // 确定目标玩家
                    val targetPlayer = if (argument != null) {
                        // 指定了玩家，需要admin权限
                        if (!sender.hasPermission("bilibili.admin")) {
                            sender.sendError("noPermissionForOthers")
                            return@execute
                        }
                        Bukkit.getPlayer(argument) ?: run {
                            sender.sendError("playerNotFound", "player" to argument)
                            return@execute
                        }
                    } else {
                        // 未指定玩家，使用执行者
                        if (sender !is ProxyPlayer) {
                            sender.sendError("commonPlayerOnly")
                            return@execute
                        }
                        sender.cast<Player>()
                    }
                    
                    // 执行命令
                    TripleStatusCommandHandler.handleTripleStatus(targetPlayer, bvid)
                    
                    // 如果是为他人执行，给管理员反馈
                    if (argument != null && sender is ProxyPlayer && sender.cast<Player>() != targetPlayer) {
                        sender.sendInfo("executedForPlayer", "player" to targetPlayer.name)
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 没有指定玩家的情况
                if (sender !is ProxyPlayer) {
                    sender.sendError("commonPlayerOnly")
                    return@execute
                }
                
                val bvid = argument
                val player = sender.cast<Player>()
                TripleStatusCommandHandler.handleTripleStatus(player, bvid)
            }
        }
    }

    @CommandBody
    val follow = subCommand {
        dynamic("mid") {
            restrict<ProxyCommandSender> { sender, _, argument ->
                if (!isValidMid(argument)) {
                    sender.sendError("commandsFollowInvalidMid")
                    false
                } else {
                    true
                }
            }
            
            dynamic("player", optional = true) {
                suggestion<ProxyCommandSender> { _, _ ->
                    Bukkit.getOnlinePlayers().map { it.name }
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val midStr = context.argument(-1)
                    val mid = midStr.toLongOrNull()!! // restrict已经验证过格式
                    
                    // 确定目标玩家
                    val targetPlayer = if (argument != null) {
                        // 指定了玩家，需要admin权限
                        if (!sender.hasPermission("bilibili.admin")) {
                            sender.sendError("noPermissionForOthers")
                            return@execute
                        }
                        Bukkit.getPlayer(argument) ?: run {
                            sender.sendError("playerNotFound", "player" to argument)
                            return@execute
                        }
                    } else {
                        // 未指定玩家，使用执行者
                        if (sender !is ProxyPlayer) {
                            sender.sendError("commonPlayerOnly")
                            return@execute
                        }
                        sender.cast<Player>()
                    }
                    
                    // 执行命令
                    FollowStatusCommandHandler.handleFollowStatus(targetPlayer, mid)
                    
                    // 如果是为他人执行，给管理员反馈
                    if (argument != null && sender is ProxyPlayer && sender.cast<Player>() != targetPlayer) {
                        sender.sendInfo("executedForPlayer", "player" to targetPlayer.name)
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                // 没有指定玩家的情况
                if (sender !is ProxyPlayer) {
                    sender.sendError("commonPlayerOnly")
                    return@execute
                }
                
                val midStr = argument
                val mid = midStr.toLongOrNull()!! // restrict已经验证过格式
                val player = sender.cast<Player>()
                FollowStatusCommandHandler.handleFollowStatus(player, mid)
            }
        }
    }
    
    // 辅助验证方法
    private fun isValidBvid(bvid: String): Boolean {
        // BV号格式：BV + 10位大小写字母和数字（不包含0、I、O、l）
        return bvid.matches(Regex("^BV[1-9a-km-zA-HJ-NP-Z]{10}$"))
    }
    
    private fun isValidMid(midStr: String): Boolean {
        val mid = midStr.toLongOrNull()
        return mid != null && mid > 0 && midStr.length >= 6 && midStr.length <= 12
    }
}