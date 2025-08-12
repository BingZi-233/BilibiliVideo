package online.bingzi.bilibili.video.api.qrcode

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
 * QRCodeSenderRegistry.register("自定义发送器", CustomQRCodeSender())
 * 
 * // 注销发送器
 * QRCodeSenderRegistry.unregister("自定义发送器")
 * ```
 */
object QRCodeSenderRegistry {
    
    /**
     * 注册二维码发送器
     * @param senderName 发送器名称
     * @param sender 发送器实例
     * @return 是否注册成功
     */
    fun register(senderName: String, sender: QRCodeSender): Boolean {
        return QRCodeSendService.registerSender(senderName, sender)
    }
    
    /**
     * 注销二维码发送器
     * @param senderName 发送器名称
     * @return 是否注销成功
     */
    fun unregister(senderName: String): Boolean {
        return QRCodeSendService.unregisterSender(senderName)
    }
    
    /**
     * 检查指定发送器是否已注册
     * @param senderName 发送器名称
     * @return 是否已注册
     */
    fun isRegistered(senderName: String): Boolean {
        return QRCodeSendService.getRegisteredSenderNames().contains(senderName)
    }
    
    /**
     * 获取所有已注册的发送器名称
     * @return 发送器名称集合
     */
    fun getRegisteredSenderNames(): Set<String> {
        return QRCodeSendService.getRegisteredSenderNames()
    }
    
    /**
     * 获取已注册的发送器数量
     * @return 发送器数量
     */
    fun getRegisteredSenderCount(): Int {
        return QRCodeSendService.getRegisteredSenderCount()
    }
}