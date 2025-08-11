package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.api.event.network.user.UserVideoListFetchEvent
import online.bingzi.bilibili.video.internal.network.entity.*
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * Bilibili 用户服务
 * 提供用户信息获取等功能
 */
object BilibiliUserService {

    private val gson = Gson()

    /**
     * 为当前用户预先获取并设置 buvid
     * 这个方法应该在用户登录后或进行需要 buvid 的操作前调用
     * @return 是否成功获取并设置 buvid
     */
    fun ensureBuvidForCurrentUser(): CompletableFuture<Boolean> {
        val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        return if (playerUuid != null) {
            BuvidService.ensureBuvid(playerUuid)
        } else {
            CompletableFuture.completedFuture(false)
        }
    }

    /**
     * 为指定用户预先获取并设置 buvid
     * @param playerUuid Player UUID
     * @return 是否成功获取并设置 buvid
     */
    fun ensureBuvidForUser(playerUuid: String): CompletableFuture<Boolean> {
        return BuvidService.ensureBuvid(playerUuid)
    }

    /**
     * 获取当前登录用户的基本信息
     * @return 用户信息或 null
     */
    fun getCurrentUserInfo(): CompletableFuture<UserInfo?> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            return CompletableFuture.completedFuture(null)
        }

        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/nav")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val mid = data.get("mid")?.asLong
                            val uname = data.get("uname")?.asString
                            val face = data.get("face")?.asString
                            val level = data.get("level_info")?.asJsonObject?.get("current_level")?.asInt ?: 0
                            val coins = data.get("money")?.asDouble ?: 0.0

                            if (mid != null && uname != null) {
                                return@thenApply UserInfo(
                                    uid = mid,
                                    username = uname,
                                    avatar = face,
                                    level = level,
                                    coins = coins
                                )
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userInfoGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userInfoParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 获取指定用户的详细信息
     * @param uid 用户 UID
     * @return 用户详细信息或 null
     */
    fun getUserInfo(uid: Long): CompletableFuture<UserDetailInfo?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/space/acc/info?mid=$uid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val mid = data.get("mid")?.asLong
                            val name = data.get("name")?.asString
                            val face = data.get("face")?.asString
                            val sign = data.get("sign")?.asString
                            val level = data.get("level")?.asInt ?: 0
                            val sex = data.get("sex")?.asString

                            if (mid != null && name != null) {
                                return@thenApply UserDetailInfo(
                                    uid = mid,
                                    name = name,
                                    avatar = face,
                                    signature = sign,
                                    level = level,
                                    gender = sex
                                )
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userDetailGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userDetailParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 获取用户的关注统计信息
     * @param uid 用户 UID
     * @return 关注统计信息或 null
     */
    fun getUserStats(uid: Long): CompletableFuture<UserStats?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/relation/stat?vmid=$uid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val following = data.get("following")?.asLong ?: 0L
                            val follower = data.get("follower")?.asLong ?: 0L

                            return@thenApply UserStats(
                                uid = uid,
                                following = following,
                                follower = follower
                            )
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userStatsGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userStatsParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 获取指定用户的上传视频列表
     * @param uid 用户UID
     * @param page 页码，从1开始
     * @param pageSize 每页数量，默认20，最大50
     * @param order 排序方式：pubdate(按发布时间)，click(按播放量)，stow(按收藏数)，默认pubdate
     * @return 用户视频列表响应或 null
     */
    fun getUserVideoList(
        uid: Long, 
        page: Int = 1, 
        pageSize: Int = 20,
        order: String = "pubdate"
    ): CompletableFuture<UserVideoListResponse?> {
        val actualPageSize = pageSize.coerceIn(1, 50) // 限制页面大小在1-50之间
        val actualPage = page.coerceAtLeast(1) // 页码至少为1
        
        val url = "https://api.bilibili.com/x/space/wbi/arc/search?mid=$uid&pn=$actualPage&ps=$actualPageSize&order=$order"
        
        return BilibiliApiClient.getAsync(url)
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")
                            val list = data.getAsJsonObject("list")
                            
                            // 获取分页信息
                            val page = list.get("page")?.asJsonObject
                            val total = page?.get("count")?.asLong ?: 0L
                            val pages = ((total + actualPageSize - 1) / actualPageSize).toInt() // 计算总页数
                            
                            // 获取视频数据
                            val vlistArray = list.getAsJsonArray("vlist")
                            val videos = mutableListOf<UserVideoInfo>()
                            
                            vlistArray?.forEach { videoElement ->
                                val videoObj = videoElement.asJsonObject
                                
                                val aid = videoObj.get("aid")?.asLong
                                val bvid = videoObj.get("bvid")?.asString
                                val title = videoObj.get("title")?.asString
                                val description = videoObj.get("description")?.asString
                                val pic = videoObj.get("pic")?.asString
                                val length = videoObj.get("length")?.asString // 格式如 "02:30"
                                val created = videoObj.get("created")?.asLong
                                val play = videoObj.get("play")?.asLong ?: 0L
                                val danmaku = videoObj.get("video_review")?.asLong ?: 0L
                                val comment = videoObj.get("comment")?.asLong ?: 0L
                                val favorites = videoObj.get("favorites")?.asLong ?: 0L
                                val author = videoObj.get("author")?.asString
                                val mid = videoObj.get("mid")?.asLong
                                
                                if (aid != null && bvid != null && title != null && mid != null && author != null) {
                                    // 将时间字符串转换为秒数
                                    val duration = length?.let { parseTimeLength(it) }
                                    
                                    videos.add(UserVideoInfo(
                                        aid = aid,
                                        bvid = bvid,
                                        title = title,
                                        description = description,
                                        cover = pic,
                                        duration = duration,
                                        publishTime = created,
                                        videoStats = SimpleVideoStats(
                                            view = play,
                                            danmaku = danmaku,
                                            reply = comment,
                                            favorite = favorites,
                                            coin = 0L, // 视频列表API不提供投币数
                                            share = 0L, // 视频列表API不提供分享数
                                            like = 0L   // 视频列表API不提供点赞数
                                        ),
                                        author = VideoAuthor(
                                            uid = mid,
                                            name = author,
                                            avatar = null // 视频列表API不提供作者头像
                                        )
                                    ))
                                }
                            }
                            
                            val result = UserVideoListResponse(
                                videos = videos,
                                total = total,
                                pages = pages,
                                currentPage = actualPage,
                                pageSize = actualPageSize
                            )
                            
                            // 触发用户视频列表获取成功事件
                            UserVideoListFetchEvent(uid, actualPage, actualPageSize, result, true).call()
                            
                            return@thenApply result
                            
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userVideoListGetFailed", message)
                            
                            // 触发用户视频列表获取失败事件
                            UserVideoListFetchEvent(uid, actualPage, actualPageSize, null, false, message).call()
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: "解析响应失败"
                        console().sendWarn("userVideoListParseError", errorMsg)
                        
                        // 触发用户视频列表获取失败事件
                        UserVideoListFetchEvent(uid, actualPage, actualPageSize, null, false, errorMsg).call()
                    }
                } else {
                    val errorMsg = response.getError() ?: "网络请求失败"
                    console().sendWarn("networkApiRequestFailed", errorMsg)
                    
                    // 触发用户视频列表获取失败事件
                    UserVideoListFetchEvent(uid, actualPage, actualPageSize, null, false, errorMsg).call()
                }
                null
            }
    }
    
    /**
     * 将时间长度字符串解析为秒数
     * @param timeStr 时间字符串，格式如 "02:30" 或 "01:02:30"
     * @return 时间秒数，解析失败返回 null
     */
    private fun parseTimeLength(timeStr: String): Int? {
        return try {
            val parts = timeStr.split(":")
            when (parts.size) {
                2 -> { // MM:SS 格式
                    val minutes = parts[0].toInt()
                    val seconds = parts[1].toInt()
                    minutes * 60 + seconds
                }
                3 -> { // HH:MM:SS 格式
                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()
                    val seconds = parts[2].toInt()
                    hours * 3600 + minutes * 60 + seconds
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}