package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.credential.QrLoginService
import online.bingzi.bilibili.video.internal.database.DatabaseFactory
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.module.metrics.Metrics
import taboolib.platform.util.bukkitPlugin

object BilibiliVideo : Plugin() {
    override fun onLoad() {
        info("正在加载 BilibiliVideo 插件...")
        info("BilibiliVideo 插件加载完成！")
    }

    override fun onEnable() {
        info("正在启动 BilibiliVideo 插件...")
        DatabaseFactory.initFromConfig()
        info("BilibiliVideo 插件启动完成！")
    }

    override fun onActive() {
        info("正在激活 BilibiliVideo 插件...")
        // 初始化Metrics以收集插件的使用统计信息
        Metrics(20132, bukkitPlugin.description.version, Platform.BUKKIT)
        info("BilibiliVideo 插件激活完成！")
    }

    override fun onDisable() {
        info("正在禁用 BilibiliVideo 插件...")
        QrLoginService.shutdown()
        DatabaseFactory.shutdown()
        info("BilibiliVideo 插件禁用完成！")
    }
}
