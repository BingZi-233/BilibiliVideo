package online.bingzi.bilibili.video.internal.database.dao

import online.bingzi.bilibili.video.api.event.database.binding.QQBindingCreateEvent
import online.bingzi.bilibili.video.api.event.database.binding.QQBindingDeleteEvent
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * QQ绑定DAO服务
 * 处理QQ绑定信息的数据库操作
 */
object QQBindingDaoService {

    private val dao get() = DatabaseDaoManager.qqBindingDao

    /**
     * 根据玩家UUID获取QQ绑定
     */
    fun getQQBindingByPlayer(playerUuid: UUID): CompletableFuture<QQBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "QQ:$playerUuid", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 根据QQ号获取绑定信息
     */
    fun getQQBindingByNumber(qqNumber: String): CompletableFuture<QQBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("qq_number", qqNumber)
                    .and()
                    .eq("is_active", true)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "QQ:$qqNumber", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 保存QQ绑定
     */
    fun saveQQBinding(qqBinding: QQBinding): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val result = dao.createOrUpdate(qqBinding)
                val success = result != null
                
                // 触发创建事件
                if (success && result.isCreated) {
                    val createEvent = QQBindingCreateEvent(
                        playerUuid = qqBinding.playerUuid,
                        qqNumber = qqBinding.qqNumber,
                        success = true
                    )
                    createEvent.call()
                }
                
                success
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", qqBinding.qqNumber, e.message ?: "Unknown error")
                
                // 触发失败事件
                val failEvent = QQBindingCreateEvent(
                    playerUuid = qqBinding.playerUuid,
                    qqNumber = qqBinding.qqNumber,
                    success = false,
                    errorMessage = e.message
                )
                failEvent.call()
                
                false
            }
        }
    }

    /**
     * 删除QQ绑定
     */
    fun deleteQQBinding(playerUuid: UUID): CompletableFuture<Boolean> {
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
                    val deleteEvent = QQBindingDeleteEvent(
                        playerUuid = playerUuid.toString(),
                        qqNumber = binding.qqNumber,
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
                val failEvent = QQBindingDeleteEvent(
                    playerUuid = playerUuid.toString(),
                    qqNumber = "",
                    success = false,
                    errorMessage = e.message
                )
                failEvent.call()
                
                false
            }
        }
    }
}