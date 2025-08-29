package online.bingzi.bilibili.bilibilivideo.api.qrcode.registry

import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyResult
import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyStatus
import online.bingzi.bilibili.bilibilivideo.api.qrcode.sender.QRCodeSender
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

object QRCodeSenderRegistry {
    private var activeSender: QRCodeSender? = null
    private val availableSenders = ConcurrentHashMap<String, QRCodeSender>()
    
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
    
    fun getActiveSender(): QRCodeSender? {
        return activeSender?.takeIf { it.isAvailable() }
    }
    
    fun getAvailableSenders(): Map<String, QRCodeSender> {
        return availableSenders.filter { (_, sender) ->
            sender.isAvailable()
        }
    }
    
    fun checkAllDependencies(): Map<String, DependencyResult> {
        return availableSenders.mapValues { (_, sender) ->
            sender.checkDependencies()
        }
    }
    
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