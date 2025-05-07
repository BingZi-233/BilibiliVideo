package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.getProxyPlayer

object LoginAction {
    fun execute(sender: ProxyPlayer) {
        if (baffleCache.hasNext(sender.name).not()) {
            sender.infoAsLang("CommandBaffle")
            return
        }
        NetworkEngine.generateBilibiliQRCodeUrl(sender)
    }

    fun execute(sender: ProxyPlayer, targetPlayerName: String) {
        getProxyPlayer(targetPlayerName)?.let { player ->
            if (baffleCache.hasNext(sender.name).not()) {
                sender.infoAsLang("CommandBaffle")
                return
            }
            NetworkEngine.generateBilibiliQRCodeUrl(sender, player)
        } ?: sender.infoAsLang("UserOffline", targetPlayerName) // Assuming UserOffline is a lang key for player not found/offline
    }
} 