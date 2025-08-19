package online.bingzi.bilibili.video.internal.database.dao

import com.j256.ormlite.dao.Dao
import online.bingzi.bilibili.video.internal.database.entity.UploaderVideo
import online.bingzi.bilibili.video.internal.database.entity.UploaderConfig
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * UP主视频DAO服务
 * 处理UP主视频和配置的数据库操作
 */
object UploaderVideoDaoService {

    private val videoDao: Dao<UploaderVideo, Long> get() = DatabaseDaoManager.uploaderVideoDao
    private val configDao: Dao<UploaderConfig, Long> get() = DatabaseDaoManager.uploaderConfigDao

    // ========== UP主配置操作 ==========

    /**
     * 获取所有UP主配置
     */
    fun getAllConfigs(): CompletableFuture<List<UploaderConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                configDao.queryForAll()
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取启用的UP主配置
     */
    fun getEnabledConfigs(): CompletableFuture<List<UploaderConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                configDao.queryBuilder()
                    .where()
                    .eq("is_enabled", true)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取需要同步的UP主配置
     */
    fun getConfigsNeedSync(): CompletableFuture<List<UploaderConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                val configs = configDao.queryBuilder()
                    .where()
                    .eq("is_enabled", true)
                    .query()
                
                configs.filter { it.needsSync() }
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 保存或更新UP主配置
     */
    fun saveConfig(config: UploaderConfig): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                configDao.createOrUpdate(config)
                true
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigSaveError", config.uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 删除UP主配置
     */
    fun deleteConfig(uploaderUid: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                configDao.deleteById(uploaderUid)
                // 同时删除该UP主的所有视频记录
                deleteVideosByUploader(uploaderUid).get()
                true
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigDeleteError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 更新同步时间
     */
    fun updateSyncTime(uploaderUid: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = configDao.queryForId(uploaderUid)
                if (config != null) {
                    config.updateSyncTime()
                    configDao.update(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("uploaderConfigUpdateError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    // ========== UP主视频操作 ==========

    /**
     * 批量保存视频
     */
    fun saveVideos(videos: List<UploaderVideo>): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            var savedCount = 0
            videos.forEach { video ->
                try {
                    // 检查视频是否已存在
                    val existing = videoDao.queryBuilder()
                        .where()
                        .eq("bv_id", video.bvId)
                        .queryForFirst()
                    
                    if (existing != null) {
                        // 更新现有记录
                        video.id = existing.id
                        video.createdAt = existing.createdAt
                        video.updateTimestamp()
                        videoDao.update(video)
                    } else {
                        // 创建新记录
                        videoDao.create(video)
                    }
                    savedCount++
                } catch (e: SQLException) {
                    console().sendWarn("uploaderVideoSaveError", video.bvId, e.message ?: "Unknown error")
                }
            }
            savedCount
        }
    }

    /**
     * 获取UP主的所有视频
     */
    fun getVideosByUploader(uploaderUid: Long): CompletableFuture<List<UploaderVideo>> {
        return CompletableFuture.supplyAsync {
            try {
                videoDao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .and()
                    .eq("is_active", true)
                    .query()
                    .sortedByDescending { it.publishTime }
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 根据BV号获取视频
     */
    fun getVideoByBvId(bvId: String): CompletableFuture<UploaderVideo?> {
        return CompletableFuture.supplyAsync {
            try {
                videoDao.queryBuilder()
                    .where()
                    .eq("bv_id", bvId)
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoQueryError", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 删除UP主的所有视频
     */
    fun deleteVideosByUploader(uploaderUid: Long): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val deleteBuilder = videoDao.deleteBuilder()
                deleteBuilder.where().eq("uploader_uid", uploaderUid)
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoDeleteError", e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 软删除视频
     */
    fun softDeleteVideo(bvId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val video = videoDao.queryBuilder()
                    .where()
                    .eq("bv_id", bvId)
                    .queryForFirst()
                
                if (video != null) {
                    video.isActive = false
                    video.updateTimestamp()
                    videoDao.update(video)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoDeleteError", bvId, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取视频总数
     */
    fun getVideoCount(uploaderUid: Long? = null): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                val queryBuilder = videoDao.queryBuilder()
                if (uploaderUid != null) {
                    queryBuilder.where()
                        .eq("uploader_uid", uploaderUid)
                        .and()
                        .eq("is_active", true)
                }
                queryBuilder.countOf()
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoCountError", e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 获取热门UP主UID（用于命令建议）
     * 根据视频数量排序获取最活跃的UP主
     * 
     * @param limit 返回数量限制
     * @return UP主UID列表
     */
    fun getPopularUploaderUids(limit: Int): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            try {
                // 获取有视频的UP主UID，按视频数量排序
                val uidCounts = mutableMapOf<Long, Int>()
                
                // 统计每个UP主的视频数量
                val allVideos = videoDao.queryBuilder()
                    .where()
                    .eq("is_active", true)
                    .query()
                
                allVideos.forEach { video ->
                    uidCounts[video.uploaderUid] = uidCounts.getOrDefault(video.uploaderUid, 0) + 1
                }
                
                // 按视频数量排序并返回UID
                uidCounts.toList()
                    .sortedByDescending { it.second }
                    .take(limit)
                    .map { it.first.toString() }
                    
            } catch (e: SQLException) {
                console().sendWarn("commandSuggestionDataError", "Popular Uploader UIDs", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取玩家可以领取奖励的最近视频（用于命令建议）
     * 
     * @param playerUuid 玩家UUID  
     * @param limit 返回数量限制
     * @return 视频列表
     */
    fun getRecentVideosForReward(playerUuid: UUID, limit: Int): CompletableFuture<List<UploaderVideo>> {
        return CompletableFuture.supplyAsync {
            try {
                // 获取最近发布的活跃视频
                videoDao.queryBuilder()
                    .orderBy("publish_time", false) // 按发布时间倒序
                    .where()
                    .eq("is_active", true)
                    .query()
                    .take(limit)
                    
            } catch (e: SQLException) {
                console().sendWarn("commandSuggestionDataError", "Recent Videos for ${playerUuid}", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 搜索视频
     */
    fun searchVideos(keyword: String): CompletableFuture<List<UploaderVideo>> {
        return CompletableFuture.supplyAsync {
            try {
                videoDao.queryBuilder()
                    .where()
                    .like("title", "%$keyword%")
                    .or()
                    .like("description", "%$keyword%")
                    .and()
                    .eq("is_active", true)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("uploaderVideoSearchError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }
}