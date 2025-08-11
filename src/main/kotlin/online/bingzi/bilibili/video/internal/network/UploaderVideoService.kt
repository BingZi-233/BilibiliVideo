package online.bingzi.bilibili.video.internal.network

import com.google.gson.JsonObject
import online.bingzi.bilibili.video.internal.database.entity.UploaderVideo
import online.bingzi.bilibili.video.internal.network.entity.ApiResponse
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * UP主视频服务
 * 负责获取UP主的所有视频信息
 */
object UploaderVideoService {

    private val client = BilibiliApiClient

    /**
     * 获取UP主的所有视频BV号
     * @param mid UP主UID
     * @param pageSize 每页数量（最大50）
     * @return 视频列表
     */
    fun getAllVideos(mid: Long, pageSize: Int = 50): CompletableFuture<List<UploaderVideo>> {
        return CompletableFuture.supplyAsync {
            val allVideos = mutableListOf<UploaderVideo>()
            var page = 1
            var hasMore = true

            console().sendInfo("uploaderVideoFetchStart", mid.toString())

            while (hasMore) {
                try {
                    val videos = getVideosByPage(mid, page, pageSize).get()
                    if (videos.isEmpty()) {
                        hasMore = false
                    } else {
                        allVideos.addAll(videos)
                        console().sendInfo("uploaderVideoFetchProgress", page.toString(), videos.size.toString())
                        page++
                        
                        // 添加延迟避免请求过快
                        Thread.sleep(500)
                    }
                } catch (e: Exception) {
                    console().sendWarn("uploaderVideoFetchError", e.message ?: "Unknown error")
                    hasMore = false
                }
            }

            console().sendInfo("uploaderVideoFetchComplete", mid.toString(), allVideos.size.toString())
            allVideos
        }
    }

    /**
     * 分页获取UP主视频
     * @param mid UP主UID
     * @param page 页码
     * @param pageSize 每页数量
     * @return 当前页的视频列表
     */
    fun getVideosByPage(mid: Long, page: Int, pageSize: Int): CompletableFuture<List<UploaderVideo>> {
        val params = mapOf(
            "mid" to mid.toString(),
            "ps" to pageSize.toString(),
            "pn" to page.toString(),
            "order" to "pubdate",  // 按发布时间排序
            "jsonp" to "jsonp"
        )

        return client.getAsyncWithWbi(
            "https://api.bilibili.com/x/space/wbi/arc/search",
            params
        ).thenApply { response ->
            parseVideoList(response.data, mid)
        }
    }

    /**
     * 获取UP主信息
     * @param mid UP主UID
     * @return UP主名称
     */
    fun getUploaderInfo(mid: Long): CompletableFuture<String> {
        val url = "https://api.bilibili.com/x/space/acc/info?mid=${mid}&jsonp=jsonp"

        return client.getAsync(url).thenApply { response ->
            val nameObj = response.data?.get("name")
            if (nameObj != null && nameObj.isJsonPrimitive) {
                nameObj.asString
            } else {
                "Unknown"
            }
        }
    }

    /**
     * 解析视频列表
     */
    private fun parseVideoList(response: JsonObject?, uploaderUid: Long): List<UploaderVideo> {
        val videos = mutableListOf<UploaderVideo>()

        try {
            val list = response?.getAsJsonObject("list")
            val vlist = list?.getAsJsonArray("vlist")

            vlist?.forEach { element ->
                val video = element.asJsonObject
                val uploaderVideo = UploaderVideo().apply {
                    this.uploaderUid = uploaderUid
                    this.uploaderName = video.get("author")?.asString ?: ""
                    this.bvId = video.get("bvid")?.asString ?: ""
                    this.title = video.get("title")?.asString ?: ""
                    this.description = video.get("description")?.asString ?: ""
                    this.publishTime = video.get("created")?.asLong ?: 0
                    this.duration = video.get("length")?.asString?.let { parseTimeToSeconds(it) } ?: 0
                    this.viewCount = video.get("play")?.asLong ?: 0
                    this.coverUrl = video.get("pic")?.asString ?: ""
                    
                    // 以下数据需要单独请求视频详情才能获取
                    this.likeCount = 0
                    this.coinCount = 0
                    this.favoriteCount = video.get("favorites")?.asLong ?: 0
                    this.shareCount = 0
                    this.danmakuCount = video.get("video_review")?.asLong ?: 0
                }
                
                if (uploaderVideo.bvId.isNotEmpty()) {
                    videos.add(uploaderVideo)
                }
            }
        } catch (e: Exception) {
            console().sendWarn("uploaderVideoParseError", e.message ?: "Unknown error")
        }

        return videos
    }

    /**
     * 解析时间字符串为秒数
     * @param timeStr 格式如 "12:34"
     */
    private fun parseTimeToSeconds(timeStr: String): Int {
        return try {
            val parts = timeStr.split(":")
            when (parts.size) {
                1 -> parts[0].toInt()
                2 -> parts[0].toInt() * 60 + parts[1].toInt()
                3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * 批量更新视频详细统计信息
     * @param videos 视频列表
     */
    fun updateVideosStats(videos: List<UploaderVideo>): CompletableFuture<List<UploaderVideo>> {
        return CompletableFuture.supplyAsync {
            videos.forEach { video ->
                try {
                    // 获取视频详细信息
                    val url = "https://api.bilibili.com/x/web-interface/view?bvid=${video.bvId}"
                    
                    val response = client.getAsync(url).get()
                    
                    val stat = response.data?.getAsJsonObject("stat")
                    
                    if (stat != null) {
                        video.updateStats(
                            viewCount = stat.getAsJsonPrimitive("view")?.asLong ?: 0,
                            likeCount = stat.getAsJsonPrimitive("like")?.asLong ?: 0,
                            coinCount = stat.getAsJsonPrimitive("coin")?.asLong ?: 0,
                            favoriteCount = stat.getAsJsonPrimitive("favorite")?.asLong ?: 0,
                            shareCount = stat.getAsJsonPrimitive("share")?.asLong ?: 0,
                            danmakuCount = stat.getAsJsonPrimitive("danmaku")?.asLong ?: 0
                        )
                    }
                    
                    // 添加延迟避免请求过快
                    Thread.sleep(200)
                } catch (e: Exception) {
                    console().sendWarn("uploaderVideoStatsUpdateError", video.bvId, e.message ?: "Unknown error")
                }
            }
            videos
        }
    }
}