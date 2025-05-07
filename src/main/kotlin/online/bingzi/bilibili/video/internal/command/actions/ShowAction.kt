package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.submit

object ShowAction {
    fun execute(sender: ProxyPlayer) {
        if (baffleCache.hasNext(sender.name).not()) {
            sender.infoAsLang("CommandBaffle")
            return
        }
        // 因为是网络操作并且下层未进行异步操作
        // 以防卡死主线程，故这里进行异步操作
        submit(async = true) {
            NetworkEngine.getPlayerBindUserInfo(sender)?.let {
                sender.infoAsLang("CommandShowBindUserInfo", it.uname, it.mid)
            } ?: sender.infoAsLang("CommandShowBindUserInfoNotFound")
        }
    }
} 