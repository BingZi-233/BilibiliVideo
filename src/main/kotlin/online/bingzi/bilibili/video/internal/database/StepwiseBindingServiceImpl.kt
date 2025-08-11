package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliCookieDaoService
import online.bingzi.bilibili.video.internal.database.dao.PlayerDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.*
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 分步绑定服务实现
 */
object StepwiseBindingServiceImpl : StepwiseBindingService {

    override fun createPlayer(playerUuid: UUID, playerName: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val player = Player(playerUuid, playerName)
                PlayerDaoService.savePlayer(player).get()
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun bindQQ(playerUuid: UUID, qqNumber: String, qqNickname: String?): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 检查QQ号是否已被其他用户绑定
                val existingBinding = QQBindingDaoService.getQQBindingByNumber(qqNumber).get()
                if (existingBinding != null && existingBinding.playerUuid != playerUuid.toString()) {
                    return@supplyAsync false // QQ号已被其他用户绑定
                }

                // 创建或更新QQ绑定
                val qqBinding = QQBinding(playerUuid.toString(), qqNumber).apply {
                    this.qqNickname = qqNickname
                }

                val success = QQBindingDaoService.saveQQBinding(qqBinding).get()

                if (success) {
                    // 更新玩家活跃时间
                    PlayerDaoService.updateLastActiveTime(playerUuid)
                    // 清除缓存
                    clearPlayerCache(playerUuid)
                }

                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun bindBilibili(
        playerUuid: UUID,
        bilibiliUid: Long,
        username: String?,
        nickname: String?,
        avatarUrl: String?,
        level: Int?
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 检查Bilibili UID是否已被其他用户绑定
                val existingBinding = BilibiliBindingDaoService.getBilibiliBindingByUid(bilibiliUid).get()
                if (existingBinding != null && existingBinding.playerUuid != playerUuid.toString()) {
                    return@supplyAsync false // Bilibili UID已被其他用户绑定
                }

                // 创建或更新Bilibili绑定
                val bilibiliBinding = BilibiliBinding(playerUuid.toString(), bilibiliUid).apply {
                    this.bilibiliUsername = username
                    this.bilibiliNickname = nickname
                    this.avatarUrl = avatarUrl
                    this.userLevel = level
                }

                val success = BilibiliBindingDaoService.saveBilibiliBinding(bilibiliBinding).get()

                if (success) {
                    // 更新玩家活跃时间
                    PlayerDaoService.updateLastActiveTime(playerUuid)
                    // 清除缓存
                    clearPlayerCache(playerUuid)
                }

                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun saveCookie(playerUuid: UUID, cookieString: String): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 必须先有Bilibili绑定才能保存Cookie
                val bilibiliBinding = BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).get()
                    ?: return@supplyAsync false

                // 获取现有Cookie或创建新的
                val existingCookie = BilibiliCookieDaoService.getCookieByPlayer(playerUuid).get()

                val cookie = if (existingCookie != null) {
                    existingCookie.apply {
                        fromCookieString(cookieString)
                        setCookieStatus(BilibiliCookie.Status.VALID)
                    }
                } else {
                    BilibiliCookie(bilibiliBinding.id, playerUuid.toString()).apply {
                        fromCookieString(cookieString)
                    }
                }

                val success = BilibiliCookieDaoService.saveCookie(cookie).get()

                if (success) {
                    // 更新Bilibili绑定的登录时间
                    BilibiliBindingDaoService.updateLastLoginTime(playerUuid)
                    // 清除缓存
                    clearPlayerCache(playerUuid)
                }

                success
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun getBindingProgress(playerUuid: UUID): CompletableFuture<BindingProgress?> {
        return CompletableFuture.supplyAsync {
            try {
                // 并发查询所有相关数据
                val playerFuture = PlayerDaoService.getPlayer(playerUuid)
                val qqFuture = QQBindingDaoService.getQQBindingByPlayer(playerUuid)
                val bilibiliFuture = BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid)
                val cookieFuture = BilibiliCookieDaoService.getCookieByPlayer(playerUuid)

                val player = playerFuture.get()
                val qqBinding = qqFuture.get()
                val bilibiliBinding = bilibiliFuture.get()
                val bilibiliCookie = cookieFuture.get()

                if (player != null && player.isActive) {
                    BindingProgress(player, qqBinding, bilibiliBinding, bilibiliCookie)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun unbindQQ(playerUuid: UUID): CompletableFuture<Boolean> {
        return QQBindingDaoService.deleteQQBinding(playerUuid).also {
            clearPlayerCache(playerUuid)
        }
    }

    override fun unbindBilibili(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 同时删除Bilibili绑定和Cookie
                val bilibiliSuccess = BilibiliBindingDaoService.deleteBilibiliBinding(playerUuid).get()
                BilibiliCookieDaoService.deleteCookie(playerUuid).get()

                clearPlayerCache(playerUuid)
                bilibiliSuccess
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun deletePlayer(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 删除所有相关数据
                val playerSuccess = PlayerDaoService.deletePlayer(playerUuid).get()
                QQBindingDaoService.deleteQQBinding(playerUuid).get()
                BilibiliBindingDaoService.deleteBilibiliBinding(playerUuid).get()
                BilibiliCookieDaoService.deleteCookie(playerUuid).get()

                clearPlayerCache(playerUuid)
                playerSuccess
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun findPlayerByQQ(qqNumber: String): CompletableFuture<BindingProgress?> {
        return CompletableFuture.supplyAsync {
            try {
                val qqBinding = QQBindingDaoService.getQQBindingByNumber(qqNumber).get()
                if (qqBinding != null) {
                    val playerUuid = UUID.fromString(qqBinding.playerUuid)
                    getBindingProgress(playerUuid).get()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun findPlayerByBilibiliUid(bilibiliUid: Long): CompletableFuture<BindingProgress?> {
        return CompletableFuture.supplyAsync {
            try {
                val bilibiliBinding = BilibiliBindingDaoService.getBilibiliBindingByUid(bilibiliUid).get()
                if (bilibiliBinding != null) {
                    val playerUuid = UUID.fromString(bilibiliBinding.playerUuid)
                    getBindingProgress(playerUuid).get()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun getValidCookie(playerUuid: UUID): CompletableFuture<String?> {
        return CompletableFuture.supplyAsync {
            try {
                val cookie = BilibiliCookieDaoService.getCookieByPlayer(playerUuid).get()
                if (cookie?.isValidCookie() == true) {
                    cookie.toCookieString()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun markCookieUsed(playerUuid: UUID): CompletableFuture<Boolean> {
        return BilibiliCookieDaoService.updateLastUsedTime(playerUuid)
    }

    override fun clearPlayerCache(playerUuid: UUID) {
        MultiTableDatabaseService.removeCacheBinding(playerUuid)
    }

    override fun clearAllCache() {
        MultiTableDatabaseService.clearAllCache()
    }

    override fun getCacheSize(): Int {
        return MultiTableDatabaseService.getCacheSize()
    }
}