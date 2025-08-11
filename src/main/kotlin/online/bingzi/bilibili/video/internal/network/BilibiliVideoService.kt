package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.api.event.network.video.VideoCommentsFetchEvent
import online.bingzi.bilibili.video.api.event.network.video.VideoInfoFetchEvent
import online.bingzi.bilibili.video.api.event.network.video.VideoTripleStatusFetchEvent
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

    /**
     * 获取视频评论区数据
     * @param oid 对象ID，可以是视频AV号或BV号
     * @param page 页码，从1开始，默认1
     * @param pageSize 每页数量，默认20，最大49
     * @param sort 排序方式：0=时间，1=点赞数，2=回复数，默认0
     * @return 评论响应数据或 null
     */
    fun getVideoComments(
        oid: String,
        page: Int = 1,
        pageSize: Int = 20,
        sort: Int = 0
    ): CompletableFuture<VideoCommentsResponse?> {
        val actualPageSize = pageSize.coerceIn(1, 49) // 限制页面大小在1-49之间
        val actualPage = page.coerceAtLeast(1) // 页码至少为1
        val actualSort = sort.coerceIn(0, 2) // 排序方式限制在0-2之间

        // 首先尝试将oid转换为AV号
        val avid = when {
            oid.startsWith("BV") -> {
                // 如果是BV号，需要先获取视频信息来得到AV号
                return getVideoInfo(oid).thenCompose { videoInfo ->
                    if (videoInfo != null) {
                        getVideoCommentsInternal(videoInfo.aid.toString(), actualPage, actualPageSize, actualSort, oid)
                    } else {
                        // 触发评论获取失败事件
                        VideoCommentsFetchEvent(oid, 1, actualPage, actualPageSize, actualSort.toString(), null, false, "无法获取视频信息").call()
                        CompletableFuture.completedFuture(null)
                    }
                }
            }

            oid.matches(Regex("\\d+")) -> oid // 纯数字，假设是AV号
            oid.startsWith("av") || oid.startsWith("AV") -> oid.substring(2) // 去掉av前缀
            else -> {
                // 触发评论获取失败事件
                VideoCommentsFetchEvent(oid, 1, actualPage, actualPageSize, actualSort.toString(), null, false, "无效的视频ID格式").call()
                return CompletableFuture.completedFuture(null)
            }
        }

        return getVideoCommentsInternal(avid, actualPage, actualPageSize, actualSort, oid)
    }

    /**
     * 获取视频评论的内部实现
     */
    private fun getVideoCommentsInternal(
        avid: String,
        page: Int,
        pageSize: Int,
        sort: Int,
        originalOid: String
    ): CompletableFuture<VideoCommentsResponse?> {
        // 使用新的带 WBI 签名的评论 API
        val baseUrl = "https://api.bilibili.com/x/v2/reply/wbi/main"
        val params = mapOf(
            "type" to 1,  // 1 表示视频评论
            "oid" to avid,
            "mode" to sort,  // 排序方式：0=时间，1=点赞数，2=回复数
            "pagination_str" to """{"offset":""}""",  // 首页留空
            "plat" to 1,  // 平台：1=web端
            "seek_rpid" to "",  // 跳转到指定评论，留空
            "web_location" to "1315875"  // 页面定位，固定值
        )

        // 对于翻页，需要使用 pagination_str 参数
        val paginationParams = if (page > 1) {
            params + ("pagination_str" to """{"offset":"${(page - 1) * pageSize}"}""")
        } else {
            params
        }

        return BilibiliApiClient.getAsyncWithWbi(baseUrl, paginationParams)
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            // 获取分页信息
                            val pageObj = data.getAsJsonObject("page")
                            val count = pageObj?.get("count")?.asLong ?: 0L
                            val size = pageObj?.get("size")?.asInt ?: pageSize
                            val pages = if (size > 0) ((count + size - 1) / size).toInt() else 0

                            // 获取游标信息
                            val cursorObj = data.getAsJsonObject("cursor")
                            val cursor = if (cursorObj != null) {
                                CommentCursor(
                                    allCount = cursorObj.get("all_count")?.asLong ?: 0L,
                                    isBegin = cursorObj.get("is_begin")?.asBoolean ?: true,
                                    prev = cursorObj.get("prev")?.asLong ?: 0L,
                                    next = cursorObj.get("next")?.asLong ?: 0L,
                                    isEnd = cursorObj.get("is_end")?.asBoolean ?: true,
                                    mode = cursorObj.get("mode")?.asInt ?: 0,
                                    showType = cursorObj.get("show_type")?.asInt ?: 0,
                                    supportMode = cursorObj.getAsJsonArray("support_mode")?.map { it.asInt } ?: emptyList(),
                                    name = cursorObj.get("name")?.asString ?: ""
                                )
                            } else null

                            // 解析评论列表
                            val repliesArray = data.getAsJsonArray("replies")
                            val comments = mutableListOf<CommentInfo>()

                            repliesArray?.forEach { replyElement ->
                                val commentInfo = parseCommentInfo(replyElement.asJsonObject)
                                if (commentInfo != null) {
                                    comments.add(commentInfo)
                                }
                            }

                            // 解析置顶评论
                            val upperObj = data.getAsJsonObject("upper")
                            val upperArray = upperObj?.getAsJsonArray("top")
                            val topComments = mutableListOf<CommentInfo>()

                            upperArray?.forEach { topElement ->
                                val topCommentInfo = parseCommentInfo(topElement.asJsonObject)
                                if (topCommentInfo != null) {
                                    topComments.add(topCommentInfo)
                                }
                            }

                            val result = VideoCommentsResponse(
                                comments = comments,
                                total = count,
                                pages = pages,
                                currentPage = page,  // 修正：使用实际的页码参数
                                pageSize = pageSize,
                                cursor = cursor,
                                hasMore = cursor?.isEnd != true,
                                topComments = topComments
                            )

                            // 触发评论获取成功事件
                            VideoCommentsFetchEvent(originalOid, 1, page, pageSize, sort.toString(), result, true).call()

                            return@thenApply result

                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("videoCommentsGetFailed", message)

                            // 触发评论获取失败事件
                            VideoCommentsFetchEvent(originalOid, 1, page, pageSize, sort.toString(), null, false, message).call()
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: "解析响应失败"
                        console().sendWarn("videoCommentsParseError", errorMsg)

                        // 触发评论获取失败事件
                        VideoCommentsFetchEvent(originalOid, 1, page, pageSize, sort.toString(), null, false, errorMsg).call()
                    }
                } else {
                    val errorMsg = response.getError() ?: "网络请求失败"
                    console().sendWarn("networkApiRequestFailed", errorMsg)

                    // 触发评论获取失败事件
                    VideoCommentsFetchEvent(originalOid, 1, page, pageSize, sort.toString(), null, false, errorMsg).call()
                }
                null
            }
    }

    /**
     * 解析评论信息
     */
    private fun parseCommentInfo(commentObj: com.google.gson.JsonObject): CommentInfo? {
        return try {
            val rpid = commentObj.get("rpid")?.asLong ?: return null
            val oid = commentObj.get("oid")?.asLong ?: return null
            val type = commentObj.get("type")?.asInt ?: return null
            val mid = commentObj.get("mid")?.asLong ?: return null
            val root = commentObj.get("root")?.asLong ?: 0L
            val parent = commentObj.get("parent")?.asLong ?: 0L
            val dialog = commentObj.get("dialog")?.asLong ?: 0L
            val count = commentObj.get("count")?.asInt ?: 0
            val rcount = commentObj.get("rcount")?.asInt ?: 0
            val state = commentObj.get("state")?.asInt ?: 0
            val fansgrade = commentObj.get("fansgrade")?.asInt ?: 0
            val attr = commentObj.get("attr")?.asInt ?: 0
            val ctime = commentObj.get("ctime")?.asLong ?: 0L
            val like = commentObj.get("like")?.asInt ?: 0
            val action = commentObj.get("action")?.asInt ?: 0
            val assist = commentObj.get("assist")?.asInt ?: 0
            val showFollow = commentObj.get("show_follow")?.asBoolean ?: false

            // 解析member信息
            val memberObj = commentObj.getAsJsonObject("member")
            val member = CommentMember(
                mid = memberObj?.get("mid")?.asString ?: "",
                uname = memberObj?.get("uname")?.asString ?: "",
                sex = memberObj?.get("sex")?.asString ?: "",
                sign = memberObj?.get("sign")?.asString ?: "",
                avatar = memberObj?.get("avatar")?.asString ?: "",
                rank = memberObj?.get("rank")?.asString ?: "",
                displayRank = memberObj?.get("DisplayRank")?.asString ?: "",
                levelInfo = null, // 简化处理
                pendant = null,   // 简化处理
                nameplate = null, // 简化处理
                officialVerify = null, // 简化处理
                vip = null,       // 简化处理
                fansDetail = null, // 简化处理
                following = memberObj?.get("following")?.asInt ?: 0,
                isFollowed = memberObj?.get("is_followed")?.asInt ?: 0
            )

            // 解析content信息
            val contentObj = commentObj.getAsJsonObject("content")
            val content = CommentContent(
                message = contentObj?.get("message")?.asString ?: "",
                plat = contentObj?.get("plat")?.asInt ?: 0,
                device = contentObj?.get("device")?.asString ?: "",
                members = null,  // 简化处理
                emote = null,    // 简化处理
                jumpUrl = null,  // 简化处理
                maxLine = contentObj?.get("max_line")?.asInt ?: 0
            )

            CommentInfo(
                rpid = rpid,
                oid = oid,
                type = type,
                mid = mid,
                root = root,
                parent = parent,
                dialog = dialog,
                count = count,
                rcount = rcount,
                state = state,
                fansgrade = fansgrade,
                attr = attr,
                ctime = ctime,
                like = like,
                action = action,
                member = member,
                content = content,
                replies = null,  // 简化处理，不解析子回复
                assist = assist,
                folder = null,   // 简化处理
                upAction = null, // 简化处理
                showFollow = showFollow
            )
        } catch (e: Exception) {
            console().sendWarn("commentParseError", e.message ?: "")
            null
        }
    }
}