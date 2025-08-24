package online.bingzi.bilibili.video.internal.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * 配置文件管理器
 * 使用TabooLib的@Config注解自动管理配置文件
 */
object ConfigManager {
    
    /**
     * 主配置文件
     * 包含二维码、登录等基础配置
     */
    @Config("config.yml")
    lateinit var mainConfig: Configuration
        private set
    
    /**
     * 数据库配置文件
     * 包含数据库连接、表结构、维护等配置
     */
    @Config("database.yml")
    lateinit var databaseConfig: Configuration
        private set
    
    /**
     * 获取二维码默认发送模式
     */
    fun getQRCodeDefaultMode(): String {
        return mainConfig.getString("qrcode.default-send-mode", "CHAT")!!
    }
    
    /**
     * 获取二维码聊天框显示大小
     */
    fun getQRCodeChatDisplaySize(): Int {
        return mainConfig.getInt("qrcode.chat.display-size", 25)
    }
    
    /**
     * 获取二维码地图尺寸
     */
    fun getQRCodeMapSize(): Int {
        return mainConfig.getInt("qrcode.map.size", 128)
    }
    
    /**
     * 是否启用地图模式
     */
    fun isMapModeEnabled(): Boolean {
        return mainConfig.getBoolean("qrcode.map.enabled", true)
    }
    
    /**
     * 是否启用聊天框模式
     */
    fun isChatModeEnabled(): Boolean {
        return mainConfig.getBoolean("qrcode.chat.enabled", true)
    }
    
    /**
     * 获取登录二维码超时时间（分钟）
     */
    fun getQRCodeTimeout(): Int {
        return mainConfig.getInt("login.qrcode-timeout", 5)
    }
    
    /**
     * 获取登录状态轮询间隔（秒）
     */
    fun getPollingInterval(): Int {
        return mainConfig.getInt("login.polling-interval", 3)
    }
    
    /**
     * 获取会话清理间隔（分钟）
     */
    fun getSessionCleanupInterval(): Int {
        return mainConfig.getInt("login.session-cleanup-interval", 10)
    }
    
    /**
     * 是否启用外部数据库
     */
    fun isDatabaseExternal(): Boolean {
        return databaseConfig.getBoolean("connection.enable", false)
    }
    
    /**
     * 获取数据表前缀
     */
    fun getTablePrefix(): String {
        return databaseConfig.getString("tables.prefix", "bv_")!!
    }
    
    /**
     * 是否启用数据清理
     */
    fun isCleanupEnabled(): Boolean {
        return databaseConfig.getBoolean("maintenance.cleanup.enabled", true)
    }
    
    /**
     * 获取清理任务执行间隔（小时）
     */
    fun getCleanupIntervalHours(): Int {
        return databaseConfig.getInt("maintenance.cleanup.interval-hours", 24)
    }
    
    /**
     * 是否启用SQL日志
     */
    fun isLogSqlEnabled(): Boolean {
        return databaseConfig.getBoolean("debug.log-sql", false)
    }
    
    /**
     * 获取慢查询阈值（毫秒）
     */
    fun getSlowQueryThreshold(): Long {
        return databaseConfig.getLong("debug.slow-query-threshold", 1000)
    }
    
    // =============== 奖励系统配置 ===============
    
    /**
     * 是否启用奖励系统
     */
    fun isRewardSystemEnabled(): Boolean {
        return mainConfig.getBoolean("reward.enabled", true)
    }
    
    /**
     * 获取每日奖励限制
     */
    fun getRewardDailyLimit(): Int {
        return mainConfig.getInt("reward.daily-limit", 3)
    }
    
    /**
     * 获取视频有效天数
     */
    fun getRewardVideoValidDays(): Int {
        return mainConfig.getInt("reward.video-valid-days", 7)
    }
    
    /**
     * 获取默认奖励脚本
     */
    fun getRewardDefaultScript(): String {
        return mainConfig.getString("reward.default-reward-script", "tell player '恭喜获得三连奖励！'")!!
    }
    
    /**
     * 获取奖励检查间隔（分钟）
     */
    fun getRewardCheckInterval(): Int {
        return mainConfig.getInt("reward.check-interval", 60)
    }
    
    /**
     * 是否需要完整三连
     */
    fun isRewardRequireFullTriple(): Boolean {
        return mainConfig.getBoolean("reward.require-full-triple", false)
    }
    
    /**
     * 获取最低要求操作
     */
    fun getRewardMinimumActions(): List<String> {
        return mainConfig.getStringList("reward.minimum-actions") ?: listOf("LIKE", "COIN")
    }
    
    // =============== 二维码生成配置 ===============
    
    /**
     * 获取二维码前景色（ARGB格式）
     */
    fun getQRCodeForegroundColor(): Int {
        val hexString = mainConfig.getString("qrcode.generation.colors.foreground", "0xFF000000")!!
        return hexString.replace("0x", "").toLong(16).toInt()
    }
    
    /**
     * 获取二维码背景色（ARGB格式）
     */
    fun getQRCodeBackgroundColor(): Int {
        val hexString = mainConfig.getString("qrcode.generation.colors.background", "0xFFFFFFFF")!!
        return hexString.replace("0x", "").toLong(16).toInt()
    }
    
    /**
     * 获取二维码默认尺寸
     */
    fun getQRCodeDefaultSize(): Int {
        return mainConfig.getInt("qrcode.generation.image.default-size", 256)
    }
    
    /**
     * 获取二维码边距大小
     */
    fun getQRCodeMargin(): Int {
        return mainConfig.getInt("qrcode.generation.image.margin", 2)
    }
    
    /**
     * 获取二维码纠错级别
     */
    fun getQRCodeErrorCorrection(): String {
        return mainConfig.getString("qrcode.generation.image.error-correction", "M")!!
    }
    
    /**
     * 获取二维码字符编码
     */
    fun getQRCodeCharacterSet(): String {
        return mainConfig.getString("qrcode.generation.image.character-set", "UTF-8")!!
    }
    
    /**
     * 获取短内容推荐尺寸
     */
    fun getQRCodeShortContentSize(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.thresholds.short-content", 200)
    }
    
    /**
     * 获取中等内容推荐尺寸
     */
    fun getQRCodeMediumContentSize(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.thresholds.medium-content", 256)
    }
    
    /**
     * 获取长内容推荐尺寸
     */
    fun getQRCodeLongContentSize(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.thresholds.long-content", 300)
    }
    
    /**
     * 获取超长内容推荐尺寸
     */
    fun getQRCodeExtraLongContentSize(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.thresholds.extra-long-content", 400)
    }
    
    /**
     * 获取短内容长度上限
     */
    fun getQRCodeShortContentLimit(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.boundaries.short-limit", 50)
    }
    
    /**
     * 获取中等内容长度上限
     */
    fun getQRCodeMediumContentLimit(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.boundaries.medium-limit", 100)
    }
    
    /**
     * 获取长内容长度上限
     */
    fun getQRCodeLongContentLimit(): Int {
        return mainConfig.getInt("qrcode.generation.size-recommendation.boundaries.long-limit", 200)
    }
}