package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.helper.infoMessageAsLang
import online.bingzi.bilibili.video.internal.helper.DeviceIdentifierHelper
import online.bingzi.bilibili.video.internal.onebot.OneBotManager
import online.bingzi.bilibili.video.internal.onebot.QQBindManager
import taboolib.common.platform.Platform
import taboolib.common.platform.Plugin
import taboolib.module.metrics.Metrics
import taboolib.platform.util.bukkitPlugin

/**
 * Bilibili视频插件
 * 该插件用于在Bukkit平台上提供Bilibili视频相关的功能。
 *
 * 主要功能包括：
 * - 加载和启用插件
 * - 收集插件使用统计信息
 * - 提供基本的启用和关闭信息
 * - OneBot集成支持QQ机器人
 * - QQ绑定系统
 */
object BilibiliVideo : Plugin() {

    /**
     * 插件加载时调用的方法
     * 该方法在插件被加载时执行，可用于初始化插件相关资源。
     */
    override fun onLoad() {
        // 输出加载信息
        infoMessageAsLang("Loading")
        // 输出加载完成信息
        infoMessageAsLang("Loaded")
    }

    /**
     * 插件启用时调用的方法
     * 该方法在插件被启用时执行，主要用于初始化必要的功能和收集统计信息。
     */
    override fun onEnable() {
        // 输出启用信息
        infoMessageAsLang("Enabling")
        
        // 初始化设备标识助手
        DeviceIdentifierHelper.initialize()
        
        // 初始化QQ绑定管理器
        QQBindManager.initialize()
        
        // 初始化OneBot连接
        OneBotManager.initialize()
        
        // 输出指标相关信息
        infoMessageAsLang("Metrics")
        // 初始化Metrics以收集插件的使用统计信息
        Metrics(20132, bukkitPlugin.description.version, Platform.BUKKIT)
        // 输出指标已收集的信息
        infoMessageAsLang("Metricsed")
        // 输出启用完成的信息
        infoMessageAsLang("Enabled")
    }

    /**
     * 插件禁用时调用的方法
     * 该方法在插件被禁用时执行，用于清理资源或执行必要的关闭逻辑。
     */
    override fun onDisable() {
        // 输出禁用信息
        infoMessageAsLang("Disabling")
        
        // 断开OneBot连接
        OneBotManager.disconnect()
        
        // 清理设备标识缓存
        DeviceIdentifierHelper.clearCache()
        
        // 输出禁用完成的信息
        infoMessageAsLang("Disabled")
    }
}