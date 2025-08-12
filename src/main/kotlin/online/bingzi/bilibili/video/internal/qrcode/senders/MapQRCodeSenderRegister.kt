package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 地图二维码发送器注册器
 * 在插件生命周期的ENABLE阶段自动注册（如果Bukkit环境可用）
 */
object MapQRCodeSenderRegister {
    
    /**
     * 在插件启用时自动注册地图发送器
     * 仅在Bukkit环境可用时注册
     */
    @Awake(LifeCycle.ENABLE)
    fun register() {
        try {
            val sender = MapQRCodeSender()
            // 检查环境是否支持
            if (sender.isAvailable()) {
                if (QRCodeSendService.registerSender(QRCodeSendMode.MAP, sender)) {
                    console().sendInfo("qrcodeSenderAutoRegistered", "地图")
                }
            } else {
                console().sendWarn("qrcodeSenderSkipped", "地图", "Bukkit环境不可用")
            }
        } catch (e: Exception) {
            console().sendInfo("qrcodeSenderAutoRegisterFailed", "地图", e.message ?: "")
        }
    }
    
    /**
     * 在插件禁用时自动注销
     */
    @Awake(LifeCycle.DISABLE)
    fun unregister() {
        QRCodeSendService.unregisterSender(QRCodeSendMode.MAP)
    }
}