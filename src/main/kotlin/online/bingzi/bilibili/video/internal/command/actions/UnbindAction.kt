package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.cache.cookieCache
import online.bingzi.bilibili.video.internal.cache.midCache
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.function.getProxyPlayer

object UnbindAction {
    fun execute(sender: ProxyCommandSender, playerName: String) {
        getProxyPlayer(playerName)?.let {
            it.setDataContainer("mid", "")
            midCache.invalidate(it.uniqueId)
            cookieCache.invalidate(it.uniqueId)
            sender.infoAsLang("PlayerUnbindSuccess", playerName)
        } ?: sender.infoAsLang("PlayerNotBindMid", playerName) // Changed from PlayerNotBindMid to reflect that player might not exist or not be bound.
                                                                // Consider adding a distinct PlayerNotFound message if needed.
    }
} 