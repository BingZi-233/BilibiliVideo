package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.api.qrcode.QRCodeSenderRegistry
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSenderNames
import online.bingzi.onebot.api.event.status.OneBotConnectedEvent
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * OneBot二维码发送器注册器
 * 在插件生命周期的ACTIVE阶段注册（需要等待其他服务初始化完成）
 */
object OneBotQRCodeSenderRegister {
    
    /**
     * 在插件激活时注册OneBot发送器
     * 使用ACTIVE生命周期，确保OneBot服务已初始化
     */
   @SubscribeEvent
    fun register(event: OneBotConnectedEvent) {
        try {
            val sender = OneBotQRCodeSender()
            // 检查OneBot服务是否可用
            if (sender.isAvailable()) {
                if (QRCodeSenderRegistry.register(QRCodeSenderNames.ONEBOT, sender)) {
                    console().sendInfo("qrcodeSenderAutoRegistered", "OneBot")
                }
            } else {
                console().sendWarn("qrcodeSenderSkipped", "OneBot", "OneBot服务不可用")
            }
        } catch (e: Exception) {
            console().sendInfo("qrcodeSenderAutoRegisterFailed", "OneBot", e.message ?: "")
        }
    }
    
    /**
     * 在插件禁用时自动注销
     */
    @Awake(LifeCycle.DISABLE)
    fun unregister() {
        QRCodeSenderRegistry.unregister(QRCodeSenderNames.ONEBOT)
    }
}