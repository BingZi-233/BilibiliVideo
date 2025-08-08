package online.bingzi.bilibili.video.internal.command

import online.bingzi.bilibili.video.internal.cache.VerificationCodeCache
import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.onebot.QQBindManager
import online.bingzi.bilibili.video.internal.cache.cookieCache
import online.bingzi.bilibili.video.internal.cache.midCache
import online.bingzi.bilibili.video.internal.config.SettingConfig
import online.bingzi.bilibili.video.internal.config.VideoConfig
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.*
import taboolib.common.platform.function.getProxyPlayer
import taboolib.common.platform.function.submit
import taboolib.expansion.createHelper
import taboolib.module.chat.colored
import taboolib.module.lang.sendInfoMessage
import taboolib.platform.util.bukkitPlugin

// MainCommand 是一个对象，负责处理所有与 Bilibili 视频相关的命令。
// 该对象包含多个子命令，涉及视频的登录、注销、信息展示等功能。
@CommandHeader(
    name = "bilibili-video", // 命令的名称
    aliases = ["bv", "bilibilivideo"], // 命令的别名
    permission = "BilibiliVideo.command.use", // 默认权限
    permissionDefault = PermissionDefault.TRUE // 默认权限设定为真
)
object MainCommand {
    // main 命令，创建帮助信息
    @CommandBody
    val main = mainCommand {
        createHelper() // 创建帮助信息
    }

    // reload 子命令，允许用户重载配置文件
    @CommandBody(permission = "BilibiliVideo.command.reload", permissionDefault = PermissionDefault.OP)
    val reload = subCommand {
        execute<ProxyCommandSender> { sender, _, _ -> // 执行命令的逻辑
            SettingConfig.config.reload() // 重载设置配置
            VideoConfig.config.reload() // 重载视频配置
            // 向发送者发送重载成功的消息
            sender.infoAsLang("CommandReloadSuccess")
        }
    }

    // unbind 子命令，解除玩家与 MID 的绑定
    @CommandBody(permission = "BilibiliVideo.command.unbind", permissionDefault = PermissionDefault.OP)
    val unbind = subCommand {
        dynamic { // 动态参数，表示玩家名称
            suggestPlayers() // 建议玩家列表
            execute<ProxyCommandSender> { sender, _, argument -> // 执行命令的逻辑
                getProxyPlayer(argument)?.let { // 检查指定的玩家是否存在
                    it.setDataContainer("mid", "") // 清空该玩家的 MID 数据
                    midCache.invalidate(it.uniqueId) // 使该玩家的 MID 缓存失效
                    cookieCache.invalidate(it.uniqueId) // 使该玩家的 Cookie 缓存失效
                } ?: let { // 如果玩家不存在
                    sender.infoAsLang("PlayerNotBindMid", argument) // 发送错误信息
                    return@execute // 退出执行
                }
                sender.infoAsLang("PlayerUnbindSuccess", argument) // 发送成功解除绑定的信息
            }
        }
    }

    // bind 子命令，申请QQ绑定
    @CommandBody(permission = "BilibiliVideo.command.bind", permissionDefault = PermissionDefault.TRUE)
    val bind = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            // 检查是否已绑定
            if (QQBindManager.hasBinding(sender.uniqueId)) {
                sender.infoAsLang("BindAlreadyBound")
                return@execute
            }
            
            // 检查冷却时间
            if (VerificationCodeCache.isInCooldown(sender.uniqueId)) {
                val remaining = VerificationCodeCache.getCooldownRemaining(sender.uniqueId)
                sender.infoAsLang("BindCooldown", remaining)
                return@execute
            }
            
            // 生成验证码
            val code = QQBindManager.requestBinding(sender)
            if (code != null) {
                sender.infoAsLang("BindCodeGeneratedHeader")
                sender.infoAsLang("BindCodeGenerated", code)
                sender.infoAsLang("BindCodeInstruction", code)
                sender.infoAsLang("BindCodeExpireTime")
                sender.infoAsLang("BindCodeGeneratedFooter")
            } else {
                sender.infoAsLang("BindCodeFailed")
            }
        }
    }
    
    // unbindqq 子命令，解除QQ绑定
    @CommandBody(permission = "BilibiliVideo.command.unbindqq", permissionDefault = PermissionDefault.TRUE)
    val unbindqq = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            if (QQBindManager.unbind(sender.uniqueId)) {
                sender.infoAsLang("UnbindQQSuccess")
            } else {
                sender.infoAsLang("UnbindQQNotBound")
            }
        }
    }
    
    // qqinfo 子命令，查看QQ绑定信息
    @CommandBody(permission = "BilibiliVideo.command.qqinfo", permissionDefault = PermissionDefault.TRUE)
    val qqinfo = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            val binding = QQBindManager.getBinding(sender.uniqueId)
            if (binding != null) {
                sender.infoAsLang("QQBindInfoHeader")
                sender.infoAsLang("QQBindInfoNumber", binding.qqNumber)
                sender.infoAsLang("QQBindInfoTime", binding.bindTime)
                sender.infoAsLang("QQBindInfoBilibili", if (binding.bilibiliMid != null) "已绑定" else "未绑定")
                sender.infoAsLang("QQBindInfoFooter")
            } else {
                sender.infoAsLang("QQBindInfoNotBound")
            }
        }
    }

    // login 子命令，启动玩家的登录流程
    @CommandBody(aliases = ["open"], permission = "BilibiliVideo.command.login", permissionDefault = PermissionDefault.TRUE)
    val login = subCommand {
        // 可指定玩家启动登陆流程
        // 可选参数
        // 需要有BilibiliVideo.command.login.other权限才可使用
        dynamic(optional = true, permission = "BilibiliVideo.command.login.other") {
            suggestPlayers() // 建议玩家列表
            execute<ProxyPlayer> { sender, _, argument -> // 执行命令的逻辑
                getProxyPlayer(argument)?.let { player -> // 检查指定的玩家是否存在
                    if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                        sender.infoAsLang("CommandBaffle") // 发送错误信息
                        return@execute // 退出执行
                    }
                    NetworkEngine.generateBilibiliQRCodeUrl(sender, player) // 生成 Bilibili 登录二维码 URL
                }
            }
        }
        execute<ProxyPlayer> { sender, _, _ -> // 执行默认的登录命令
            if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                sender.infoAsLang("CommandBaffle") // 发送错误信息
                return@execute // 退出执行
            }
            NetworkEngine.generateBilibiliQRCodeUrl(sender) // 生成 Bilibili 登录二维码 URL
        }
    }

    // show 子命令，展示玩家绑定的用户信息
    @CommandBody(permission = "BilibiliVideo.command.show", permissionDefault = PermissionDefault.TRUE)
    val show = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                sender.infoAsLang("CommandBaffle") // 发送错误信息
                return@execute // 退出执行
            }
            // 因为是网络操作并且下层未进行异步操作
            // 以防卡死主线程，故这里进行异步操作
            submit(async = true) {
                NetworkEngine.getPlayerBindUserInfo(sender)?.let { // 获取绑定的用户信息
                    sender.infoAsLang("CommandShowBindUserInfo", it.uname, it.mid) // 发送用户信息
                } ?: sender.infoAsLang("CommandShowBindUserInfoNotFound") // 找不到用户信息，发送错误信息
            }
        }
    }

    // logout 子命令，执行用户注销操作
    @CommandBody(permission = "BilibiliVideo.command.logout", permissionDefault = PermissionDefault.TRUE)
    val logout = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            cookieCache.invalidate(sender.uniqueId) // 使该玩家的 Cookie 缓存失效
            sender.infoAsLang("CommandLogoutSuccess") // 发送注销成功的信息
        }
    }

    // receive 子命令，接收视频相关信息
    @CommandBody(aliases = ["use"], permission = "BilibiliVideo.command.receive", permissionDefault = PermissionDefault.TRUE)
    val receive = subCommand {
        dynamic(comment = "bv") { // 动态参数，表示视频的 bv 号
            suggestion<ProxyPlayer> { _, _ -> // 建议视频列表
                VideoConfig.receiveMap.keys.toList() // 返回可用的 bv 号
            }
            execute<ProxyPlayer> { sender, _, argument -> // 执行命令的逻辑
                if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                    sender.infoAsLang("CommandBaffle") // 发送错误信息
                    return@execute // 退出执行
                }
                NetworkEngine.getTripleStatusShow(sender, argument) // 获取视频状态并展示
            }
            literal("show", optional = true) { // 显示视频状态
                execute<ProxyPlayer> { sender, context, _ -> // 执行命令的逻辑
                    if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                        sender.infoAsLang("CommandBaffle") // 发送错误信息
                        return@execute // 退出执行
                    }
                    submit(async = true) { // 异步操作
                        NetworkEngine.getTripleStatusShow(sender, context["bv"]) // 获取视频状态并展示
                    }
                }
            }
            literal("auto", optional = true) { // 自动处理视频状态
                execute<ProxyPlayer> { sender, context, _ -> // 执行命令的逻辑
                    if (baffleCache.hasNext(sender.name).not()) { // 检查是否可以执行命令
                        sender.infoAsLang("CommandBaffle") // 发送错误信息
                        return@execute // 退出执行
                    }
                    submit(async = true) { // 异步操作
                        NetworkEngine.getTripleStatusShow(sender, context["bv"]) // 获取视频状态
                    }
                }
            }
        }
    }

    // version 子命令，显示插件的版本信息
    @CommandBody(permission = "BilibiliVideo.command.version", permissionDefault = PermissionDefault.OP)
    val version = subCommand {
        execute<ProxyCommandSender> { sender, _, _ -> // 执行命令的逻辑
            sender.sendInfoMessage("&a&l插件名称 > ${bukkitPlugin.description.name}".colored()) // 发送插件名称
            sender.sendInfoMessage("&a&l插件版本 > ${bukkitPlugin.description.version}".colored()) // 发送插件版本
            sender.sendInfoMessage("&a&l插件作者 > ${bukkitPlugin.description.authors.joinToString(", ")}".colored()) // 发送插件作者
        }
    }
}