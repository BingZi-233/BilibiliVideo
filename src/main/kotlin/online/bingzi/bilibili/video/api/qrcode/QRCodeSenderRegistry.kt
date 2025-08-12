package online.bingzi.bilibili.video.api.qrcode

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSender
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService

/**
 * 二维码发送器注册API
 * 提供给外部插件注册自定义二维码发送器的接口
 * 
 * 使用示例：
 * ```kotlin
 * // 创建自定义发送器
 * class CustomQRCodeSender : QRCodeSender {
 *     override fun sendQRCode(...): Boolean { ... }
 *     override fun getSenderName(): String = "自定义发送器"
 *     override fun isAvailable(player: ProxyPlayer?): Boolean = true
 * }
 * 
 * // 注册发送器
 * QRCodeSenderRegistry.register(QRCodeSendMode.CUSTOM, CustomQRCodeSender())
 * 
 * // 注销发送器
 * QRCodeSenderRegistry.unregister(QRCodeSendMode.CUSTOM)
 * ```
 */
object QRCodeSenderRegistry {
    
    /**
     * 注册二维码发送器
     * @param mode 发送模式
     * @param sender 发送器实例
     * @return 是否注册成功
     */
    fun register(mode: QRCodeSendMode, sender: QRCodeSender): Boolean {
        return QRCodeSendService.registerSender(mode, sender)
    }
    
    /**
     * 注销二维码发送器
     * @param mode 发送模式
     * @return 是否注销成功
     */
    fun unregister(mode: QRCodeSendMode): Boolean {
        return QRCodeSendService.unregisterSender(mode)
    }
    
    /**
     * 检查指定模式是否已注册
     * @param mode 发送模式
     * @return 是否已注册
     */
    fun isRegistered(mode: QRCodeSendMode): Boolean {
        return QRCodeSendService.getRegisteredModes().contains(mode)
    }
    
    /**
     * 获取所有已注册的发送模式
     * @return 发送模式集合
     */
    fun getRegisteredModes(): Set<QRCodeSendMode> {
        return QRCodeSendService.getRegisteredModes()
    }
    
    /**
     * 获取已注册的发送器数量
     * @return 发送器数量
     */
    fun getRegisteredSenderCount(): Int {
        return QRCodeSendService.getRegisteredSenderCount()
    }
}