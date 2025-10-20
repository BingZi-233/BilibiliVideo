package online.bingzi.bilibili.bilibilivideo

import online.bingzi.bilibili.bilibilivideo.api.qrcode.registry.QRCodeSenderRegistry
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object BilibiliVideo : Plugin() {
    override fun onLoad() {
        info("正在加载 BilibiliVideo 插件...")
        info("BilibiliVideo 插件加载完成！")
    }

    override fun onEnable() {
        info("正在启动 BilibiliVideo 插件...")
        info("BilibiliVideo 插件启动完成！")
    }

    override fun onActive() {
        info("正在激活 BilibiliVideo 插件...")
        info("BilibiliVideo 插件激活完成！")
    }

    override fun onDisable() {
        info("正在禁用 BilibiliVideo 插件...")
        // 关闭所有已注册的二维码发送器，释放资源
        QRCodeSenderRegistry.shutdown()
        info("BilibiliVideo 插件禁用完成！")
    }
}
