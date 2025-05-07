package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.submit

object ReceiveAction {
    private fun handleAction(sender: ProxyPlayer, bv: String, isAuto: Boolean = false) {
        if (baffleCache.hasNext(sender.name).not()) {
            sender.infoAsLang("CommandBaffle")
            return
        }
        // The original code for "auto" and default/"show" was identical in its NetworkEngine call.
        // If 'isAuto' needs different logic in the future, it can be branched here.
        submit(async = true) {
            NetworkEngine.getTripleStatusShow(sender, bv)
        }
    }

    fun executeDefault(sender: ProxyPlayer, bv: String) {
        handleAction(sender, bv)
    }

    fun executeShow(sender: ProxyPlayer, bv: String) {
        handleAction(sender, bv)
    }

    fun executeAuto(sender: ProxyPlayer, bv: String) {
        handleAction(sender, bv, isAuto = true)
    }
} 