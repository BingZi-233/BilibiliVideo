package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.CoinStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.FavoriteStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.LikeStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import taboolib.common.platform.function.submitAsync

/**
 * Bilibili视频API工具类
 * 
 * 提供视频三连状态（点赞、投币、收藏）查询功能。
 * 支持BV号到AV号的转换和并发查询多个状态。
 * 所有网络请求均为异步执行，通过回调函数返回结果。
 * 
 * 主要功能：
 * - 查询视频三连状态
 * - BV号与AV号互转
 * - 并发查询优化
 * - Cookie认证支持
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object VideoApi {
    
    /** Bilibili点赞状态查询API端点 */
    private const val LIKE_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/has/like"
    /** Bilibili投币状态查询API端点 */
    private const val COIN_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/coins"
    /** Bilibili收藏状态查询API端点 */
    private const val FAVORITE_STATUS_URL = "https://api.bilibili.com/x/v2/fav/video/favoured"
    
    /** JSON解析器实例 */
    private val gson = Gson()
    
    /**
     * 获取视频三连状态
     * 
     * 并发查询指定视频的点赞、投币、收藏状态，提高查询效率。
     * 需要用户已登录并提供有效的Cookie信息。
     * 
     * @param bvid 视频BV号
     * @param sessdata 用户SESSDATA Cookie
     * @param buvid3 设备标识Cookie
     * @param callback 结果回调，成功时返回VideoTripleData，失败时返回null
     */
    fun getTripleStatus(
        bvid: String,
        sessdata: String,
        buvid3: String,
        callback: (VideoTripleData?) -> Unit
    ) {
        submitAsync {
            try {
                val aid = bvidToAid(bvid)
                if (aid == null) {
                    callback(null)
                    return@submitAsync
                }
                
                var isLiked = false
                var coinCount = 0
                var isFavorited = false
                var completedRequests = 0
                
                val onRequestComplete = {
                    completedRequests++
                    if (completedRequests >= 3) {
                        val tripleData = VideoTripleData(
                            bvid = bvid,
                            playerUuid = "",
                            mid = 0L,
                            isLiked = isLiked,
                            coinCount = coinCount,
                            isFavorited = isFavorited
                        )
                        callback(tripleData)
                    }
                }
                
                // 查询点赞状态
                getLikeStatus(bvid, aid, sessdata, buvid3) { liked ->
                    isLiked = liked
                    onRequestComplete()
                }
                
                // 查询投币状态
                getCoinStatus(bvid, aid, sessdata, buvid3) { coins ->
                    coinCount = coins
                    onRequestComplete()
                }
                
                // 查询收藏状态
                getFavoriteStatus(aid, sessdata, buvid3) { favorited ->
                    isFavorited = favorited
                    onRequestComplete()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
    
    private fun getLikeStatus(
        bvid: String,
        aid: Long,
        sessdata: String,
        buvid3: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            try {
                val client = HttpClientFactory.createCustomClient(
                    sessdata = sessdata,
                    buvid3 = buvid3
                )
                
                val request = Request.Builder()
                    .url("$LIKE_STATUS_URL?bvid=$bvid")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val likeResponse = gson.fromJson(responseBody, LikeStatusResponse::class.java)
                        callback(likeResponse.code == 0 && likeResponse.data == 1)
                        return@submitAsync
                    }
                }
                
                callback(false)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    private fun getCoinStatus(
        bvid: String,
        aid: Long,
        sessdata: String,
        buvid3: String,
        callback: (Int) -> Unit
    ) {
        submitAsync {
            try {
                val client = HttpClientFactory.createCustomClient(
                    sessdata = sessdata,
                    buvid3 = buvid3
                )
                
                val request = Request.Builder()
                    .url("$COIN_STATUS_URL?bvid=$bvid")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val coinResponse = gson.fromJson(responseBody, CoinStatusResponse::class.java)
                        callback(coinResponse.data?.multiply ?: 0)
                        return@submitAsync
                    }
                }
                
                callback(0)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(0)
            }
        }
    }
    
    private fun getFavoriteStatus(
        aid: Long,
        sessdata: String,
        buvid3: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            try {
                val client = HttpClientFactory.createCustomClient(
                    sessdata = sessdata,
                    buvid3 = buvid3
                )
                
                val request = Request.Builder()
                    .url("$FAVORITE_STATUS_URL?aid=$aid")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val favoriteResponse = gson.fromJson(responseBody, FavoriteStatusResponse::class.java)
                        callback(favoriteResponse.data?.favoured == true)
                        return@submitAsync
                    }
                }
                
                callback(false)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * BV号转AV号算法
     * 
     * 使用Bilibili官方算法将BV号转换为AV号。
     * BV号是Bilibili视频的新标识符，AV号是旧标识符，某些API仍需要AV号。
     * 
     * @param bvid BV号字符串，格式如"BV1xx411c7mD"
     * @return 对应的AV号，转换失败时返回null
     */
    private fun bvidToAid(bvid: String): Long? {
        val table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
        val s = intArrayOf(11, 10, 3, 8, 4, 6)
        val xor = 177451812L
        val add = 8728348608L

        return try {
            if (!bvid.startsWith("BV") || bvid.length != 12) return null
            var r = 0L
            for (i in 0..5) {
                val c = bvid[s[i]]
                val index = table.indexOf(c)
                if (index < 0) return null
                r += index * Math.pow(58.0, i.toDouble()).toLong()
            }
            (r - add) xor xor
        } catch (e: Exception) {
            null
        }
    }
}