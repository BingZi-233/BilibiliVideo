package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.dao.BilibiliBindingDaoService
import online.bingzi.bilibili.video.internal.database.dao.BilibiliCookieDaoService
import online.bingzi.bilibili.video.internal.database.dao.PlayerDaoService
import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.*
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 数据访问服务实现（保持原有逻辑）
 */
object PlayerDataServiceImpl : PlayerDataService {

    // 玩家操作
    override fun getPlayer(playerUuid: UUID) = PlayerDaoService.getPlayer(playerUuid)
    override fun savePlayer(player: Player) = PlayerDaoService.savePlayer(player)
    override fun deletePlayer(playerUuid: UUID) = PlayerDaoService.deletePlayer(playerUuid)
    override fun updateLastActiveTime(playerUuid: UUID) = PlayerDaoService.updateLastActiveTime(playerUuid)

    // QQ绑定操作
    override fun getQQBinding(playerUuid: UUID) = QQBindingDaoService.getQQBindingByPlayer(playerUuid)
    override fun getQQBindingByNumber(qqNumber: String) = QQBindingDaoService.getQQBindingByNumber(qqNumber)
    override fun saveQQBinding(qqBinding: QQBinding) = QQBindingDaoService.saveQQBinding(qqBinding)
    override fun deleteQQBinding(playerUuid: UUID) = QQBindingDaoService.deleteQQBinding(playerUuid)

    // Bilibili绑定操作
    override fun getBilibiliBinding(playerUuid: UUID) = BilibiliBindingDaoService.getBilibiliBindingByPlayer(playerUuid)
    override fun getBilibiliBindingByUid(bilibiliUid: Long) = BilibiliBindingDaoService.getBilibiliBindingByUid(bilibiliUid)
    override fun saveBilibiliBinding(bilibiliBinding: BilibiliBinding) = BilibiliBindingDaoService.saveBilibiliBinding(bilibiliBinding)
    override fun deleteBilibiliBinding(playerUuid: UUID) = BilibiliBindingDaoService.deleteBilibiliBinding(playerUuid)

    // Cookie操作
    override fun getCookie(playerUuid: UUID) = BilibiliCookieDaoService.getCookieByPlayer(playerUuid)
    override fun getCookieByBindingId(bindingId: Long) = BilibiliCookieDaoService.getCookieByBindingId(bindingId)
    override fun saveCookie(cookie: BilibiliCookie) = BilibiliCookieDaoService.saveCookie(cookie)
    override fun deleteCookie(playerUuid: UUID) = BilibiliCookieDaoService.deleteCookie(playerUuid)
    override fun updateCookieUsedTime(playerUuid: UUID) = BilibiliCookieDaoService.updateLastUsedTime(playerUuid)

    // 批量操作
    override fun getAllActivePlayers(): CompletableFuture<List<Player>> {
        return PlayerDaoService.getAllActivePlayers()
    }

    override fun getAllCompleteBindings(): CompletableFuture<List<BindingProgress>> {
        return CompletableFuture.supplyAsync {
            try {
                val activePlayers = PlayerDaoService.getAllActivePlayers().get()
                val bindings = mutableListOf<BindingProgress>()

                activePlayers.forEach { player ->
                    val playerUuid = player.getPlayerUuidAsUuid()
                    val progress = StepwiseBindingServiceImpl.getBindingProgress(playerUuid).get()
                    if (progress?.isFullyBound() == true) {
                        bindings.add(progress)
                    }
                }

                bindings
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun cleanupExpiredData(bindingExpireDays: Int, cookieExpireDays: Int): CompletableFuture<Int> {
        return MultiTableDatabaseService.cleanupExpiredData(bindingExpireDays, cookieExpireDays)
    }
}