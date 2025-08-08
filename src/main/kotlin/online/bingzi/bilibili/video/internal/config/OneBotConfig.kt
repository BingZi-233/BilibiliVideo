package online.bingzi.bilibili.video.internal.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * OneBot配置
 * 
 * 管理OneBot相关的所有配置项
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object OneBotConfig {
    
    @Config("onebot.yml")
    lateinit var config: Configuration
        private set
    
    /**
     * 是否启用OneBot功能
     */
    val isEnabled: Boolean
        get() = config.getBoolean("onebot.enabled", true)
    
    /**
     * WebSocket连接地址
     */
    val url: String
        get() = config.getString("onebot.url", "ws://127.0.0.1:8080")!!
    
    /**
     * 访问令牌
     */
    val token: String
        get() = config.getString("onebot.token", "")!!
    
    /**
     * 机器人QQ号
     */
    val botId: Long
        get() = config.getLong("onebot.bot-id", 0)
    
    /**
     * 是否启用自动重连
     */
    val reconnectEnabled: Boolean
        get() = config.getBoolean("onebot.reconnect.enabled", true)
    
    /**
     * 重连间隔（秒）
     */
    val reconnectInterval: Int
        get() = config.getInt("onebot.reconnect.interval", 5)
    
    /**
     * 最大重连次数
     */
    val maxReconnectAttempts: Int
        get() = config.getInt("onebot.reconnect.max-attempts", -1)
    
    // QQ绑定相关配置
    
    /**
     * 验证码长度
     */
    val codeLength: Int
        get() = config.getInt("qq-binding.code-length", 6)
    
    /**
     * 验证码有效期（秒）
     */
    val codeExpire: Int
        get() = config.getInt("qq-binding.code-expire", 300)
    
    /**
     * 验证码前缀
     */
    val codePrefix: String
        get() = config.getString("qq-binding.code-prefix", "绑定")!!
    
    /**
     * 申请验证码冷却时间（秒）
     */
    val bindCooldown: Int
        get() = config.getInt("qq-binding.cooldown", 60)
    
    /**
     * 是否允许一个QQ绑定多个游戏账号
     */
    val allowMultipleBinding: Boolean
        get() = config.getBoolean("qq-binding.allow-multiple", false)
    
    // 二维码相关配置
    
    /**
     * 二维码大小
     */
    val qrCodeSize: Int
        get() = config.getInt("qrcode.size", 256)
    
    /**
     * 是否启用过期提醒
     */
    val qrCodeExpireReminder: Boolean
        get() = config.getBoolean("qrcode.expire-reminder", true)
    
    /**
     * 过期提醒时间（秒）
     */
    val qrCodeExpireReminderTime: Int
        get() = config.getInt("qrcode.expire-reminder-time", 30)
    
    /**
     * 管理员群号
     */
    val adminGroupId: Long
        get() = config.getLong("admin.group-id", 0)
    
    /**
     * 重载配置
     */
    fun reload() {
        config.reload()
    }
}