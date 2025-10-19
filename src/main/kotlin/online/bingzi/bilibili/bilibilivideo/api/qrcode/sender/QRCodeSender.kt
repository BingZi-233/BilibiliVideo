package online.bingzi.bilibili.bilibilivideo.api.qrcode.sender

import online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata.DependencyResult
import online.bingzi.bilibili.bilibilivideo.api.qrcode.options.SendOptions
import online.bingzi.bilibili.bilibilivideo.api.qrcode.result.SendResult
import org.bukkit.entity.Player
import java.util.function.Consumer

/**
 * 二维码发送器接口
 * 
 * 定义了二维码发送器的基本操作规范，包括发送、依赖检查、生命周期管理等功能。
 * 实现此接口可以支持多种不同的二维码发送方式（如本地图片、URL、邮件等）。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
interface QRCodeSender {
    /**
     * 发送器的唯一标识符
     * 
     * 用于在注册表中标识不同的发送器实现
     */
    val id: String
    
    /**
     * 发送器的显示名称
     * 
     * 用于在界面中显示给用户的友好名称
     */
    val name: String
    
    /**
     * 检查发送器是否可用
     * 
     * @return true 如果发送器当前可用，false 否则
     */
    fun isAvailable(): Boolean
    
    /**
     * 检查发送器的依赖项
     * 
     * @return 依赖检查结果，包含所有依赖项的状态信息
     */
    fun checkDependencies(): DependencyResult
    
    /**
     * 同步发送二维码
     * 
     * @param player 目标玩家
     * @param content 二维码内容
     * @param options 发送选项
     * @return 发送结果
     */
    fun send(player: Player, content: String, options: SendOptions): SendResult
    
    /**
     * 异步发送二维码
     * 
     * @param player 目标玩家
     * @param content 二维码内容
     * @param options 发送选项
     * @param callback 结果回调函数
     */
    fun sendAsync(player: Player, content: String, options: SendOptions, callback: Consumer<SendResult>)
    
    /**
     * 初始化发送器资源
     * 
     * 在发送器被注册时调用，用于初始化必要的资源
     */
    fun initialize()
    
    /**
     * 清理发送器资源
     * 
     * 在发送器被卸载或插件关闭时调用，用于释放占用的资源
     */
    fun shutdown()
}