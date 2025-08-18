package online.bingzi.bilibili.video.internal.database.dao

import com.j256.ormlite.dao.Dao
import online.bingzi.bilibili.video.internal.database.entity.PlayerRewardStats
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.concurrent.CompletableFuture

/**
 * 玩家奖励统计DAO服务
 * 处理玩家奖励统计的数据库操作
 */
object PlayerRewardStatsDaoService {

    private val dao: Dao<PlayerRewardStats, Long> get() = DatabaseDaoManager.playerRewardStatsDao

    /**
     * 保存或更新玩家奖励统计
     */
    fun saveStats(stats: PlayerRewardStats): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                stats.updateTimestamp()
                dao.createOrUpdate(stats)
                true
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsSaveError", stats.playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取玩家指定日期的奖励统计
     */
    fun getPlayerDailyStats(playerUuid: String, date: String): CompletableFuture<PlayerRewardStats?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("reward_date", date)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", playerUuid, e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 获取或创建玩家今日奖励统计
     */
    fun getOrCreateTodayStats(playerUuid: String): CompletableFuture<PlayerRewardStats> {
        return CompletableFuture.supplyAsync {
            try {
                val today = PlayerRewardStats.getTodayDateString()
                var stats = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("reward_date", today)
                    .queryForFirst()
                
                if (stats == null) {
                    stats = PlayerRewardStats.createTodayStats(playerUuid)
                    dao.create(stats)
                }
                
                stats
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsCreateError", playerUuid, e.message ?: "Unknown error")
                PlayerRewardStats.createTodayStats(playerUuid)
            }
        }
    }

    /**
     * 增加玩家奖励次数
     */
    fun incrementPlayerReward(playerUuid: String, rewardTime: Long = System.currentTimeMillis()): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val stats = getOrCreateTodayStats(playerUuid).get()
                stats.incrementReward(rewardTime)
                dao.update(stats)
                true
            } catch (e: Exception) {
                console().sendWarn("playerRewardStatsIncrementError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 检查玩家今日是否达到奖励限制
     */
    fun hasReachedDailyLimit(playerUuid: String, dailyLimit: Int): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val today = PlayerRewardStats.getTodayDateString()
                val stats = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("reward_date", today)
                    .queryForFirst()
                
                stats?.hasReachedDailyLimit(dailyLimit) ?: false
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsCheckError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取玩家今日剩余奖励次数
     */
    fun getRemainingRewards(playerUuid: String, dailyLimit: Int): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val today = PlayerRewardStats.getTodayDateString()
                val stats = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("reward_date", today)
                    .queryForFirst()
                
                stats?.getRemainingRewards(dailyLimit) ?: dailyLimit
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", playerUuid, e.message ?: "Unknown error")
                dailyLimit
            }
        }
    }

    /**
     * 获取玩家所有奖励统计记录
     */
    fun getPlayerAllStats(playerUuid: String, limit: Int = 30): CompletableFuture<List<PlayerRewardStats>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .query()
                    .sortedByDescending { it.rewardDate }
                    .take(limit)
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", playerUuid, e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 获取玩家总奖励次数
     */
    fun getPlayerTotalRewards(playerUuid: String): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                val latestStats = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .query()
                    .maxByOrNull { it.updatedAt }
                
                latestStats?.totalRewardCount ?: 0L
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", playerUuid, e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 删除玩家的所有奖励统计
     */
    fun deletePlayerStats(playerUuid: String): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val deleteBuilder = dao.deleteBuilder()
                deleteBuilder.where().eq("player_uuid", playerUuid)
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsDeleteError", playerUuid, e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 删除过期的统计记录
     */
    fun deleteExpiredStats(expireDays: Int): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val expireDate = java.time.LocalDate.now()
                    .minusDays(expireDays.toLong())
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                
                val deleteBuilder = dao.deleteBuilder()
                deleteBuilder.where().lt("reward_date", expireDate)
                deleteBuilder.delete()
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsDeleteError", "expired stats", e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 获取指定日期的所有统计记录
     */
    fun getDailyStats(date: String): CompletableFuture<List<PlayerRewardStats>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("reward_date", date)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 统计活跃玩家数量
     */
    fun countActivePlayers(date: String): CompletableFuture<Long> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("reward_date", date)
                    .and()
                    .gt("daily_reward_count", 0)
                    .countOf()
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsCountError", e.message ?: "Unknown error")
                0L
            }
        }
    }

    /**
     * 获取奖励排行榜（按总奖励次数）
     */
    fun getTopRewardPlayers(limit: Int = 10): CompletableFuture<List<PlayerRewardStats>> {
        return CompletableFuture.supplyAsync {
            try {
                // 获取每个玩家的最新统计记录
                dao.queryForAll()
                    .groupBy { it.playerUuid }
                    .mapValues { (_, stats) -> stats.maxByOrNull { it.updatedAt } }
                    .values
                    .filterNotNull()
                    .sortedByDescending { it.totalRewardCount }
                    .take(limit)
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsQueryError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 重置玩家当日统计（用于跨日处理）
     */
    fun resetPlayerDailyStats(playerUuid: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val today = PlayerRewardStats.getTodayDateString()
                val stats = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid)
                    .and()
                    .eq("reward_date", today)
                    .queryForFirst()
                
                if (stats != null) {
                    stats.resetDailyCount()
                    dao.update(stats)
                } else {
                    // 创建新的今日统计
                    val newStats = PlayerRewardStats.createTodayStats(playerUuid)
                    dao.create(newStats)
                }
                true
            } catch (e: SQLException) {
                console().sendWarn("playerRewardStatsResetError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }
}