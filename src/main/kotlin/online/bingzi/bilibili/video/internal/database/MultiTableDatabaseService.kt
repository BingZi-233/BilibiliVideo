package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliCookieDaoService
import online.bingzi.bilibili.video.internal.database.dao.PlayerDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.BilibiliBinding
import online.bingzi.bilibili.video.internal.database.entity.BilibiliCookie
import online.bingzi.bilibili.video.internal.database.entity.Player
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 多表数据库服务
 * 提供统一的数据库操作接口，管理多个相关联的表
 */
object MultiTableDatabaseService {

    /**
     * 内存缓存，提高查询性能
     */
    private val playerCache = ConcurrentHashMap<String, Player>()
    private val qqBindingCache = ConcurrentHashMap<String, QQBinding>()
    private val bilibiliBindingCache = ConcurrentHashMap<String, BilibiliBinding>()
    private val cookieCache = ConcurrentHashMap<String, BilibiliCookie>()

    /**
     * 完整的用户绑定信息
     */
    data class CompleteUserBinding(
        val player: Player,
        val qqBinding: QQBinding?,
        val bilibiliBinding: BilibiliBinding?,
        val bilibiliCookie: BilibiliCookie?
    ) {
        /**
         * 检查是否有完整的绑定信息
         */
        fun hasCompleteBinding(): Boolean {
            return qqBinding?.isValidBinding() == true &&
                    bilibiliBinding?.isValidBinding() == true &&
                    bilibiliCookie?.isValidCookie() == true
        }

        /**
         * 检查是否有QQ绑定
         */
        fun hasQQBinding(): Boolean = qqBinding?.isValidBinding() == true

        /**
         * 检查是否有Bilibili绑定
         */
        fun hasBilibiliBinding(): Boolean = bilibiliBinding?.isValidBinding() == true

        /**
         * 检查是否有有效Cookie
         */
        fun hasValidCookie(): Boolean = bilibiliCookie?.isValidCookie() == true
    }

    /**
     * 获取完整的用户绑定信息
     */
    fun getCompleteUserBinding(playerUuid: UUID): CompletableFuture<CompleteUserBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                val uuidString = playerUuid.toString()

                // 尝试从缓存获取
                val cachedPlayer = playerCache[uuidString]
                val cachedQQ = qqBindingCache[uuidString]
                val cachedBilibili = bilibiliBindingCache[uuidString]
                val cachedCookie = cookieCache[uuidString]

                // 如果缓存中有完整信息，直接返回
                if (cachedPlayer != null && cachedQQ != null && cachedBilibili != null && cachedCookie != null) {
                    return@supplyAsync CompleteUserBinding(cachedPlayer, cachedQQ, cachedBilibili, cachedCookie)
                }

                // 并发查询所有相关数据
                val playerFuture = PlayerDaoService.getPlayer(playerUuid)
                val qqFuture = QQBindingDaoService.getQQBindingByPlayer(playerUuid)
                val bilibiliFuture = BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid)
                val cookieFuture = BilibiliCookieDaoService.getCookieByPlayer(playerUuid)

                val player = playerFuture.get()
                val qqBinding = qqFuture.get()
                val bilibiliBinding = bilibiliFuture.get()
                val bilibiliCookie = cookieFuture.get()

                // 如果玩家不存在，返回null
                if (player == null) {
                    return@supplyAsync null
                }

                // 更新缓存
                playerCache[uuidString] = player
                qqBinding?.let { qqBindingCache[uuidString] = it }
                bilibiliBinding?.let { bilibiliBindingCache[uuidString] = it }
                bilibiliCookie?.let { cookieCache[uuidString] = it }

                CompleteUserBinding(player, qqBinding, bilibiliBinding, bilibiliCookie)
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseGetAllError", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 创建新的用户绑定
     */
    fun createUserBinding(
        playerUuid: UUID,
        playerName: String,
        qqNumber: String? = null,
        bilibiliUid: Long? = null,
        cookieString: String? = null
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val uuidString = playerUuid.toString()

                // 创建玩家记录
                val player = Player(playerUuid, playerName)
                val playerSuccess = PlayerDaoService.savePlayer(player).get()

                if (!playerSuccess) {
                    return@supplyAsync false
                }

                // 创建QQ绑定（如果提供了QQ号）
                var qqBindingSuccess = true
                if (qqNumber != null) {
                    val qqBinding = QQBinding(uuidString, qqNumber)
                    qqBindingSuccess = QQBindingDaoService.saveQQBinding(qqBinding).get()
                }

                // 创建Bilibili绑定（如果提供了UID）
                var bilibiliBindingSuccess = true
                var bilibiliBinding: BilibiliBinding? = null
                if (bilibiliUid != null) {
                    bilibiliBinding = BilibiliBinding(uuidString, bilibiliUid)
                    bilibiliBindingSuccess = BilibiliBindingDaoService.saveBilibiliBinding(bilibiliBinding).get()
                }

                // 创建Cookie记录（如果提供了Cookie且有Bilibili绑定）
                var cookieSuccess = true
                if (cookieString != null && bilibiliBinding != null) {
                    val cookie = BilibiliCookie(bilibiliBinding.id, uuidString)
                    cookie.fromCookieString(cookieString)
                    cookieSuccess = BilibiliCookieDaoService.saveCookie(cookie).get()
                }

                // 清除缓存
                removeCacheBinding(playerUuid)

                playerSuccess && qqBindingSuccess && bilibiliBindingSuccess && cookieSuccess
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseSaveError", playerUuid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 更新QQ绑定
     */
    fun updateQQBinding(playerUuid: UUID, qqNumber: String, qqNickname: String? = null): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val uuidString = playerUuid.toString()

                // 检查是否已存在QQ绑定
                val existingBinding = QQBindingDaoService.getQQBindingByPlayer(playerUuid).get()

                val qqBinding = if (existingBinding != null) {
                    // 更新现有绑定
                    existingBinding.apply {
                        this.qqNumber = qqNumber
                        this.qqNickname = qqNickname
                        updateLastVerifyTime()
                    }
                } else {
                    // 创建新绑定
                    QQBinding(uuidString, qqNumber).apply {
                        this.qqNickname = qqNickname
                    }
                }

                val success = QQBindingDaoService.saveQQBinding(qqBinding).get()

                // 更新玩家活跃时间
                PlayerDaoService.updateLastActiveTime(playerUuid)

                // 清除缓存
                removeCacheBinding(playerUuid)

                success
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseSaveError", playerUuid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 更新Bilibili绑定和Cookie
     */
    fun updateBilibiliBinding(
        playerUuid: UUID,
        bilibiliUid: Long,
        username: String? = null,
        nickname: String? = null,
        avatarUrl: String? = null,
        level: Int? = null,
        cookieString: String? = null
    ): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                val uuidString = playerUuid.toString()

                // 检查是否已存在Bilibili绑定
                val existingBinding = BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid).get()

                val bilibiliBinding = if (existingBinding != null) {
                    // 更新现有绑定
                    existingBinding.apply {
                        this.bilibiliUid = bilibiliUid
                        updateUserInfo(username, nickname, avatarUrl, level)
                        updateLastLoginTime()
                    }
                } else {
                    // 创建新绑定
                    BilibiliBinding(uuidString, bilibiliUid).apply {
                        updateUserInfo(username, nickname, avatarUrl, level)
                    }
                }

                val bindingSuccess = BilibiliBindingDaoService.saveBilibiliBinding(bilibiliBinding).get()

                // 更新Cookie（如果提供）
                var cookieSuccess = true
                if (cookieString != null && bindingSuccess) {
                    val existingCookie = BilibiliCookieDaoService.getCookieByPlayer(playerUuid).get()

                    val cookie = if (existingCookie != null) {
                        // 更新现有Cookie
                        existingCookie.apply {
                            fromCookieString(cookieString)
                            setCookieStatus(BilibiliCookie.Status.VALID)
                        }
                    } else {
                        // 创建新Cookie
                        BilibiliCookie(bilibiliBinding.id, uuidString).apply {
                            fromCookieString(cookieString)
                        }
                    }

                    cookieSuccess = BilibiliCookieDaoService.saveCookie(cookie).get()
                }

                // 更新玩家活跃时间
                PlayerDaoService.updateLastActiveTime(playerUuid)

                // 清除缓存
                removeCacheBinding(playerUuid)

                bindingSuccess && cookieSuccess
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseSaveError", playerUuid.toString(), e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 根据QQ号查询用户绑定信息
     */
    fun getUserBindingByQQ(qqNumber: String): CompletableFuture<CompleteUserBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                val qqBinding = QQBindingDaoService.getQQBindingByNumber(qqNumber).get()
                if (qqBinding != null) {
                    val playerUuid = UUID.fromString(qqBinding.playerUuid)
                    getCompleteUserBinding(playerUuid).get()
                } else {
                    null
                }
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "QQ:$qqNumber", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 根据Bilibili UID查询用户绑定信息
     */
    fun getUserBindingByBilibiliUid(bilibiliUid: Long): CompletableFuture<CompleteUserBinding?> {
        return CompletableFuture.supplyAsync {
            try {
                val bilibiliBinding = BilibiliBindingDaoService.getBilibiliBindingByUid(bilibiliUid).get()
                if (bilibiliBinding != null) {
                    val playerUuid = UUID.fromString(bilibiliBinding.playerUuid)
                    getCompleteUserBinding(playerUuid).get()
                } else {
                    null
                }
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseSaveError", "Bilibili:$bilibiliUid", e.message ?: "Unknown error")
                null
            }
        }
    }

    /**
     * 删除用户绑定（逻辑删除）
     */
    fun deleteUserBinding(playerUuid: UUID): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            try {
                // 并发删除所有相关数据
                val playerFuture = PlayerDaoService.deletePlayer(playerUuid)
                val qqFuture = QQBindingDaoService.deleteQQBinding(playerUuid)
                val bilibiliBindingFuture = BilibiliBindingDaoService.deleteBilibiliBinding(playerUuid)
                val cookieFuture = BilibiliCookieDaoService.deleteCookie(playerUuid)

                val playerSuccess = playerFuture.get()
                qqFuture.get()
                bilibiliBindingFuture.get()
                cookieFuture.get()

                // 清除缓存
                removeCacheBinding(playerUuid)

                console().sendInfo("playerBindingDeleted")

                // 只要主要数据删除成功就算成功
                playerSuccess
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseDeleteError", playerUuid, e.message ?: "Unknown error")
                false
            }
        }
    }

    /**
     * 清理过期数据
     */
    fun cleanupExpiredData(bindingExpireDays: Int = 30, cookieExpireDays: Int = 7): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync {
            try {
                var cleanedCount = 0

                // 清理过期的Bilibili绑定
                val expiredBindings = BilibiliBindingDaoService.getExpiredBindings(bindingExpireDays).get()
                expiredBindings.forEach { binding ->
                    binding.updateActiveStatus(false)
                    BilibiliBindingDaoService.saveBilibiliBinding(binding)
                    cleanedCount++
                }

                // 清理过期的Cookie
                val expiredCookies = BilibiliCookieDaoService.getExpiredCookies(cookieExpireDays).get()
                expiredCookies.forEach { cookie ->
                    cookie.updateActiveStatus(false)
                    BilibiliCookieDaoService.saveCookie(cookie)
                    cleanedCount++
                }

                // 清理无效的Cookie
                val invalidCookiesCount = BilibiliCookieDaoService.cleanupInvalidCookies().get()
                cleanedCount += invalidCookiesCount

                if (cleanedCount > 0) {
                    console().sendInfo("playerBilibiliDatabaseCleanupComplete", cleanedCount)
                    // 清空缓存
                    clearAllCache()
                }

                cleanedCount
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseCleanupError", e.message ?: "Unknown error")
                0
            }
        }
    }

    /**
     * 从缓存中移除指定玩家的绑定信息
     */
    fun removeCacheBinding(playerUuid: UUID) {
        val uuidString = playerUuid.toString()
        playerCache.remove(uuidString)
        qqBindingCache.remove(uuidString)
        bilibiliBindingCache.remove(uuidString)
        cookieCache.remove(uuidString)
    }

    /**
     * 获取所有缓存大小
     */
    fun getCacheSize(): Int {
        return playerCache.size + qqBindingCache.size + bilibiliBindingCache.size + cookieCache.size
    }

    /**
     * 清空所有缓存
     */
    fun clearAllCache() {
        playerCache.clear()
        qqBindingCache.clear()
        bilibiliBindingCache.clear()
        cookieCache.clear()
    }

    /**
     * 获取所有有完整绑定的用户
     */
    fun getAllCompleteBindings(): CompletableFuture<List<CompleteUserBinding>> {
        return CompletableFuture.supplyAsync {
            try {
                val activePlayers = PlayerDaoService.getAllActivePlayers().get()
                val completeBindings = mutableListOf<CompleteUserBinding>()

                activePlayers.forEach { player ->
                    val playerUuid = player.getPlayerUuidAsUuid()
                    val binding = getCompleteUserBinding(playerUuid).get()

                    if (binding?.hasCompleteBinding() == true) {
                        completeBindings.add(binding)
                    }
                }

                completeBindings
            } catch (e: Exception) {
                console().sendWarn("playerBilibiliDatabaseGetAllError", e.message ?: "Unknown error")
                emptyList()
            }
        }
    }
}