package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.cache.cookieCache
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer

object LogoutAction {
    fun execute(sender: ProxyPlayer) {
        cookieCache.invalidate(sender.uniqueId)
        sender.infoAsLang("CommandLogoutSuccess")
    }
} 