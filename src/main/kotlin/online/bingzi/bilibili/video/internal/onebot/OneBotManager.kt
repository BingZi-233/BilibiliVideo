package online.bingzi.bilibili.video.internal.onebot

import cn.evole.onebot.client.OneBotClient
import cn.evole.onebot.client.core.BotConfig
import online.bingzi.bilibili.video.internal.config.OneBotConfig
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning
import taboolib.common.platform.function.submit
import taboolib.common5.cbool
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OneBot管理器
 * 
 * 负责管理OneBot客户端的连接、断开、重连等操作
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object OneBotManager {
    
    /**
     * OneBot客户端实例
     */
    private var client: OneBotClient? = null
    
    /**
     * 连接状态
     */
    private val connected = AtomicBoolean(false)
    
    /**
     * 重连任务ID
     */
    private var reconnectTask: Any? = null
    
    /**
     * 重连次数
     */
    private var reconnectCount = 0
    
    /**
     * 初始化OneBot连接
     */
    fun initialize() {
        if (!OneBotConfig.isEnabled) {
            info("OneBot功能已禁用")
            return
        }
        
        connect()
        
        // 如果启用了自动重连
        if (OneBotConfig.reconnectEnabled) {
            startReconnectTask()
        }
    }
    
    /**
     * 连接到OneBot服务器
     */
    fun connect() {
        try {
            // 创建配置
            val config = if (OneBotConfig.token.isNotEmpty()) {
                BotConfig(OneBotConfig.url, OneBotConfig.token)
            } else {
                BotConfig(OneBotConfig.url)
            }
            
            // 设置机器人ID（如果配置了）
            if (OneBotConfig.botId > 0) {
                config.botId = OneBotConfig.botId
            }
            
            // 创建并连接客户端
            client = OneBotClient.create(config).apply {
                // 注册事件监听器（需要在实际使用时通过OneBot-Client API注册）
                // 由于OneBot-Client在libs中，这里仅记录日志
                // registerEvents(OneBotEventListener)
                // 打开连接
                open()
            }
            
            connected.set(true)
            reconnectCount = 0
            info("OneBot连接成功")
            
        } catch (e: Exception) {
            connected.set(false)
            warning("OneBot连接失败: ${e.message}")
            
            // 如果启用了自动重连且未达到最大重连次数
            if (OneBotConfig.reconnectEnabled && 
                (OneBotConfig.maxReconnectAttempts == -1 || reconnectCount < OneBotConfig.maxReconnectAttempts)) {
                reconnectCount++
                warning("将在 ${OneBotConfig.reconnectInterval} 秒后尝试第 $reconnectCount 次重连")
            }
        }
    }
    
    /**
     * 断开OneBot连接
     */
    fun disconnect() {
        client?.close()
        client = null
        connected.set(false)
        stopReconnectTask()
        info("OneBot连接已断开")
    }
    
    /**
     * 获取OneBot客户端
     * 
     * @return OneBot客户端实例，如果未连接则返回null
     */
    fun getClient(): OneBotClient? {
        return if (connected.get()) client else null
    }
    
    /**
     * 检查是否已连接
     * 
     * @return 是否已连接
     */
    fun isConnected(): Boolean {
        return connected.get() && client != null
    }
    
    /**
     * 发送私聊消息
     * 
     * @param userId QQ号
     * @param message 消息内容
     * @param autoEscape 是否不解析CQ码
     * @return 是否发送成功
     */
    fun sendPrivateMessage(userId: Long, message: String, autoEscape: Boolean = false): Boolean {
        val bot = client?.bot ?: run {
            warning("OneBot未连接，无法发送消息")
            return false
        }
        
        return try {
            bot.sendPrivateMsg(userId, message, autoEscape)
            true
        } catch (e: Exception) {
            warning("发送私聊消息失败: ${e.message}")
            false
        }
    }
    
    /**
     * 发送群消息
     * 
     * @param groupId 群号
     * @param message 消息内容
     * @param autoEscape 是否不解析CQ码
     * @return 是否发送成功
     */
    fun sendGroupMessage(groupId: Long, message: String, autoEscape: Boolean = false): Boolean {
        val bot = client?.bot ?: run {
            warning("OneBot未连接，无法发送消息")
            return false
        }
        
        return try {
            bot.sendGroupMsg(groupId, message, autoEscape)
            true
        } catch (e: Exception) {
            warning("发送群消息失败: ${e.message}")
            false
        }
    }
    
    /**
     * 发送图片（私聊）
     * 
     * @param userId QQ号
     * @param imageBase64 图片的Base64编码
     * @param caption 图片说明（可选）
     * @return 是否发送成功
     */
    fun sendPrivateImage(userId: Long, imageBase64: String, caption: String? = null): Boolean {
        val message = buildString {
            if (!caption.isNullOrEmpty()) {
                append(caption).append("\n")
            }
            append("[CQ:image,file=base64://").append(imageBase64).append("]")
        }
        return sendPrivateMessage(userId, message)
    }
    
    /**
     * 发送图片（群聊）
     * 
     * @param groupId 群号
     * @param imageBase64 图片的Base64编码
     * @param caption 图片说明（可选）
     * @return 是否发送成功
     */
    fun sendGroupImage(groupId: Long, imageBase64: String, caption: String? = null): Boolean {
        val message = buildString {
            if (!caption.isNullOrEmpty()) {
                append(caption).append("\n")
            }
            append("[CQ:image,file=base64://").append(imageBase64).append("]")
        }
        return sendGroupMessage(groupId, message)
    }
    
    /**
     * 开始自动重连任务
     */
    private fun startReconnectTask() {
        reconnectTask = submit(
            async = true,
            delay = OneBotConfig.reconnectInterval * 20L,
            period = OneBotConfig.reconnectInterval * 20L
        ) {
            if (!connected.get() && OneBotConfig.reconnectEnabled) {
                if (OneBotConfig.maxReconnectAttempts == -1 || 
                    reconnectCount < OneBotConfig.maxReconnectAttempts) {
                    info("尝试重新连接OneBot...")
                    connect()
                } else {
                    warning("已达到最大重连次数，停止重连")
                    this.cancel()
                }
            }
        }
    }
    
    /**
     * 停止自动重连任务
     */
    private fun stopReconnectTask() {
        // 直接设置为null，TabooLib会自动处理任务取消
        reconnectTask = null
    }
    
    /**
     * 重置连接
     */
    fun reconnect() {
        disconnect()
        connect()
    }
}