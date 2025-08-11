package online.bingzi.bilibili.video.internal.database.dao

import online.bingzi.bilibili.video.api.event.database.binding.BilibiliBindingCreateEvent
import online.bingzi.bilibili.video.api.event.database.binding.BilibiliBindingDeleteEvent
import online.bingzi.bilibili.video.internal.database.entity.BilibiliBinding
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Bilibili绑定DAO服务
 * 处理Bilibili账号绑定的数据库操作
 */
object BilibiliBindingDaoService {

    private val dao get() = DatabaseDaoManager.bilibiliBindingDao

    /**
     * 根据玩家UUID获取Bilibili绑定
     */
    fun getBilibiliBindingByPlayer(playerUuid: UUID): CompletableFuture<BilibiliBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "Bilibili:$playerUuid", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 根据Bilibili UID获取绑定信息
     */
    fun getBilibiliBindingByUid(bilibiliUid: Long): CompletableFuture<BilibiliBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("bilibili_uid", bilibiliUid)
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "Bilibili:$bilibiliUid", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 保存Bilibili绑定
     */
    fun saveBilibiliBinding(bilibiliBinding: BilibiliBinding): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val result = dao.createOrUpdate(bilibiliBinding)

                // 记录操作结果
                if (result.isCreated) {
                    console().sendInfo("playerBindingCreated")
                    
                    // 触发创建事件
                    val createEvent = BilibiliBindingCreateEvent(
                        playerUuid = bilibiliBinding.playerUuid,
                        bilibiliUid = bilibiliBinding.bilibiliUid,
                        success = true
                    )
                    createEvent.call()
                } else {
                    console().sendInfo("playerBindingUpdated")
                }

                true
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", bilibiliBinding.bilibiliUid.toString(), e.message ?: "Unknown error")
                
                // 触发失败事件
                val failEvent = BilibiliBindingCreateEvent(
                    playerUuid = bilibiliBinding.playerUuid,
                    bilibiliUid = bilibiliBinding.bilibiliUid,
                    success = false,
                    errorMessage = e.message
                )
                failEvent.call()
                
                false
            }
        }
    }

    /**
     * 删除Bilibili绑定
     */
    fun deleteBilibiliBinding(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val binding = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .queryForFirst()

                if (binding != null) {
                    binding.updateActiveStatus(false)
                    dao.update(binding)
                    
                    // 触发删除事件
                    val deleteEvent = BilibiliBindingDeleteEvent(
                        playerUuid = playerUuid.toString(),
                        bilibiliUid = binding.bilibiliUid,
                        success = true
                    )
                    deleteEvent.call()
                    
                    true
                } else {
                    false
                }
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseDeleteError", playerUuid, e.message ?: "Unknown error")
                
                // 触发失败事件
                val failEvent = BilibiliBindingDeleteEvent(
                    playerUuid = playerUuid.toString(),
                    bilibiliUid = 0L,
                    success = false,
                    errorMessage = e.message
                )
                failEvent.call()
                
                false
            }
        }
    }

    /**
     * 获取所有活跃的Bilibili绑定
     */
    fun getAllActiveBilibiliBindings(): CompletableFuture<List<BilibiliBinding>> {
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
     * 获取过期的Bilibili绑定
     */
    fun getExpiredBindings(expireDays: Int): CompletableFuture<List<BilibiliBinding>> {
        return CompletableFuture.supplyAsync {
            try {
                val thresholdTime = System.currentTimeMillis() - (expireDays * 24 * 60 * 60 * 1000L)

                dao.queryBuilder()
                    .where()
                    .eq("is_active", true)
                    .and()
                    .lt("last_login_time", thresholdTime)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseGetAllError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 更新绑定的登录时间
     */
    fun updateLastLoginTime(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val binding = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()

                if (binding != null) {
                    binding.updateLastLoginTime()
                    dao.update(binding)
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