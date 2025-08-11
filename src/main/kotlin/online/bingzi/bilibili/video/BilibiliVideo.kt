package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.network.EnhancedLoginService
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.platform.BukkitPlugin

object BilibiliVideo : Plugin() {

    override fun onEnable() {
        info("Successfully running BilibiliVideo!")
        
        // 启动定时任务清理过期的登录会话
        startLoginSessionCleanupTask()
    }
    
    override fun onDisable() {
        info("BilibiliVideo has been disabled.")
    }
    
    /**
     * 启动登录会话清理任务
     */
    private fun startLoginSessionCleanupTask() {
        try {
            // 每10分钟清理一次过期会话
            val bukkitPlugin = BukkitPlugin.getInstance()
            org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(
                bukkitPlugin,
                Runnable {
                    EnhancedLoginService.cleanupExpiredSessions()
                },
                12000L, // 10分钟后开始 (20 ticks/秒 * 60秒 * 10分钟)
                12000L  // 每10分钟执行一次
            )
        } catch (e: Exception) {
            // 如果不在Bukkit环境，跳过定时任务
            info("Not in Bukkit environment, skipping periodic tasks")
        }
    }
}