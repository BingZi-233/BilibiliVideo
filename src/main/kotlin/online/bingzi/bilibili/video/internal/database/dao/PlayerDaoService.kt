package online.bingzi.bilibili.video.internal.database.dao

import online.bingzi.bilibili.video.internal.database.entity.Player
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 玩家DAO服务
 * 处理玩家基础信息的数据库操作
 */
object PlayerDaoService {

    private val dao get() = DatabaseDaoManager.playerDao

    /**
     * 根据UUID获取玩家信息
     */
    fun getPlayer(playerUuid: UUID): CompletableFuture<Player?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryForId(playerUuid.toString())
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", playerUuid.toString(), e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 保存或更新玩家信息
     */
    fun savePlayer(player: Player): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                dao.createOrUpdate(player)
                true
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", player.playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 删除玩家（逻辑删除）
     */
    fun deletePlayer(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val player = dao.queryForId(playerUuid.toString())
                if (player != null) {
                    player.updateActiveStatus(false)
                    dao.update(player)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseDeleteError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取所有活跃玩家
     */
    fun getAllActivePlayers(): CompletableFuture<List<Player>> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("is_active", true)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseGetAllError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 更新玩家最后活跃时间
     */
    fun updateLastActiveTime(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val player = dao.queryForId(playerUuid.toString())
                if (player != null) {
                    player.updateLastActiveTime()
                    dao.update(player)
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", playerUuid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }
}