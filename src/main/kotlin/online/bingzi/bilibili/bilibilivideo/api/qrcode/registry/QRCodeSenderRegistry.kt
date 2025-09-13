package online.bingzi.bilibili.bilibilivideo.api.qrcode.registry

import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyResult
import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyStatus
import online.bingzi.bilibili.bilibilivideo.api.qrcode.sender.QRCodeSender
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

/**
 * 二维码发送器注册表
 * 
 * 单例对象，用于管理所有二维码发送器的注册、激活和生命周期。
 * 支持多个发送器同时注册，但同一时间只能有一个发送器处于激活状态。
 * 线程安全，使用ConcurrentHashMap确保并发访问的安全性。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object QRCodeSenderRegistry {
    /** 当前激活的发送器 */
    private var activeSender: QRCodeSender? = null
    /** 所有已注册的发送器 */
    private val availableSenders = ConcurrentHashMap<String, QRCodeSender>()
    
    /**
     * 注册一个二维码发送器
     * 
     * 将发送器添加到注册表中并初始化。如果初始化失败，会自动从注册表中移除。
     * 
     * @param sender 要注册的发送器
     * @return true 如果注册成功，false 如果已存在相同ID的发送器或初始化失败
     */
    fun register(sender: QRCodeSender): Boolean {
        if (availableSenders.containsKey(sender.id)) {
            return false
        }
        
        availableSenders[sender.id] = sender
        
        try {
            sender.initialize()
            return true
        } catch (e: Exception) {
            availableSenders.remove(sender.id)
            return false
        }
    }
    
    /**
     * 取消注册一个二维码发送器
     * 
     * 从注册表中移除指定的发送器，并调用其shutdown方法清理资源。
     * 如果移除的是当前激活的发送器，会将其设为null。
     * 
     * @param senderId 要移除的发送器ID
     * @return true 如果移除成功，false 如果指定ID的发送器不存在
     */
    fun unregister(senderId: String): Boolean {
        val sender = availableSenders.remove(senderId)
        
        if (activeSender?.id == senderId) {
            activeSender?.shutdown()
            activeSender = null
        }
        
        try {
            sender?.shutdown()
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * 激活指定的二维码发送器
     * 
     * 将指定ID的发送器设为当前激活状态。只有可用且依赖满足的发送器才能被激活。
     * 激活前会先关闭当前激活的发送器。
     * 
     * @param senderId 要激活的发送器ID
     * @return true 如果激活成功，false 如果发送器不存在、不可用或依赖不满足
     */
    fun activate(senderId: String): Boolean {
        val sender = availableSenders[senderId]
        if (sender == null || !sender.isAvailable()) {
            return false
        }
        
        val dependencyResult = sender.checkDependencies()
        if (!dependencyResult.satisfied) {
            return false
        }
        
        activeSender?.shutdown()
        activeSender = sender
        return true
    }
    
    /**
     * 获取当前激活的发送器
     * 
     * @return 当前激活且可用的发送器，如果没有激活的发送器或发送器不可用则返回null
     */
    fun getActiveSender(): QRCodeSender? {
        return activeSender?.takeIf { it.isAvailable() }
    }
    
    /**
     * 获取所有可用的发送器
     * 
     * 返回当前所有处于可用状态的发送器映射。
     * 
     * @return 可用发送器的ID到实例的映射
     */
    fun getAvailableSenders(): Map<String, QRCodeSender> {
        return availableSenders.filter { (_, sender) ->
            sender.isAvailable()
        }
    }
    
    /**
     * 检查所有发送器的依赖状态
     * 
     * 对每个已注册的发送器进行依赖检查。
     * 
     * @return 发送器ID到依赖检查结果的映射
     */
    fun checkAllDependencies(): Map<String, DependencyResult> {
        return availableSenders.mapValues { (_, sender) ->
            sender.checkDependencies()
        }
    }
    
    /**
     * 关闭注册表
     * 
     * 关闭所有已注册的发送器并清空注册表。通常在插件关闭时调用。
     * 会忽略关闭过程中产生的异常，确保所有发送器都能被尝试关闭。
     */
    fun shutdown() {
        activeSender?.shutdown()
        activeSender = null
        
        availableSenders.values.forEach { sender ->
            try {
                sender.shutdown()
            } catch (e: Exception) {
                // 忽略关闭异常
            }
        }
        availableSenders.clear()
    }
}