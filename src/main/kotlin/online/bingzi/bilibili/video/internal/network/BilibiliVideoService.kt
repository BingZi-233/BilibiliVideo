package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.api.event.network.video.*
import online.bingzi.bilibili.video.internal.network.entity.*
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * Bilibili 视频服务
 * 提供视频信息获取、点赞、投币、收藏等功能
 */
object BilibiliVideoService {

    private val gson = Gson()

    /**
     * 根据 BV 号获取视频详细信息
     * @param bvid BV 号
     * @return 视频信息或 null
     */
    fun getVideoInfo(bvid: String): CompletableFuture<VideoInfo?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/view?bvid=$bvid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val aid = data.get("aid")?.asLong
                            val bvid = data.get("bvid")?.asString
                            val title = data.get("title")?.asString
                            val desc = data.get("desc")?.asString
                            val pic = data.get("pic")?.asString
                            val pubdate = data.get("pubdate")?.asLong
                            val duration = data.get("duration")?.asInt

                            // 获取统计信息
                            val stat = data.getAsJsonObject("stat")
                            val view = stat.get("view")?.asLong ?: 0L
                            val danmaku = stat.get("danmaku")?.asLong ?: 0L
                            val reply = stat.get("reply")?.asLong ?: 0L
                            val favorite = stat.get("favorite")?.asLong ?: 0L
                            val coin = stat.get("coin")?.asLong ?: 0L
                            val share = stat.get("share")?.asLong ?: 0L
                            val like = stat.get("like")?.asLong ?: 0L

                            // 获取UP主信息
                            val owner = data.getAsJsonObject("owner")
                            val mid = owner.get("mid")?.asLong
                            val name = owner.get("name")?.asString
                            val face = owner.get("face")?.asString

                            if (aid != null && bvid != null && title != null && mid != null && name != null) {
                                val videoInfo = VideoInfo(
                                    aid = aid,
                                    bvid = bvid,
                                    title = title,
                                    description = desc,
                                    cover = pic,
                                    publishTime = pubdate,
                                    duration = duration,
                                    stats = VideoStats(
                                        view = view,
                                        danmaku = danmaku,
                                        reply = reply,
                                        favorite = favorite,
                                        coin = coin,
                                        share = share,
                                        like = like
                                    ),
                                    uploader = UploaderInfo(
                                        uid = mid,
                                        name = name,
                                        avatar = face
                                    )
                                )
                                
                                // 触发视频信息获取成功事件
                                VideoInfoFetchEvent(bvid, videoInfo, true).call()
                                
                                return@thenApply videoInfo
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("videoInfoGetFailed", message)
                            
                            // 触发视频信息获取失败事件
                            VideoInfoFetchEvent(bvid, null, false, message).call()
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: "解析响应失败"
                        console().sendWarn("videoInfoParseError", errorMsg)
                        
                        // 触发视频信息获取失败事件
                        VideoInfoFetchEvent(bvid, null, false, errorMsg).call()
                    }
                } else {
                    val errorMsg = response.getError() ?: "网络请求失败"
                    console().sendWarn("networkApiRequestFailed", errorMsg)
                    
                    // 触发视频信息获取失败事件
                    VideoInfoFetchEvent(bvid, null, false, errorMsg).call()
                }
                null
            }
    }

    /**
     * 获取视频的一键三连状态（点赞、投币、收藏）
     * @param aid 视频 AV 号
     * @return 一键三连状态或 null
     */
    fun getTripleActionStatus(aid: Long): CompletableFuture<TripleActionStatus?> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            console().sendWarn("loginRequired")
            
            // 触发视频三连状态获取失败事件
            VideoTripleStatusFetchEvent(aid, null, null, false, "需要登录").call()
            
            return CompletableFuture.completedFuture(null)
        }

        // 并发获取点赞、投币、收藏状态
        val likeFuture = getLikeStatus(aid)
        val coinFuture = getCoinStatus(aid)
        val favoriteFuture = getFavoriteStatus(aid)

        return CompletableFuture.allOf(likeFuture, coinFuture, favoriteFuture)
            .thenApply {
                try {
                    val liked = likeFuture.get() ?: false
                    val coined = coinFuture.get() ?: false
                    val favorited = favoriteFuture.get() ?: false

                    val tripleStatus = TripleActionStatus(
                        aid = aid,
                        liked = liked,
                        coined = coined,
                        favorited = favorited
                    )
                    
                    // 触发视频三连状态获取成功事件
                    VideoTripleStatusFetchEvent(aid, null, tripleStatus, true).call()
                    
                    tripleStatus
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "获取三连状态失败"
                    console().sendWarn("videoTripleStatusGetFailed", errorMsg)
                    
                    // 触发视频三连状态获取失败事件
                    VideoTripleStatusFetchEvent(aid, null, null, false, errorMsg).call()
                    
                    null
                }
            }
    }

    /**
     * 获取视频点赞状态
     * @param aid 视频 AV 号
     * @return 是否已点赞
     */
    private fun getLikeStatus(aid: Long): CompletableFuture<Boolean?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/archive/like?aid=$aid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.get("data")?.asInt ?: 0
                            return@thenApply data == 1
                        }
                    } catch (e: Exception) {
                        console().sendWarn("videoLikeParseError", e.message ?: "")
                    }
                }
                null
            }
    }

    /**
     * 获取视频投币状态
     * @param aid 视频 AV 号
     * @return 是否已投币
     */
    private fun getCoinStatus(aid: Long): CompletableFuture<Boolean?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/archive/coins?aid=$aid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")
                            val multiply = data.get("multiply")?.asInt ?: 0
                            return@thenApply multiply > 0
                        }
                    } catch (e: Exception) {
                        console().sendWarn("videoCoinParseError", e.message ?: "")
                    }
                }
                null
            }
    }

    /**
     * 获取视频收藏状态
     * @param aid 视频 AV 号
     * @return 是否已收藏
     */
    private fun getFavoriteStatus(aid: Long): CompletableFuture<Boolean?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/v2/fav/video/favoured?aid=$aid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")
                            val favoured = data.get("favoured")?.asBoolean ?: false
                            return@thenApply favoured
                        }
                    } catch (e: Exception) {
                        console().sendWarn("videoFavoriteParseError", e.message ?: "")
                    }
                }
                null
            }
    }

    /**
     * 对视频执行点赞操作
     * @param aid 视频 AV 号
     * @param like true 为点赞，false 为取消点赞
     * @return 操作是否成功
     */
    fun likeVideo(aid: Long, like: Boolean = true): CompletableFuture<Boolean> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            console().sendWarn("loginRequired")
            return CompletableFuture.completedFuture(false)
        }

        // 确保有 buvid3，这对点赞操作是必需的
        val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        if (playerUuid != null && !BilibiliCookieJar.hasValidBuvid3(playerUuid)) {
            return BuvidService.ensureBuvid(playerUuid).thenCompose { buvidSuccess ->
                if (!buvidSuccess) {
                    console().sendWarn("buvidRequiredForOperation", "点赞")
                    return@thenCompose CompletableFuture.completedFuture(false)
                }
                performLikeOperation(aid, like)
            }
        }

        return performLikeOperation(aid, like)
    }

    /**
     * 执行具体的点赞操作
     */
    private fun performLikeOperation(aid: Long, like: Boolean): CompletableFuture<Boolean> {
        val csrf = BilibiliCookieJar.getCsrfToken() ?: ""
        val likeValue = if (like) 1 else 2

        val formData = "aid=$aid&like=$likeValue&csrf=$csrf"

        return BilibiliApiClient.postAsync(
            "https://api.bilibili.com/x/web-interface/archive/like",
            formData,
            contentType = "application/x-www-form-urlencoded"
        ).thenApply { response ->
            if (response.isSuccess()) {
                try {
                    val json = JsonParser.parseString(response.data).asJsonObject
                    val code = json.get("code")?.asInt ?: -1

                    if (code == 0) {
                        console().sendInfo(if (like) "videoLikeSuccess" else "videoUnlikeSuccess")
                        return@thenApply true
                    } else {
                        val message = json.get("message")?.asString ?: "未知错误"
                        console().sendWarn("videoLikeFailed", message)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "解析响应失败"
                    console().sendWarn("videoLikeParseResponseError", errorMsg)
                }
            } else {
                val errorMsg = response.getError() ?: "网络请求失败"
                console().sendWarn("networkApiRequestFailed", errorMsg)
            }
            false
        }
    }

    /**
     * 对视频执行投币操作
     * @param aid 视频 AV 号
     * @param multiply 投币数量（1 或 2）
     * @param selectLike 是否同时点赞
     * @return 操作是否成功
     */
    fun coinVideo(aid: Long, multiply: Int = 1, selectLike: Boolean = false): CompletableFuture<Boolean> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            console().sendWarn("loginRequired")
            return CompletableFuture.completedFuture(false)
        }

        // 确保有 buvid3，这对投币操作是必需的
        val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        if (playerUuid != null && !BilibiliCookieJar.hasValidBuvid3(playerUuid)) {
            return BuvidService.ensureBuvid(playerUuid).thenCompose { buvidSuccess ->
                if (!buvidSuccess) {
                    console().sendWarn("buvidRequiredForOperation", "投币")
                    return@thenCompose CompletableFuture.completedFuture(false)
                }
                performCoinOperation(aid, multiply, selectLike)
            }
        }

        return performCoinOperation(aid, multiply, selectLike)
    }

    /**
     * 执行具体的投币操作
     */
    private fun performCoinOperation(aid: Long, multiply: Int, selectLike: Boolean): CompletableFuture<Boolean> {
        val csrf = BilibiliCookieJar.getCsrfToken() ?: ""
        val selectLikeValue = if (selectLike) 1 else 0

        val formData = "aid=$aid&multiply=$multiply&select_like=$selectLikeValue&cross_domain=true&csrf=$csrf"

        return BilibiliApiClient.postAsync(
            "https://api.bilibili.com/x/web-interface/coin/add",
            formData,
            contentType = "application/x-www-form-urlencoded"
        ).thenApply { response ->
            if (response.isSuccess()) {
                try {
                    val json = JsonParser.parseString(response.data).asJsonObject
                    val code = json.get("code")?.asInt ?: -1

                    if (code == 0) {
                        console().sendInfo("videoCoinSuccess", multiply.toString())
                        return@thenApply true
                    } else {
                        val message = json.get("message")?.asString ?: "未知错误"
                        console().sendWarn("videoCoinFailed", message)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "解析响应失败"
                    console().sendWarn("videoCoinParseResponseError", errorMsg)
                }
            } else {
                val errorMsg = response.getError() ?: "网络请求失败"
                console().sendWarn("networkApiRequestFailed", errorMsg)
            }
            false
        }
    }

    /**
     * 对视频执行收藏操作
     * @param aid 视频 AV 号
     * @param addMediaIds 要添加到的收藏夹 ID 列表
     * @param delMediaIds 要从中移除的收藏夹 ID 列表
     * @return 操作是否成功
     */
    fun favoriteVideo(aid: Long, addMediaIds: List<Long> = emptyList(), delMediaIds: List<Long> = emptyList()): CompletableFuture<Boolean> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            console().sendWarn("loginRequired")
            return CompletableFuture.completedFuture(false)
        }

        val csrf = BilibiliCookieJar.getCsrfToken() ?: ""
        val addMediaIdsStr = addMediaIds.joinToString(",")
        val delMediaIdsStr = delMediaIds.joinToString(",")

        val formData = "rid=$aid&type=2&add_media_ids=$addMediaIdsStr&del_media_ids=$delMediaIdsStr&csrf=$csrf"

        return BilibiliApiClient.postAsync(
            "https://api.bilibili.com/x/v3/fav/resource/deal",
            formData,
            contentType = "application/x-www-form-urlencoded"
        ).thenApply { response ->
            if (response.isSuccess()) {
                try {
                    val json = JsonParser.parseString(response.data).asJsonObject
                    val code = json.get("code")?.asInt ?: -1

                    if (code == 0) {
                        console().sendInfo("videoFavoriteSuccess")
                        return@thenApply true
                    } else {
                        val message = json.get("message")?.asString ?: "未知错误"
                        console().sendWarn("videoFavoriteFailed", message)
                    }
                } catch (e: Exception) {
                    val errorMsg = e.message ?: "解析响应失败"
                    console().sendWarn("videoFavoriteParseResponseError", errorMsg)
                }
            } else {
                val errorMsg = response.getError() ?: "网络请求失败"
                console().sendWarn("networkApiRequestFailed", errorMsg)
            }
            false
        }
    }

    /**
     * 执行一键三连操作
     * @param aid 视频 AV 号
     * @return 操作结果
     */
    fun performTripleAction(aid: Long): CompletableFuture<TripleActionResult> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            console().sendWarn("loginRequired")
            val result = TripleActionResult(false, false, false)
            return CompletableFuture.completedFuture(result)
        }

        // 依次执行点赞、投币操作
        return likeVideo(aid, true)
            .thenCompose { likeSuccess ->
                coinVideo(aid, 1, false).thenApply { coinSuccess ->
                    Pair(likeSuccess, coinSuccess)
                }
            }
            .thenCompose { (likeSuccess, coinSuccess) ->
                // 收藏操作需要先获取默认收藏夹，这里简化处理
                val result = TripleActionResult(
                    liked = likeSuccess,
                    coined = coinSuccess,
                    favorited = false  // 收藏操作需要收藏夹 ID，这里暂时不实现
                )
                
                CompletableFuture.completedFuture(result)
            }
    }
}