package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.credential.QrLoginService
import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import taboolib.common.platform.Plugin

object BilibiliVideo : Plugin() {

    override fun onEnable() {
        DatabaseFactory.initFromConfig()
    }

    override fun onDisable() {
        QrLoginService.shutdown()
        DatabaseFactory.shutdown()
    }
}
