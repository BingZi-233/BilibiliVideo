package online.bingzi.bilibili.video.internal.binding

import online.bingzi.bilibili.video.internal.database.PlayerQQBindingService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.ConcurrentHashMap

/**
 * QQ验证处理器
 * 负责处理通过OneBot接收的QQ消息，实现自动绑定验证
 */
@Awake(LifeCycle.ENABLE)
object QQVerificationHandler {
    
    // OneBot插件集成状态
    private var oneBotAvailable = false
    
    // 待处理的QQ消息队列：QQ号 -> 消息列表
    private val pendingMessages = ConcurrentHashMap<Long, MutableList<String>>()
    
    /**
     * 初始化QQ验证处理器
     */
    fun initialize() {
        // 检查OneBot插件是否可用
        checkOneBotAvailability()
        
        if (oneBotAvailable) {
            console().sendInfo("qqVerificationHandlerInitialized")
            registerOneBotListener()
        } else {
            console().sendWarn("qqVerificationHandlerOneBotUnavailable")
        }
    }
    
    /**
     * 处理收到的QQ私聊消息
     * @param qqNumber 发送者QQ号
     * @param message 消息内容
     */
    fun handlePrivateMessage(qqNumber: Long, message: String) {
        console().sendInfo("qqVerificationHandlerReceivedMessage", qqNumber.toString(), message)
        
        // 检查是否是验证码格式的消息
        val trimmedMessage = message.trim()
        
        if (isLikelyVerificationCode(trimmedMessage)) {
            // 处理可能的验证码
            AutoBindingService.handleQQVerificationMessage(qqNumber, trimmedMessage).thenAccept { success ->
                if (!success) {
                    // 如果验证码处理失败，可能需要提供帮助信息
                    sendHelpMessage(qqNumber)
                }
            }.exceptionally { throwable ->
                console().sendError("qqVerificationHandlerError", qqNumber.toString(), trimmedMessage, throwable.message ?: "Unknown error")
                sendErrorMessage(qqNumber, "处理验证码时出错")
                null
            }
        } else {
            // 处理其他类型的消息（如帮助请求）
            handleOtherMessage(qqNumber, trimmedMessage)
        }
    }
    
    /**
     * 处理收到的QQ群消息（暂不支持群消息验证）
     * @param groupId 群号
     * @param qqNumber 发送者QQ号
     * @param message 消息内容
     */
    fun handleGroupMessage(groupId: Long, qqNumber: Long, message: String) {
        console().sendInfo("qqVerificationHandlerGroupMessage", groupId.toString(), qqNumber.toString(), message)
        // 群消息暂不处理验证码，可以在这里添加群组功能
    }
    
    /**
     * 通知绑定成功
     * @param qqNumber QQ号
     * @param playerName 玩家名称
     */
    fun notifyBindingSuccess(qqNumber: Long, playerName: String) {
        if (oneBotAvailable) {
            val message = "✅ 绑定成功！\\n" +
                    "您的QQ号已成功绑定到玩家：$playerName\\n" +
                    "现在您可以接收游戏内的二维码等消息了！"
            sendPrivateMessage(qqNumber, message)
        }
    }
    
    /**
     * 通知绑定失败
     * @param qqNumber QQ号
     * @param reason 失败原因
     */
    fun notifyBindingFailure(qqNumber: Long, reason: String) {
        if (oneBotAvailable) {
            val message = "❌ 绑定失败\\n" +
                    "原因：$reason\\n" +
                    "请重新在游戏中使用 /bilibilivideo qqbind auto 命令获取新的验证码"
            sendPrivateMessage(qqNumber, message)
        }
    }
    
    /**
     * 检查消息是否像验证码
     */
    private fun isLikelyVerificationCode(message: String): Boolean {
        // 检查是否为纯数字，且长度在合理范围内
        return message.matches(Regex("^\\d{4,10}$"))
    }
    
    /**
     * 处理非验证码消息
     */
    private fun handleOtherMessage(qqNumber: Long, message: String) {
        when (message.lowercase()) {
            "help", "帮助", "?" -> sendHelpMessage(qqNumber)
            "status", "状态" -> sendStatusMessage(qqNumber)
            else -> {
                // 检查用户是否已绑定
                PlayerQQBindingService.getPlayerByQQNumber(qqNumber).thenAccept { playerUuidStr ->
                    if (playerUuidStr != null) {
                        val message = "您已绑定到玩家账户。\\n" +
                                "发送 'help' 获取帮助信息"
                        sendPrivateMessage(qqNumber, message)
                    } else {
                        val message = "您尚未绑定玩家账户。\\n" +
                                "请先在游戏中使用 /bilibilivideo qqbind auto 命令开始绑定流程\\n" +
                                "然后将获得的验证码发送给我"
                        sendPrivateMessage(qqNumber, message)
                    }
                }
            }
        }
    }
    
    /**
     * 发送帮助消息
     */
    private fun sendHelpMessage(qqNumber: Long) {
        val message = "🤖 BilibiliVideo QQ绑定助手\\n\\n" +
                "功能说明：\\n" +
                "• 在游戏中使用 /bilibilivideo qqbind auto 获取验证码\\n" +
                "• 将验证码发送给我完成绑定\\n" +
                "• 绑定后可接收游戏内的二维码消息\\n\\n" +
                "命令：\\n" +
                "• help - 显示此帮助\\n" +
                "• status - 查看绑定状态"
        
        sendPrivateMessage(qqNumber, message)
    }
    
    /**
     * 发送状态消息
     */
    private fun sendStatusMessage(qqNumber: Long) {
        PlayerQQBindingService.getPlayerByQQNumber(qqNumber).thenAccept { playerUuidStr ->
            val message = if (playerUuidStr != null) {
                "✅ 绑定状态：已绑定\\n" +
                        "您的QQ号已绑定到游戏账户"
            } else {
                "❌ 绑定状态：未绑定\\n" +
                        "请在游戏中使用 /bilibilivideo qqbind auto 开始绑定"
            }
            sendPrivateMessage(qqNumber, message)
        }.exceptionally { throwable ->
            console().sendError("qqVerificationHandlerStatusError", qqNumber.toString(), throwable.message ?: "Unknown error")
            sendErrorMessage(qqNumber, "查询绑定状态时出错")
            null
        }
    }
    
    /**
     * 发送错误消息
     */
    private fun sendErrorMessage(qqNumber: Long, error: String) {
        val message = "❌ 出错了：$error\\n" +
                "发送 'help' 获取帮助信息"
        sendPrivateMessage(qqNumber, message)
    }
    
    /**
     * 检查OneBot插件可用性
     */
    private fun checkOneBotAvailability() {
        try {
            // 检查OneBot插件是否存在且可用
            val oneBotClass = Class.forName("online.bingzi.onebot.OneBot")
            val instanceField = oneBotClass.getDeclaredField("INSTANCE")
            val oneBotInstance = instanceField.get(null)
            
            if (oneBotInstance != null) {
                // 进一步检查OneBot是否已连接
                val isConnectedMethod = oneBotClass.getMethod("isConnected")
                val connected = isConnectedMethod.invoke(oneBotInstance) as Boolean
                
                oneBotAvailable = connected
                if (connected) {
                    console().sendInfo("qqVerificationHandlerOneBotDetected")
                } else {
                    console().sendWarn("qqVerificationHandlerOneBotNotConnected")
                }
            } else {
                oneBotAvailable = false
                console().sendWarn("qqVerificationHandlerOneBotInstanceNull")
            }
        } catch (e: ClassNotFoundException) {
            oneBotAvailable = false
            console().sendWarn("qqVerificationHandlerOneBotNotInstalled")
        } catch (e: Exception) {
            oneBotAvailable = false
            console().sendWarn("qqVerificationHandlerOneBotCheckFailed", e.message ?: "Unknown error")
        }
    }
    
    /**
     * 注册OneBot消息监听器
     */
    private fun registerOneBotListener() {
        try {
            // 通过反射获取OneBot实例并注册消息监听器
            val oneBotClass = Class.forName("online.bingzi.onebot.OneBot")
            val instanceField = oneBotClass.getDeclaredField("INSTANCE")
            val oneBotInstance = instanceField.get(null)
            
            if (oneBotInstance != null) {
                // 注册私聊消息监听器
                val registerPrivateMessageListenerMethod = oneBotClass.getMethod("registerPrivateMessageListener", 
                    Class.forName("kotlin.jvm.functions.Function2"))
                
                // 创建消息处理函数
                val messageHandler = { qqNumber: Long, message: String ->
                    handlePrivateMessage(qqNumber, message)
                }
                
                registerPrivateMessageListenerMethod.invoke(oneBotInstance, messageHandler)
                
                // 注册群消息监听器
                val registerGroupMessageListenerMethod = oneBotClass.getMethod("registerGroupMessageListener", 
                    Class.forName("kotlin.jvm.functions.Function3"))
                
                // 创建群消息处理函数
                val groupMessageHandler = { groupId: Long, qqNumber: Long, message: String ->
                    handleGroupMessage(groupId, qqNumber, message)
                }
                
                registerGroupMessageListenerMethod.invoke(oneBotInstance, groupMessageHandler)
                
                console().sendInfo("qqVerificationHandlerListenerRegistered")
            } else {
                throw RuntimeException("OneBot instance is null")
            }
        } catch (e: Exception) {
            console().sendError("qqVerificationHandlerListenerError", e.message ?: "Unknown error")
            oneBotAvailable = false
        }
    }
    
    /**
     * 发送私聊消息
     */
    private fun sendPrivateMessage(qqNumber: Long, message: String) {
        if (!oneBotAvailable) {
            console().sendWarn("qqVerificationHandlerSendFailedNoOneBot", qqNumber.toString())
            return
        }
        
        try {
            // 通过反射调用OneBot API发送私聊消息
            val oneBotClass = Class.forName("online.bingzi.onebot.OneBot")
            val instanceField = oneBotClass.getDeclaredField("INSTANCE")
            val oneBotInstance = instanceField.get(null)
            
            if (oneBotInstance != null) {
                val sendPrivateMessageMethod = oneBotClass.getMethod("sendPrivateMessage", Long::class.java, String::class.java)
                sendPrivateMessageMethod.invoke(oneBotInstance, qqNumber, message)
                
                console().sendInfo("qqVerificationHandlerSendMessage", qqNumber.toString(), message.replace("\\n", " "))
            } else {
                console().sendWarn("qqVerificationHandlerOneBotInstanceNull")
            }
        } catch (e: Exception) {
            console().sendError("qqVerificationHandlerSendError", qqNumber.toString(), e.message ?: "Unknown error")
        }
    }
    
    /**
     * 获取QQ验证处理器统计信息
     */
    fun getStats(): QQVerificationStats {
        return QQVerificationStats(
            oneBotAvailable = oneBotAvailable,
            pendingMessagesCount = pendingMessages.size
        )
    }
    
    /**
     * 关闭QQ验证处理器
     */
    fun shutdown() {
        pendingMessages.clear()
        console().sendInfo("qqVerificationHandlerShutdown")
    }
    
    /**
     * QQ验证统计信息
     */
    data class QQVerificationStats(
        val oneBotAvailable: Boolean,
        val pendingMessagesCount: Int
    )
}