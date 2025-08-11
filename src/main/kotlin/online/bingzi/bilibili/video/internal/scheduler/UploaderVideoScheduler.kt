package online.bingzi.bilibili.video.internal.scheduler

import online.bingzi.bilibili.video.internal.database.dao.UploaderVideoDaoService
import online.bingzi.bilibili.video.internal.database.entity.UploaderConfig
import online.bingzi.bilibili.video.internal.network.UploaderVideoService
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.common.platform.function.submit
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.ConcurrentHashMap

/**
 * UP主视频同步调度器
 * 负责定期同步UP主的视频信息
 */
object UploaderVideoScheduler {

    /**
     * 正在同步的UP主集合（避免重复同步）
     */
    private val syncingUploaders = ConcurrentHashMap.newKeySet<Long>()

    /**
     * 调度任务是否运行中
     */
    private var isRunning = false

    /**
     * 同步间隔（分钟）
     */
    private const val CHECK_INTERVAL_MINUTES = 30L

    /**
     * 启动调度器
     */
    @Awake(LifeCycle.ENABLE)
    fun start() {
        console().sendInfo("uploaderSchedulerStart")
        
        isRunning = true
        
        // 启动定期检查任务
        submit(
            async = true,
            delay = 60L * 20, // 延迟1分钟启动
            period = CHECK_INTERVAL_MINUTES * 60 * 20 // 每30分钟检查一次
        ) {
            if (isRunning) {
                checkAndSyncUploaders()
            }
        }

        // 启动时立即执行一次同步
        submit(async = true, delay = 20L) {
            checkAndSyncUploaders()
        }
    }

    /**
     * 停止调度器
     */
    @Awake(LifeCycle.DISABLE)
    fun stop() {
        isRunning = false
        console().sendInfo("uploaderSchedulerStop")
        syncingUploaders.clear()
    }

    /**
     * 检查并同步需要更新的UP主
     */
    private fun checkAndSyncUploaders() {
        submit(async = true) {
            try {
                console().sendInfo("uploaderSchedulerCheckStart")
                
                // 获取需要同步的UP主配置
                val configs = UploaderVideoDaoService.getConfigsNeedSync().get()
                
                if (configs.isEmpty()) {
                    console().sendInfo("uploaderSchedulerNoNeedSync")
                    return@submit
                }

                console().sendInfo("uploaderSchedulerNeedSync", configs.size.toString())
                
                // 依次同步每个UP主
                configs.forEach { config ->
                    if (!syncingUploaders.contains(config.uploaderUid)) {
                        syncUploader(config)
                        
                        // 添加延迟，避免请求过快
                        Thread.sleep(2000)
                    }
                }
                
                console().sendInfo("uploaderSchedulerCheckComplete")
            } catch (e: Exception) {
                console().sendWarn("uploaderSchedulerCheckError", e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 同步单个UP主的视频
     */
    fun syncUploader(config: UploaderConfig) {
        if (syncingUploaders.contains(config.uploaderUid)) {
            console().sendWarn("uploaderAlreadySyncing", config.uploaderUid.toString())
            return
        }

        syncingUploaders.add(config.uploaderUid)
        
        submit(async = true) {
            try {
                console().sendInfo("uploaderSyncStart", config.uploaderName, config.uploaderUid.toString())
                
                // 获取UP主的所有视频
                val videos = UploaderVideoService.getAllVideos(config.uploaderUid).get()
                
                if (videos.isEmpty()) {
                    console().sendWarn("uploaderNoVideos", config.uploaderName)
                } else {
                    // 保存视频到数据库
                    val savedCount = UploaderVideoDaoService.saveVideos(videos).get()
                    console().sendInfo("uploaderVideosSaved", config.uploaderName, savedCount.toString(), videos.size.toString())
                    
                    // 更新同步时间
                    UploaderVideoDaoService.updateSyncTime(config.uploaderUid).get()
                }
                
                console().sendInfo("uploaderSyncComplete", config.uploaderName)
            } catch (e: Exception) {
                console().sendWarn("uploaderSyncError", config.uploaderName, e.message ?: "Unknown error")
            } finally {
                syncingUploaders.remove(config.uploaderUid)
            }
        }
    }

    /**
     * 手动同步指定UP主
     */
    fun syncUploaderManual(uploaderUid: Long): Boolean {
        return try {
            val config = UploaderVideoDaoService.getAllConfigs().get()
                .find { it.uploaderUid == uploaderUid }
            
            if (config != null) {
                syncUploader(config)
                true
            } else {
                console().sendWarn("uploaderConfigNotFound", uploaderUid.toString())
                false
            }
        } catch (e: Exception) {
            console().sendWarn("uploaderManualSyncError", e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 添加UP主到监控列表
     */
    fun addUploader(uploaderUid: Long, syncIntervalHours: Int = 24): Boolean {
        return try {
            // 获取UP主信息
            val uploaderName = UploaderVideoService.getUploaderInfo(uploaderUid).get()
            
            // 创建配置
            val config = UploaderConfig(uploaderUid, uploaderName, syncIntervalHours)
            
            // 保存配置
            val saved = UploaderVideoDaoService.saveConfig(config).get()
            
            if (saved) {
                console().sendInfo("uploaderAdded", uploaderName, uploaderUid.toString())
                
                // 立即同步一次
                syncUploader(config)
            }
            
            saved
        } catch (e: Exception) {
            console().sendWarn("uploaderAddError", uploaderUid.toString(), e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 移除UP主监控
     */
    fun removeUploader(uploaderUid: Long): Boolean {
        return try {
            val deleted = UploaderVideoDaoService.deleteConfig(uploaderUid).get()
            
            if (deleted) {
                console().sendInfo("uploaderRemoved", uploaderUid.toString())
            }
            
            deleted
        } catch (e: Exception) {
            console().sendWarn("uploaderRemoveError", uploaderUid.toString(), e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 获取同步状态
     */
    fun getSyncStatus(): Map<String, Any> {
        return mapOf(
            "isRunning" to isRunning,
            "syncingUploaders" to syncingUploaders.toList(),
            "checkIntervalMinutes" to CHECK_INTERVAL_MINUTES
        )
    }

    /**
     * 检查UP主是否正在同步
     */
    fun isSyncing(uploaderUid: Long): Boolean {
        return syncingUploaders.contains(uploaderUid)
    }
}