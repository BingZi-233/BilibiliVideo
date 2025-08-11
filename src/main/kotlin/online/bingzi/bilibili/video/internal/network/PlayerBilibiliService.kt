package online.bingzi.bilibili.video.internal.network

import online.bingzi.bilibili.video.internal.network.entity.*
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * 带Player上下文的 Bilibili 网络服务
 * 自动管理每个Player的独立Cookie和会话状态
 */
class PlayerBilibiliService(private val playerUuid: String) {

    /**
     * 在指定Player上下文中执行操作
     */
    private fun <T> withPlayerContext(operation: () -> T): T {
        val originalPlayer = BilibiliCookieJar.getCurrentPlayerUuid()
        return try {
            BilibiliCookieJar.setCurrentPlayer(playerUuid)
            operation()
        } finally {
            BilibiliCookieJar.setCurrentPlayer(originalPlayer)
        }
    }

    /**
     * 在指定Player上下文中执行异步操作
     */
    private fun <T> withPlayerContextAsync(operation: () -> CompletableFuture<T>): CompletableFuture<T> {
        val originalPlayer = BilibiliCookieJar.getCurrentPlayerUuid()
        BilibiliCookieJar.setCurrentPlayer(playerUuid)

        return operation().whenComplete { _, _ ->
            BilibiliCookieJar.setCurrentPlayer(originalPlayer)
        }
    }

    // ==================== 登录服务 ====================

    /**
     * 生成二维码登录信息
     */
    fun generateQrCode(): CompletableFuture<QrCodeLoginInfo?> {
        return withPlayerContextAsync {
            BilibiliLoginService.generateQrCode()
        }
    }

    /**
     * 轮询登录状态
     */
    fun pollLoginStatus(qrcodeKey: String): CompletableFuture<LoginStatus> {
        return withPlayerContextAsync {
            BilibiliLoginService.pollLoginStatus(qrcodeKey)
        }
    }

    /**
     * 检查当前Player登录状态
     */
    fun isLoggedIn(): Boolean {
        return BilibiliCookieJar.isLoggedIn(playerUuid)
    }

    /**
     * 获取当前Player的用户 UID
     */
    fun getCurrentUserId(): String? {
        return BilibiliCookieJar.getUserId(playerUuid)
    }

    /**
     * 登出当前Player
     */
    fun logout() {
        BilibiliCookieJar.clearCookies(playerUuid)
        console().sendWarn("loginLogout")
    }

    /**
     * 使用 Cookie 字符串为当前Player设置登录状态
     */
    fun loginWithCookies(cookies: String) {
        withPlayerContext {
            BilibiliLoginService.loginWithCookies(cookies)
        }
    }

    // ==================== 用户服务 ====================

    /**
     * 获取当前Player登录用户的基本信息
     */
    fun getCurrentUserInfo(): CompletableFuture<UserInfo?> {
        return withPlayerContextAsync {
            BilibiliUserService.getCurrentUserInfo()
        }
    }

    /**
     * 获取指定用户的详细信息
     */
    fun getUserInfo(uid: Long): CompletableFuture<UserDetailInfo?> {
        return withPlayerContextAsync {
            BilibiliUserService.getUserInfo(uid)
        }
    }

    /**
     * 获取用户的关注统计信息
     */
    fun getUserStats(uid: Long): CompletableFuture<UserStats?> {
        return withPlayerContextAsync {
            BilibiliUserService.getUserStats(uid)
        }
    }

    // ==================== 视频服务 ====================

    /**
     * 根据 BV 号获取视频详细信息
     */
    fun getVideoInfo(bvid: String): CompletableFuture<VideoInfo?> {
        return withPlayerContextAsync {
            BilibiliVideoService.getVideoInfo(bvid)
        }
    }

    /**
     * 获取视频的一键三连状态
     */
    fun getTripleActionStatus(aid: Long): CompletableFuture<TripleActionStatus?> {
        return withPlayerContextAsync {
            BilibiliVideoService.getTripleActionStatus(aid)
        }
    }

    /**
     * 对视频执行点赞操作
     */
    fun likeVideo(aid: Long, like: Boolean = true): CompletableFuture<Boolean> {
        return withPlayerContextAsync {
            BilibiliVideoService.likeVideo(aid, like)
        }
    }

    /**
     * 对视频执行投币操作
     */
    fun coinVideo(aid: Long, multiply: Int = 1, selectLike: Boolean = false): CompletableFuture<Boolean> {
        return withPlayerContextAsync {
            BilibiliVideoService.coinVideo(aid, multiply, selectLike)
        }
    }

    /**
     * 对视频执行收藏操作
     */
    fun favoriteVideo(aid: Long, addMediaIds: List<Long> = emptyList(), delMediaIds: List<Long> = emptyList()): CompletableFuture<Boolean> {
        return withPlayerContextAsync {
            BilibiliVideoService.favoriteVideo(aid, addMediaIds, delMediaIds)
        }
    }

    /**
     * 执行一键三连操作
     */
    fun performTripleAction(aid: Long): CompletableFuture<TripleActionResult> {
        return withPlayerContextAsync {
            BilibiliVideoService.performTripleAction(aid)
        }
    }

    /**
     * 快速获取视频信息和一键三连状态
     */
    fun getVideoWithTripleStatus(bvid: String): CompletableFuture<VideoWithTripleStatus?> {
        return getVideoInfo(bvid).thenCompose { videoInfo ->
            if (videoInfo != null && isLoggedIn()) {
                getTripleActionStatus(videoInfo.aid).thenApply { tripleStatus ->
                    VideoWithTripleStatus(videoInfo, tripleStatus)
                }
            } else {
                CompletableFuture.completedFuture(
                    if (videoInfo != null) VideoWithTripleStatus(videoInfo, null) else null
                )
            }
        }
    }
}