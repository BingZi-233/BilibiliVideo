package online.bingzi.bilibili.video.internal.network

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.common.platform.service.PlatformExecutor
import taboolib.module.lang.sendInfo

/**
 * 登录会话管理器
 * 负责管理登录会话的生命周期，包括自动清理过期会话
 */
object LoginSessionManager {

    /**
     * 清理任务的引用，用于取消任务
     */
    private var cleanupTask: PlatformExecutor.PlatformTask? = null

    /**
     * 初始化登录会话管理器
     * 使用TabooLib的生命周期管理，在插件启用时自动启动
     */
    @Awake(LifeCycle.ENABLE)
    fun onEnable() {
        startCleanupTask()
    }

    /**
     * 关闭登录会话管理器
     * 在插件禁用时停止清理任务
     */
    @Awake(LifeCycle.DISABLE)
    fun onDisable() {
        stopCleanupTask()
    }

    /**
     * 启动定时清理任务
     * 每10分钟执行一次过期会话清理
     */
    private fun startCleanupTask() {
        // 如果已有任务在运行，先取消
        cleanupTask?.cancel()
        
        try {
            // 使用TabooLib的跨平台任务调度器
            // 延迟10分钟后开始，每10分钟执行一次
            cleanupTask = submit(
                async = true,
                delay = 12000L, // 10分钟 = 600秒 = 12000 ticks
                period = 12000L
            ) {
                cleanupExpiredSessions()
            }
            
            console().sendInfo("loginSessionManagerStarted")
        } catch (e: Exception) {
            // 如果启动失败，记录日志但不中断插件运行
            cleanupTask = null
            console().sendInfo("loginSessionManagerSkipped", e.message ?: "未知错误")
        }
    }

    /**
     * 停止清理任务
     */
    private fun stopCleanupTask() {
        cleanupTask?.let {
            it.cancel()
            cleanupTask = null
            console().sendInfo("loginSessionManagerStopped")
        }
    }

    /**
     * 执行过期会话清理
     * 代理到EnhancedLoginService的清理方法
     */
    private fun cleanupExpiredSessions() {
        try {
            val sessionsBeforeCleanup = EnhancedLoginService.getActiveLoginSessions().size
            EnhancedLoginService.cleanupExpiredSessions()
            val sessionsAfterCleanup = EnhancedLoginService.getActiveLoginSessions().size
            
            val cleanedCount = sessionsBeforeCleanup - sessionsAfterCleanup
            if (cleanedCount > 0) {
                console().sendInfo("loginSessionsCleaned", cleanedCount)
            }
        } catch (e: Exception) {
            console().sendInfo("loginSessionCleanupError", e.message ?: "未知错误")
        }
    }

    /**
     * 手动触发会话清理
     * 可供其他模块调用进行即时清理
     */
    fun forceCleanup() {
        cleanupExpiredSessions()
    }
}