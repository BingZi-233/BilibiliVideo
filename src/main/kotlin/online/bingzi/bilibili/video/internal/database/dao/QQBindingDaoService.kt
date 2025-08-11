package online.bingzi.bilibili.video.internal.database.dao

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
                dao.createOrUpdate(qqBinding)
                true
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", qqBinding.qqNumber, e.message ?: "Unknown error")
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
}