package online.bingzi.bilibili.video.internal.database

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.getDataFolder
import taboolib.module.configuration.Configuration
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 数据库维护工具
 * 负责定期清理过期数据和其他维护任务
 */
object DatabaseMaintenance {

    /**
     * 定时任务执行器
     */
    private lateinit var scheduler: ScheduledExecutorService

    /**
     * 在插件启动后启动定期维护任务
     */
    @Awake(LifeCycle.ACTIVE)
    fun startMaintenance() {
        val config = loadConfig()

        if (!config.cleanupEnabled) {
            console().sendInfo("databaseInitialized")
            return
        }

        // 创建单线程调度器
        scheduler = Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "BilibiliVideo-DatabaseMaintenance").apply {
                isDaemon = true
            }
        }

        // 启动定期清理任务
        scheduler.scheduleAtFixedRate({
            try {
                performMaintenance(config)
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseCleanupError", e.message ?: "Unknown error")
            }
        }, config.intervalHours.toLong(), config.intervalHours.toLong(), TimeUnit.HOURS)

        console().sendInfo("databaseInitialized")
    }

    /**
     * 插件禁用时停止维护任务
     */
    @Awake(LifeCycle.DISABLE)
    fun stopMaintenance() {
        if (::scheduler.isInitialized) {
            scheduler.shutdown()
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow()
                }
            } catch (e: InterruptedException) {
                scheduler.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * 执行维护任务
     */
    private fun performMaintenance(config: MaintenanceConfig) {
        console().sendInfo("databaseInitializing")

        // 清理过期绑定
        val cleanedCount = MultiTableDatabaseService.cleanupExpiredData(
            config.bindingExpireDays,
            config.cookieExpireDays
        ).get()

        if (cleanedCount > 0) {
            console().sendInfo("playerBilibiliDatabaseCleanupComplete", cleanedCount)
        }

        console().sendInfo("databaseInitialized")
    }

    /**
     * 手动触发维护任务
     *
     * @return 清理的记录数量
     */
    fun manualMaintenance(): Int {
        val config = loadConfig()
        return MultiTableDatabaseService.cleanupExpiredData(
            config.bindingExpireDays,
            config.cookieExpireDays
        ).get()
    }

    /**
     * 加载维护配置
     */
    private fun loadConfig(): MaintenanceConfig {
        val configFile = File(getDataFolder(), "database.yml")
        val config = Configuration.loadFromFile(configFile)
        val maintenanceSection = config.getConfigurationSection("maintenance")
        val cleanupSection = maintenanceSection?.getConfigurationSection("cleanup")
        val thresholdsSection = cleanupSection?.getConfigurationSection("thresholds")

        return MaintenanceConfig(
            cleanupEnabled = cleanupSection?.getBoolean("enabled") ?: true,
            intervalHours = cleanupSection?.getInt("interval-hours") ?: 24,
            bindingExpireDays = thresholdsSection?.getInt("binding-expire-days") ?: 30,
            cookieExpireDays = thresholdsSection?.getInt("cookie-expire-days") ?: 7
        )
    }

    /**
     * 维护配置数据类
     */
    data class MaintenanceConfig(
        val cleanupEnabled: Boolean,
        val intervalHours: Int,
        val bindingExpireDays: Int,
        val cookieExpireDays: Int
    )
}