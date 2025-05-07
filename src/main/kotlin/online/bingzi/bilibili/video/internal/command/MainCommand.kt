package online.bingzi.bilibili.video.internal.command

import online.bingzi.bilibili.video.internal.command.actions.*
import online.bingzi.bilibili.video.internal.config.VideoConfig // Kept for suggestions
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.*
import taboolib.expansion.createHelper

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
            ReloadAction.execute(sender)
        }
    }

    // unbind 子命令，解除玩家与 MID 的绑定
    @CommandBody(permission = "BilibiliVideo.command.unbind", permissionDefault = PermissionDefault.OP)
    val unbind = subCommand {
        dynamic { // 动态参数，表示玩家名称
            suggestPlayers() // 建议玩家列表
            execute<ProxyCommandSender> { sender, _, argument -> // 执行命令的逻辑
                UnbindAction.execute(sender, argument)
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
                LoginAction.execute(sender, argument)
            }
        }
        execute<ProxyPlayer> { sender, _, _ -> // 执行默认的登录命令
            LoginAction.execute(sender)
        }
    }

    // show 子命令，展示玩家绑定的用户信息
    @CommandBody(permission = "BilibiliVideo.command.show", permissionDefault = PermissionDefault.TRUE)
    val show = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            ShowAction.execute(sender)
        }
    }

    // logout 子命令，执行用户注销操作
    @CommandBody(permission = "BilibiliVideo.command.logout", permissionDefault = PermissionDefault.TRUE)
    val logout = subCommand {
        execute<ProxyPlayer> { sender, _, _ -> // 执行命令的逻辑
            LogoutAction.execute(sender)
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
                ReceiveAction.executeDefault(sender, argument)
            }
            literal("show", optional = true) { // 显示视频状态
                execute<ProxyPlayer> { sender, context, _ -> // 执行命令的逻辑
                    ReceiveAction.executeShow(sender, context["bv"])
                }
            }
            literal("auto", optional = true) { // 自动处理视频状态
                execute<ProxyPlayer> { sender, context, _ -> // 执行命令的逻辑
                    ReceiveAction.executeAuto(sender, context["bv"])
                }
            }
        }
    }

    // video 子命令，展示视频的相关信息
    @CommandBody(permission = "BilibiliVideo.command.video", permissionDefault = PermissionDefault.TRUE)
    val video = subCommand {
        dynamic(comment = "bv") { // 动态参数，表示视频的 bv 号
            suggestion<ProxyPlayer> { _, _ -> // 建议视频列表
                VideoConfig.receiveMap.keys.toList() // 返回可用的 bv 号
            }
            execute<ProxyPlayer> { sender, _, argument -> // 执行命令的逻辑
                VideoAction.execute(sender, argument)
            }
        }
    }

    // version 子命令，显示插件的版本信息
    @CommandBody(permission = "BilibiliVideo.command.version", permissionDefault = PermissionDefault.OP)
    val version = subCommand {
        execute<ProxyCommandSender> { sender, _, _ -> // 执行命令的逻辑
            VersionAction.execute(sender)
        }
    }
}