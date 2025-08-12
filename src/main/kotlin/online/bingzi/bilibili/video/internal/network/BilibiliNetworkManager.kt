package online.bingzi.bilibili.video.internal.network

import online.bingzi.bilibili.video.internal.network.entity.UserInfo
import online.bingzi.bilibili.video.internal.network.entity.VideoWithTripleStatus
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Bilibili 网络模块管理器
 * 统一管理所有 Bilibili API 相关的网络操作，支持多Player独立会话
 */
object BilibiliNetworkManager {

    // Player服务实例缓存
    private val playerServices = ConcurrentHashMap<String, PlayerBilibiliService>()

    @Awake(LifeCycle.ENABLE)
    fun init(){
        console().sendInfo("networkModuleStartup")
    }

    /**
     * 获取指定Player的网络服务实例
     * @param playerUuid Player的UUID字符串
     * @return Player专属的网络服务实例
     */
    fun getPlayerService(playerUuid: String): PlayerBilibiliService {
        return playerServices.getOrPut(playerUuid) {
            PlayerBilibiliService(playerUuid)
        }
    }

    /**
     * 移除指定Player的服务实例（清理资源）
     * @param playerUuid Player的UUID字符串
     */
    fun removePlayerService(playerUuid: String) {
        playerServices.remove(playerUuid)
        BilibiliCookieJar.clearCookies(playerUuid)
    }

    /**
     * 获取所有活跃Player的UUID列表
     * @return Player UUID集合
     */
    fun getActivePlayerUuids(): Set<String> {
        return BilibiliCookieJar.getAllPlayerUuids()
    }

    /**
     * 检查指定Player是否已登录
     * @param playerUuid Player的UUID字符串
     * @return 是否已登录
     */
    fun isPlayerLoggedIn(playerUuid: String): Boolean {
        return BilibiliCookieJar.isLoggedIn(playerUuid)
    }

    /**
     * 获取指定Player的当前用户信息（如果已登录）
     * @param playerUuid Player的UUID字符串
     * @return 用户信息或 null
     */
    fun getPlayerCurrentUser(playerUuid: String): CompletableFuture<UserInfo?> {
        return getPlayerService(playerUuid).getCurrentUserInfo()
    }

    /**
     * 为指定Player快速获取视频信息和一键三连状态
     * @param playerUuid Player的UUID字符串
     * @param bvid BV 号
     * @return 包含视频信息和三连状态的结果
     */
    fun getVideoWithTripleStatusForPlayer(playerUuid: String, bvid: String): CompletableFuture<VideoWithTripleStatus?> {
        return getPlayerService(playerUuid).getVideoWithTripleStatus(bvid)
    }

    // ==================== 兼容性方法（向后兼容） ====================

    /**
     * 登录服务实例（兼容性保留）
     * @deprecated 使用 getPlayerService(playerUuid).xxx 替代
     */
    @Deprecated("使用 getPlayerService(playerUuid) 获取Player专属服务")
    val loginService = BilibiliLoginService

    /**
     * 用户服务实例（兼容性保留）
     * @deprecated 使用 getPlayerService(playerUuid).xxx 替代
     */
    @Deprecated("使用 getPlayerService(playerUuid) 获取Player专属服务")
    val userService = BilibiliUserService

    /**
     * 视频服务实例（兼容性保留）
     * @deprecated 使用 getPlayerService(playerUuid).xxx 替代
     */
    @Deprecated("使用 getPlayerService(playerUuid) 获取Player专属服务")
    val videoService = BilibiliVideoService

    /**
     * API 客户端实例
     */
    val apiClient = BilibiliApiClient

    /**
     * Cookie 管理器实例（兼容性保留）
     * @deprecated 使用新的 BilibiliCookieJar 替代
     */
    @Deprecated("使用新的 BilibiliCookieJar 替代")
    val cookieJar = BilibiliCookieJar

    /**
     * 检查网络模块是否就绪
     * @return 是否就绪
     */
    fun isReady(): Boolean {
        return true
    }

    /**
     * 快速登录检查（兼容性保留，使用第一个找到的已登录Player）
     * @deprecated 使用 isPlayerLoggedIn(playerUuid) 替代
     * @return 是否已登录
     */
    @Deprecated("使用 isPlayerLoggedIn(playerUuid) 替代")
    fun isLoggedIn(): Boolean {
        return BilibiliCookieJar.getAllPlayerUuids().any { BilibiliCookieJar.isLoggedIn(it) }
    }

    /**
     * 获取当前用户信息（兼容性保留，使用第一个找到的已登录Player）
     * @deprecated 使用 getPlayerCurrentUser(playerUuid) 替代
     * @return 用户信息或 null
     */
    @Deprecated("使用 getPlayerCurrentUser(playerUuid) 替代")
    fun getCurrentUser(): CompletableFuture<UserInfo?> {
        val loggedInPlayer = BilibiliCookieJar.getAllPlayerUuids().find { BilibiliCookieJar.isLoggedIn(it) }
        return if (loggedInPlayer != null) {
            getPlayerCurrentUser(loggedInPlayer)
        } else {
            CompletableFuture.completedFuture(null)
        }
    }

    /**
     * 快速获取视频信息和一键三连状态（兼容性保留，使用第一个找到的已登录Player）
     * @deprecated 使用 getVideoWithTripleStatusForPlayer(playerUuid, bvid) 替代
     * @param bvid BV 号
     * @return 包含视频信息和三连状态的结果
     */
    @Deprecated("使用 getVideoWithTripleStatusForPlayer(playerUuid, bvid) 替代")
    fun getVideoWithTripleStatus(bvid: String): CompletableFuture<VideoWithTripleStatus?> {
        val loggedInPlayer = BilibiliCookieJar.getAllPlayerUuids().find { BilibiliCookieJar.isLoggedIn(it) }
        return if (loggedInPlayer != null) {
            getVideoWithTripleStatusForPlayer(loggedInPlayer, bvid)
        } else {
            // 未登录时，只获取视频信息
            BilibiliVideoService.getVideoInfo(bvid).thenApply { videoInfo ->
                if (videoInfo != null) VideoWithTripleStatus(videoInfo, null) else null
            }
        }
    }

    /**
     * 清理网络模块资源
     * @param playerUuid 指定Player UUID，null表示清理所有
     */
    fun cleanup(playerUuid: String? = null) {
        if (playerUuid != null) {
            removePlayerService(playerUuid)
            console().sendInfo("networkModulePlayerCleanup", playerUuid)
        } else {
            playerServices.clear()
            BilibiliCookieJar.clearAllCookies()
            console().sendInfo("networkModuleCleanup")
        }
    }
}