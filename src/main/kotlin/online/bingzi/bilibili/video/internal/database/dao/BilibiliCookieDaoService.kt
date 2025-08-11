package online.bingzi.bilibili.video.internal.database.dao

import online.bingzi.bilibili.video.internal.database.entity.BilibiliCookie
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.sql.SQLException
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Bilibili Cookie DAO服务
 * 处理Bilibili登录Cookie的数据库操作
 */
object BilibiliCookieDaoService {

    private val dao get() = DatabaseDaoManager.bilibiliCookieDao

    /**
     * 根据玩家UUID获取Cookie信息
     */
    fun getCookieByPlayer(playerUuid: UUID): CompletableFuture<BilibiliCookie?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .and()
                    .eq("is_active", true)
                    .queryBuilder()
                    .orderBy("update_time", false) // 按更新时间倒序，获取最新的
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "Cookie:$playerUuid", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 根据Bilibili绑定ID获取Cookie信息
     */
    fun getCookieByBindingId(bindingId: Long): CompletableFuture<BilibiliCookie?> {
        return CompletableFuture.supplyAsync {
            try {
                dao.queryBuilder()
                    .where()
                    .eq("bilibili_binding_id", bindingId)
                    .and()
                    .eq("is_active", true)
                    .queryBuilder()
                    .orderBy("update_time", false)
                    .queryForFirst()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "Cookie:$bindingId", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 保存Cookie信息
     */
    fun saveCookie(cookie: BilibiliCookie): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                dao.createOrUpdate(cookie)
                true
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseSaveError", cookie.playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 删除Cookie信息
     */
    fun deleteCookie(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val cookies = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .query()

                cookies.forEach { cookie ->
                    cookie.updateActiveStatus(false)
                    dao.update(cookie)
                }

                cookies.isNotEmpty()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseDeleteError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 获取过期的Cookie
     */
    fun getExpiredCookies(expireDays: Int): CompletableFuture<List<BilibiliCookie>> {
        return CompletableFuture.supplyAsync {
            try {
                val thresholdTime = System.currentTimeMillis() - (expireDays * 24 * 60 * 60 * 1000L)

                dao.queryBuilder()
                    .where()
                    .eq("is_active", true)
                    .and()
                    .lt("last_used_time", thresholdTime)
                    .query()
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseGetAllError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }

    /**
     * 更新Cookie使用时间
     */
    fun updateLastUsedTime(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val cookie = dao.queryBuilder()
                    .where()
                    .eq("player_uuid", playerUuid.toString())
                    .and()
                    .eq("is_active", true)
                    .queryBuilder()
                    .orderBy("update_time", false)
                    .queryForFirst()

                if (cookie != null) {
                    cookie.updateLastUsedTime()
                    dao.update(cookie)
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

    /**
     * 清理无效的Cookie
     */
    fun cleanupInvalidCookies(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                val invalidCookies = dao.queryBuilder()
                    .where()
                    .eq("is_active", true)
                    .and()
                    .eq("cookie_status", "INVALID")
                    .query()

                invalidCookies.forEach { cookie ->
                    cookie.updateActiveStatus(false)
                    dao.update(cookie)
                }

                invalidCookies.size
            } catch (e: SQLException) {
                console().sendWarn("playerBilibiliDatabaseCleanupError", e.message ?: "Unknown error")
                0
            }
        }
    }
}