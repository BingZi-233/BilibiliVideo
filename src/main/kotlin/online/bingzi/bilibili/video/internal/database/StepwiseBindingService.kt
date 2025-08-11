package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.entity.BindingProgress
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 分步绑定服务接口
 * 针对实际使用场景优化，支持逐步完善用户绑定信息
 */
interface StepwiseBindingService {

    /**
     * 第一步：创建玩家记录
     * 通常在玩家首次进入服务器时调用
     */
    fun createPlayer(playerUuid: UUID, playerName: String): CompletableFuture<Boolean>

    /**
     * 第二步：绑定QQ号
     * 通常通过QQ机器人或游戏内命令完成
     */
    fun bindQQ(playerUuid: UUID, qqNumber: String, qqNickname: String? = null): CompletableFuture<Boolean>

    /**
     * 第三步：绑定Bilibili账号
     * 通常通过二维码登录完成
     */
    fun bindBilibili(
        playerUuid: UUID,
        bilibiliUid: Long,
        username: String? = null,
        nickname: String? = null,
        avatarUrl: String? = null,
        level: Int? = null
    ): CompletableFuture<Boolean>

    /**
     * 第四步：保存/更新Cookie
     * 在Bilibili登录成功后调用
     */
    fun saveCookie(playerUuid: UUID, cookieString: String): CompletableFuture<Boolean>

    /**
     * 查询用户当前绑定进度
     */
    fun getBindingProgress(playerUuid: UUID): CompletableFuture<BindingProgress?>

    /**
     * 解绑指定类型的绑定
     */
    fun unbindQQ(playerUuid: UUID): CompletableFuture<Boolean>
    fun unbindBilibili(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * 完全删除用户及所有绑定信息
     */
    fun deletePlayer(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * 根据不同方式查找用户
     */
    fun findPlayerByQQ(qqNumber: String): CompletableFuture<BindingProgress?>
    fun findPlayerByBilibiliUid(bilibiliUid: Long): CompletableFuture<BindingProgress?>

    /**
     * 获取可用于API调用的Cookie
     */
    fun getValidCookie(playerUuid: UUID): CompletableFuture<String?>

    /**
     * 更新Cookie使用时间（在使用Cookie进行API调用时）
     */
    fun markCookieUsed(playerUuid: UUID): CompletableFuture<Boolean>

    /**
     * 缓存管理
     */
    fun clearPlayerCache(playerUuid: UUID)
    fun clearAllCache()
    fun getCacheSize(): Int
}

