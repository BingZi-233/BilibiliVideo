package online.bingzi.bilibili.video.internal.database.dao

import com.j256.ormlite.dao.Dao
import online.bingzi.bilibili.video.internal.database.entity.RewardConfig
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * 奖励配置DAO服务
 * 处理奖励配置的数据库操作
 */
object RewardConfigDaoService {

    private val dao: Dao<RewardConfig, Long> get() = DatabaseDaoManager.rewardConfigDao

    /**
     * 保存或更新奖励配置
     */
    fun saveConfig(config: RewardConfig): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                config.updateTimestamp()
                dao.createOrUpdate(config)
                true
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigSaveError", config.uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 根据UP主UID获取奖励配置
     */
    fun getConfigByUploaderUid(uploaderUid: Long): CompletableFuture<RewardConfig?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigQueryError", uploaderUid.toString(), e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 获取所有启用的奖励配置
     */
    fun getEnabledConfigs(): CompletableFuture<List<RewardConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("is_enabled", true)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取所有奖励配置
     */
    fun getAllConfigs(): CompletableFuture<List<RewardConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryForAll()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 删除奖励配置
     */
    fun deleteConfig(uploaderUid: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                
                if (config != null) {
                    dao.delete(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigDeleteError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 启用或禁用奖励配置
     */
    fun toggleConfigEnabled(uploaderUid: Long, enabled: Boolean): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                
                if (config != null) {
                    config.isEnabled = enabled
                    config.updateTimestamp()
                    dao.update(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigUpdateError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 更新奖励脚本
     */
    fun updateRewardScript(uploaderUid: Long, newScript: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                
                if (config != null) {
                    config.updateRewardScript(newScript)
                    dao.update(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigUpdateError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 更新UP主名称
     */
    fun updateUploaderName(uploaderUid: Long, newName: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                
                if (config != null) {
                    config.updateUploaderName(newName)
                    dao.update(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigUpdateError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 设置视频年龄范围
     */
    fun setVideoAgeRange(uploaderUid: Long, minDays: Int, maxDays: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                
                if (config != null) {
                    config.setVideoAgeRange(minDays, maxDays)
                    dao.update(config)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigUpdateError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 检查UP主是否有奖励配置
     */
    fun hasConfig(uploaderUid: Long): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val config = dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .queryForFirst()
                config != null
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigQueryError", uploaderUid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 批量保存奖励配置
     */
    fun saveConfigs(configs: List<RewardConfig>): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            var savedCount = 0
            configs.forEach { config ->
                try {
                    config.updateTimestamp()
                    dao.createOrUpdate(config)
                    savedCount++
                } catch (e: SQLException) {
                    console().sendWarn("rewardConfigSaveError", config.uploaderUid.toString(), e.message ?: "Unknown error")
                }
            }
            savedCount
        }
    }

    /**
     * 统计奖励配置数量
     */
    fun countConfigs(enabledOnly: Boolean = false): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                val queryBuilder = dao.queryBuilder()
                if (enabledOnly) {
                    queryBuilder.where().eq("is_enabled", true)
                }
                queryBuilder.countOf()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigCountError", e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 搜索奖励配置
     */
    fun searchConfigs(keyword: String): CompletableFuture<List<RewardConfig>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .like("uploader_name", "%$keyword%")
                    .or()
                    .eq("uploader_uid", keyword.toLongOrNull() ?: -1)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigSearchError", keyword, e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 清理所有配置（仅用于测试或重置）
     */
    fun clearAllConfigs(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val deleteBuilder = dao.deleteBuilder()
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("rewardConfigDeleteError", "all configs", e.message ?: "Unknown error")
                0
            }
        }
    }
}