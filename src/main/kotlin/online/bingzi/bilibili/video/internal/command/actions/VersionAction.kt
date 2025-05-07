package online.bingzi.bilibili.video.internal.command.actions

import taboolib.common.platform.ProxyCommandSender
import taboolib.module.chat.colored
import taboolib.module.lang.sendInfoMessage
import taboolib.platform.util.bukkitPlugin

object VersionAction {
    fun execute(sender: ProxyCommandSender) {
        sender.sendInfoMessage("&a&l插件名称 > ${bukkitPlugin.description.name}".colored())
        sender.sendInfoMessage("&a&l插件版本 > ${bukkitPlugin.description.version}".colored())
        sender.sendInfoMessage("&a&l插件作者 > ${bukkitPlugin.description.authors.joinToString(", ")}".colored())
    }
} 