package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UpFollowData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UserCardData
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.UserCardResponse
import taboolib.common.platform.function.submitAsync

/**
 * Bilibili用户API工具类
 * 
 * 提供用户信息查询和关注状态查询功能。
 * 支持获取用户基本信息、关注状态检查和MID有效性验证。
 * 所有网络请求均为异步执行，通过回调函数返回结果。
 * 
 * 主要功能：
 * - 查询UP主关注状态
 * - 获取用户基本信息
 * - MID格式验证
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object UserApi {
    
    /** Bilibili用户卡片信息API端点 */
    private const val USER_CARD_URL = "https://api.bilibili.com/x/web-interface/card"
    
    /** JSON解析器实例 */
    private val gson = Gson()
    
    /**
     * 获取UP主关注状态
     * 
     * 查询当前登录用户是否关注指定的UP主。
     * 需要提供有效的登录Cookie。
     * 
     * @param upMid UP主的Bilibili MID
     * @param sessdata 用户SESSDATA Cookie
     * @param callback 结果回调，成功时返回UpFollowData，失败时返回null
     */
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
    
    /**
     * 获取用户基本信息
     * 
     * 查询指定用户的基本信息，包括昵称、粉丝数、关注数等。
     * 可选择提供Cookie以获取更完整的信息。
     * 
     * @param mid 用户的Bilibili MID
     * @param sessdata 用户SESSDATA Cookie，可为null
     * @param callback 结果回调，成功时返回UserCardData，失败时返回null
     */
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
    
    /**
     * 验证MID格式是否有效
     * 
     * 检查给定的MID是否符合Bilibili用户ID的基本格式要求。
     * 有效的MID应该大于0且长度在6-12位之间。
     * 
     * @param mid 要验证的MID
     * @return true 如果MID格式有效，false 否则
     */
    fun isValidMid(mid: Long): Boolean {
        return mid > 0 && mid.toString().length >= 6
    }
}