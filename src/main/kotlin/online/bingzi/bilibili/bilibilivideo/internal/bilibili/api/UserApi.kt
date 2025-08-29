package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UserCardData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UserCardResponse
import taboolib.common.platform.function.submitAsync

object UserApi {
    
    private const val USER_CARD_URL = "https://api.bilibili.com/x/web-interface/card"
    
    private val gson = Gson()
    
    fun getFollowStatus(
        upMid: Long,
        sessdata: String,
        callback: (UpFollowData?) -> Unit
    ) {
        submitAsync {
            try {
                val client = HttpClientFactory.createCustomClient(sessdata = sessdata)
                
                val request = Request.Builder()
                    .url("$USER_CARD_URL?mid=$upMid")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val userCardResponse = gson.fromJson(responseBody, UserCardResponse::class.java)
                        
                        if (userCardResponse.code == 0 && userCardResponse.data != null) {
                            val userData = userCardResponse.data
                            val followData = UpFollowData(
                                upMid = userData.mid,
                                upName = userData.name,
                                followerMid = 0L, // 需要从session中获取
                                playerUuid = "", // 需要从外部传入
                                isFollowing = userData.following
                            )
                            
                            callback(followData)
                            return@submitAsync
                        }
                    }
                }
                
                callback(null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
    
    fun getUserBasicInfo(
        mid: Long,
        sessdata: String?,
        callback: (UserCardData?) -> Unit
    ) {
        submitAsync {
            try {
                val client = if (sessdata != null) {
                    HttpClientFactory.createCustomClient(sessdata = sessdata)
                } else {
                    HttpClientFactory.httpClient
                }
                
                val request = Request.Builder()
                    .url("$USER_CARD_URL?mid=$mid")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val userCardResponse = gson.fromJson(responseBody, UserCardResponse::class.java)
                        
                        if (userCardResponse.code == 0) {
                            callback(userCardResponse.data)
                            return@submitAsync
                        }
                    }
                }
                
                callback(null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
    
    fun isValidMid(mid: Long): Boolean {
        return mid > 0 && mid.toString().length >= 6
    }
}