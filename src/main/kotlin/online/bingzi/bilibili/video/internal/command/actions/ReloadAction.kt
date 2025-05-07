package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.config.SettingConfig
import online.bingzi.bilibili.video.internal.config.VideoConfig
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyCommandSender

object ReloadAction {
    fun execute(sender: ProxyCommandSender) {
        SettingConfig.config.reload()
        VideoConfig.config.reload()
        sender.infoAsLang("CommandReloadSuccess")
    }
} 