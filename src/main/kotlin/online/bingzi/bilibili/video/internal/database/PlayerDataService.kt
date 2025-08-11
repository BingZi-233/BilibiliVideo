package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.entity.*
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 玩家数据访问服务接口（保持不变，用于高级操作）
 */
interface PlayerDataService {

    /**
     * 玩家基础信息操作
     */
    fun getPlayer(playerUuid: UUID): CompletableFuture<Player?>
    fun savePlayer(player: Player): CompletableFuture<Boolean>
    fun deletePlayer(playerUuid: UUID): CompletableFuture<Boolean>
    fun updateLastActiveTime(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * QQ绑定操作
     */
    fun getQQBinding(playerUuid: UUID): CompletableFuture<QQBinding?>
    fun getQQBindingByNumber(qqNumber: String): CompletableFuture<QQBinding?>
    fun saveQQBinding(qqBinding: QQBinding): CompletableFuture<Boolean>
    fun deleteQQBinding(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * Bilibili绑定操作
     */
    fun getBilibiliBinding(playerUuid: UUID): CompletableFuture<BilibiliBinding?>
    fun getBilibiliBindingByUid(bilibiliUid: Long): CompletableFuture<BilibiliBinding?>
    fun saveBilibiliBinding(bilibiliBinding: BilibiliBinding): CompletableFuture<Boolean>
    fun deleteBilibiliBinding(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * Cookie操作
     */
    fun getCookie(playerUuid: UUID): CompletableFuture<BilibiliCookie?>
    fun getCookieByBindingId(bindingId: Long): CompletableFuture<BilibiliCookie?>
    fun saveCookie(cookie: BilibiliCookie): CompletableFuture<Boolean>
    fun deleteCookie(playerUuid: UUID): CompletableFuture<Boolean>
    fun updateCookieUsedTime(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * 批量操作和维护
     */
    fun getAllActivePlayers(): CompletableFuture<List<Player>>
    fun getAllCompleteBindings(): CompletableFuture<List<BindingProgress>>
    fun cleanupExpiredData(bindingExpireDays: Int = 30, cookieExpireDays: Int = 7): CompletableFuture<Int>
}