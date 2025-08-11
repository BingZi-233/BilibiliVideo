package online.bingzi.bilibili.video.internal.qrcode

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo

/**
 * 独立的二维码模块管理器
 * 使用TabooLib的生命周期注解自动管理初始化和清理
 * 
 * 该模块完全独立运行，不需要主插件文件的任何调用
 */
object QRCodeModule {
    
    /**
     * 在插件启用阶段自动初始化二维码服务
     */
    @Awake(LifeCycle.ENABLE)
    fun initialize() {
        try {
            console().sendInfo("qrcodeModuleStarting")
            QRCodeSendService.initialize()
            console().sendInfo("qrcodeModuleStarted")
        } catch (e: Exception) {
            console().sendInfo("qrcodeModuleStartFailed", e.message ?: "未知错误")
        }
    }
    
    /**
     * 在插件禁用阶段自动清理二维码服务
     */
    @Awake(LifeCycle.DISABLE)
    fun shutdown() {
        try {
            QRCodeSendService.shutdown()
            console().sendInfo("qrcodeModuleShutdown")
        } catch (e: Exception) {
            console().sendInfo("qrcodeModuleShutdownFailed", e.message ?: "未知错误")
        }
    }
}