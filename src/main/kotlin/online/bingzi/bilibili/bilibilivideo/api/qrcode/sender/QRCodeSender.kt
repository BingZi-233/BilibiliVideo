package online.bingzi.bilibili.bilibilivideo.api.qrcode.sender

import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyResult
import online.bingzi.bilibili.bilibilivideo.api.qrcode.options.SendOptions
import online.bingzi.bilibili.bilibilivideo.api.qrcode.result.SendResult
import org.bukkit.entity.Player
import java.util.function.Consumer

interface QRCodeSender {
    val id: String                                              // 唯一标识符
    val name: String                                            // 显示名称
    
    fun isAvailable(): Boolean                                  // 检查发送器是否可用
    fun checkDependencies(): DependencyResult                   // 检查依赖
    
    fun send(player: Player, content: String, options: SendOptions): SendResult
    fun sendAsync(player: Player, content: String, options: SendOptions, callback: Consumer<SendResult>)
    
    fun initialize()                                            // 初始化资源
    fun shutdown()                                              // 清理资源
}