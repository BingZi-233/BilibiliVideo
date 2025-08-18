package online.bingzi.bilibili.video.internal.database.dao

import com.j256.ormlite.dao.Dao
import online.bingzi.bilibili.video.internal.database.entity.VideoRewardRecord
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * 视频奖励记录DAO服务
 * 处理视频奖励记录的数据库操作
 */
object VideoRewardRecordDaoService {

    private val dao: Dao<VideoRewardRecord, Long> get() = DatabaseDaoManager.videoRewardRecordDao

    /**
     * 保存或更新奖励记录
     */
    fun saveRecord(record: VideoRewardRecord): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                dao.createOrUpdate(record)
                true
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordSaveError", record.playerUuid, record.bvId, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 检查玩家是否已经领取过指定视频的奖励
     */
    fun hasClaimedReward(playerUuid: String, bvId: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val record = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("bv_id", bvId)
                    .queryForFirst()
                record != null
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取玩家指定日期的奖励记录
     */
    fun getPlayerDailyRecords(playerUuid: String, date: String): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .query()
                    .filter { it.isClaimedOnDate(date) }
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取玩家所有奖励记录
     */
    fun getPlayerRecords(playerUuid: String, limit: Int = 50): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .query()
                    .sortedByDescending { it.rewardClaimedAt }
                    .take(limit)
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取UP主视频的奖励记录
     */
    fun getVideoRecords(bvId: String): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("bv_id", bvId)
                    .query()
                    .sortedByDescending { it.rewardClaimedAt }
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取UP主的所有奖励记录
     */
    fun getUploaderRewardRecords(uploaderUid: Long, limit: Int = 100): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("uploader_uid", uploaderUid)
                    .query()
                    .sortedByDescending { it.rewardClaimedAt }
                    .take(limit)
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 统计玩家指定日期的奖励次数
     */
    fun countPlayerDailyRewards(playerUuid: String, date: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .query()
                    .count { it.isClaimedOnDate(date) }
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordCountError", e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 统计玩家总奖励次数
     */
    fun countPlayerTotalRewards(playerUuid: String): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .countOf()
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordCountError", e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 统计视频奖励总次数
     */
    fun countVideoTotalRewards(bvId: String): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("bv_id", bvId)
                    .countOf()
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordCountError", e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 删除玩家的所有奖励记录
     */
    fun deletePlayerRecords(playerUuid: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val deleteBuilder = dao.deleteBuilder()
                deleteBuilder.where().eq("player_uuid", playerUuid)
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordDeleteError", playerUuid, e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 删除过期的奖励记录
     */
    fun deleteExpiredRecords(expireDays: Int): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val expireTime = System.currentTimeMillis() - (expireDays * 24 * 60 * 60 * 1000L)
                val deleteBuilder = dao.deleteBuilder()
                deleteBuilder.where().lt("created_at", expireTime)
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordDeleteError", "expired records", e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 获取最近的奖励记录
     */
    fun getRecentRecords(limit: Int = 20): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryForAll()
                    .sortedByDescending { it.rewardClaimedAt }
                    .take(limit)
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 搜索奖励记录
     */
    fun searchRecords(keyword: String): CompletableFuture<List<VideoRewardRecord>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .like("video_title", "%$keyword%")
                    .or()
                    .like("bv_id", "%$keyword%")
                    .or()
                    .like("player_uuid", "%$keyword%")
                    .query()
                    .sortedByDescending { it.rewardClaimedAt }
            } catch (e: SQLException) {
                console().sendWarn("videoRewardRecordSearchError", keyword, e.message ?: "Unknown error")
                emptyList()
            }
        }
    }
}