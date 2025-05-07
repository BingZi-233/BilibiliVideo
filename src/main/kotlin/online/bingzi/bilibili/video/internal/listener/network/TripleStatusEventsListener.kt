package online.bingzi.bilibili.video.internal.listener.network

import online.bingzi.bilibili.video.api.event.TripleSendRewardsEvent
import online.bingzi.bilibili.video.api.event.TripleStatusResultEvent
import online.bingzi.bilibili.video.api.event.TripleStatusShowResultEvent
import online.bingzi.bilibili.video.internal.cache.bvCache
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import online.bingzi.bilibili.video.internal.helper.warningAsLang
import taboolib.common.platform.event.SubscribeEvent

object TripleStatusEventsListener {

    private const val ERROR_MESSAGE_NOT_PROVIDED: String = "Bilibili未提供任何错误信息" // 从NetworkEngine移来

    @SubscribeEvent
    fun onTripleStatusResult(event: TripleStatusResultEvent) {
        val player = event.player
        val bvid = event.bvid

        if (event.isSuccess && event.tripleData != null) {
            val tripleData = event.tripleData
            if (tripleData.coin && tripleData.fav && tripleData.like) {
                player.setDataContainer(bvid, true.toString())
                bvCache[player.uniqueId to bvid] = true
                TripleSendRewardsEvent(player, bvid).call()
            } else {
                player.infoAsLang(
                    "GetTripleStatusFailure",
                    tripleData.like,
                    tripleData.coin,
                    tripleData.multiply,
                    tripleData.fav
                )
            }
        } else {
            // 处理错误情况
            when (event.code) {
                -101 -> player.infoAsLang("GetTripleStatusCookieInvalid")
                10003 -> player.infoAsLang("GetTripleStatusTargetFailed")
                // 根据 isSuccess 和 errorMessage 处理其他错误
                else -> {
                    if (event.errorMessage != null && event.errorMessage.contains("HTTP受限")) { // 基于原NetworkEngine的判断
                         player.infoAsLang("NetworkRequestRefuse", event.errorMessage)
                    } else if (event.code != null) {
                         player.infoAsLang("GetTripleStatusError", event.errorMessage ?: ERROR_MESSAGE_NOT_PROVIDED)
                    }
                    else {
                         player.infoAsLang("NetworkRequestFailure", event.errorMessage ?: ERROR_MESSAGE_NOT_PROVIDED)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTripleStatusShowResult(event: TripleStatusShowResultEvent) {
        if (event.isSuccess) {
            TripleSendRewardsEvent(event.player, event.bvid).call()
        } else {
            // `showAction.handle` 内部通常已发送消息
            // 如果需要，可以在这里添加额外的错误提示
            // event.player.warningAsLang("TripleShowModeFailure", event.errorMessage ?: "Show模式三连失败")
        }
    }
} 