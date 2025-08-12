package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo

/**
 * 聊天框二维码发送器注册器
 * 在插件生命周期的ENABLE阶段自动注册
 */
object ChatQRCodeSenderRegister {
    
    /**
     * 在插件启用时自动注册聊天框发送器
     */
    @Awake(LifeCycle.ENABLE)
    fun register() {
        try {
            val sender = ChatQRCodeSender()
            if (QRCodeSendService.registerSender(QRCodeSendMode.CHAT, sender)) {
                console().sendInfo("qrcodeSenderAutoRegistered", "聊天框")
            }
        } catch (e: Exception) {
            console().sendInfo("qrcodeSenderAutoRegisterFailed", "聊天框", e.message ?: "")
        }
    }
    
    /**
     * 在插件禁用时自动注销
     */
    @Awake(LifeCycle.DISABLE)
    fun unregister() {
        QRCodeSendService.unregisterSender(QRCodeSendMode.CHAT)
    }
}