package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.CoinStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.FavoriteStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.LikeStatusResponse
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.VideoTripleData
import taboolib.common.platform.function.submitAsync

object VideoApi {
    
    private const val LIKE_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/has/like"
    private const val COIN_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/coins"
    private const val FAVORITE_STATUS_URL = "https://api.bilibili.com/x/v2/fav/video/favoured"
    
    private val gson = Gson()
    
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
    
    private fun bvidToAid(bvid: String): Long? {
        // BV号转AV号的算法
        val table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
        val s = intArrayOf(11, 10, 3, 8, 4, 6)
        val xor = 177451812L
        val add = 8728348608L
        
        return try {
            if (!bvid.startsWith("BV")) return null
            
            val bvStr = bvid.substring(2)
            var r = 0L
            for (i in 0..5) {
                r += table.indexOf(bvStr[s[i]]) * Math.pow(58.0, i.toDouble()).toLong()
            }
            
            (r - add) xor xor
        } catch (e: Exception) {
            null
        }
    }
}