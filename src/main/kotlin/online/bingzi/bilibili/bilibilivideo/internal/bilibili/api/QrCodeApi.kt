package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.helper.CookieHelper
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.*
import taboolib.common.platform.function.submitAsync

/**
 * Bilibili二维码登录API工具类
 * 
 * 提供Bilibili二维码登录功能，包括二维码生成和登录状态轮询。
 * 支持多玩家账户管理和Cookie信息提取。
 * 所有网络请求均为异步执行，通过回调函数返回结果。
 * 
 * 主要功能：
 * - 生成登录二维码
 * - 轮询登录状态
 * - Cookie信息提取
 * - 登录信息封装
 * 
 * 登录流程：
 * 1. 调用generateQrCode生成二维码
 * 2. 展示二维码让用户扫描
 * 3. 循环调用pollQrCodeStatus轮询状态
 * 4. 登录成功后获取LoginInfo
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object QrCodeApi {
    
    /** Bilibili二维码生成API端点 */
    private const val GENERATE_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    /** Bilibili二维码状态轮询API端点 */
    private const val POLL_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    
    /** JSON解析器实例 */
    private val gson = Gson()
    /** HTTP客户端实例 */
    private val httpClient = HttpClientFactory.httpClient
    
    /**
     * 生成登录二维码
     * 
     * 请求Bilibili服务器生成用于登录的二维码。
     * 用户可以使用Bilibili手机APP扫描此二维码进行登录。
     * 
     * @param callback 结果回调函数，成功时返回QrCodeData，失败时返回null
     */
    fun generateQrCode(callback: (QrCodeData?) -> Unit) {
        submitAsync {
            try {
                val request = Request.Builder()
                    .url(GENERATE_URL)
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        println("QrCodeApi: Response body: $responseBody")
                        println("QrCodeApi: Response body length: ${responseBody.length}")
                        println("QrCodeApi: Response body first char: '${responseBody.firstOrNull()}' (${responseBody.firstOrNull()?.code})")
                        try {
                            val qrResponse = gson.fromJson(responseBody, QrCodeGenerateResponse::class.java)
                            if (qrResponse.code == 0 && qrResponse.data != null) {
                                callback(qrResponse.data)
                                return@submitAsync
                            }
                        } catch (e: Exception) {
                            println("QrCodeApi: JSON parsing failed: ${e.message}")
                            println("QrCodeApi: Raw response (first 200 chars): ${responseBody.take(200)}")
                            e.printStackTrace()
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
     * 轮询二维码登录状态
     * 
     * 使用二维码密钥轮询登录状态，直到用户扫码并确认登录。
     * 登录成功后会自动提取Cookie信息并封装为LoginInfo。
     * 
     * @param qrcodeKey 二维码密钥，从generateQrCode获取
     * @param callback 结果回调函数，返回登录状态、轮询数据和登录信息（如果成功）
     */
    fun pollQrCodeStatus(qrcodeKey: String, callback: (LoginStatus, QrCodePollData?, LoginInfo?) -> Unit) {
        submitAsync {
            try {
                val request = Request.Builder()
                    .url("$POLL_URL?qrcode_key=$qrcodeKey")
                    .get()
                    .build()
                
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val pollResponse = gson.fromJson(responseBody, QrCodePollResponse::class.java)
                        
                        if (pollResponse.data != null) {
                            val status = LoginStatus.fromCode(pollResponse.data.code)
                            
                            // 如果登录成功，提取Cookie信息
                            if (status == LoginStatus.SUCCESS) {
                                val cookies = response.headers("Set-Cookie")
                                val loginInfo = extractLoginInfo(pollResponse.data, cookies)
                                callback(status, pollResponse.data, loginInfo)
                            } else {
                                callback(status, pollResponse.data, null)
                            }
                            
                            return@submitAsync
                        }
                    }
                }
                
                callback(LoginStatus.EXPIRED, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(LoginStatus.EXPIRED, null, null)
            }
        }
    }
    
    /**
     * 保存从响应中获取的Cookie信息
     * 
     * 内部方法，用于解析Set-Cookie响应头并保存Cookie信息。
     * Cookie会通过HttpClientFactory的CookieJar自动管理。
     * 
     * @param cookieHeaders Set-Cookie响应头列表
     * @param refreshToken 刷新令牌，可为null
     */
    private fun saveCookiesFromResponse(cookieHeaders: List<String>, refreshToken: String?) {
        // 解析Set-Cookie头获取Cookie信息
        val cookieMap = mutableMapOf<String, String>()
        
        cookieHeaders.forEach { cookieHeader ->
            val parts = cookieHeader.split(";")[0].split("=", limit = 2)
            if (parts.size == 2) {
                cookieMap[parts[0]] = parts[1]
            }
        }
        
        // 这里Cookie会通过HttpClientFactory的CookieJar自动保存
    }
    
    /**
     * 从轮询结果提取登录信息
     * 
     * 从二维码轮询的响应数据和Cookie中提取完整的登录信息。
     * 包括用户MID、SESSDATA、buvid3、bili_jct等认证信息。
     * 
     * @param pollData 轮询结果数据
     * @param cookies Cookie响应头列表
     * @return 成功时返回LoginInfo，失败时返回null
     */
    fun extractLoginInfo(pollData: QrCodePollData, cookies: List<String>): LoginInfo? {
        try {
            val cookieMap = mutableMapOf<String, String>()
            
            cookies.forEach { cookieHeader ->
                val parts = cookieHeader.split(";")[0].split("=", limit = 2)
                if (parts.size == 2) {
                    cookieMap[parts[0]] = parts[1]
                }
            }
            
            val sessdata = cookieMap["SESSDATA"]
            val buvid3 = cookieMap["buvid3"]
            val biliJct = cookieMap["bili_jct"]
            val dedeUserId = cookieMap["DedeUserID"]
            
            if (!sessdata.isNullOrEmpty() && !dedeUserId.isNullOrEmpty()) {
                return LoginInfo(
                    mid = dedeUserId.toLongOrNull() ?: 0L,
                    sessdata = sessdata,
                    buvid3 = buvid3 ?: "",
                    biliJct = biliJct ?: "",
                    refreshToken = pollData.refreshToken ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    /**
     * 登录信息数据类
     * 
     * 封装从二维码登录成功后获取的完整认证信息。
     * 包含用户身份标识和所有必要的Cookie信息。
     * 
     * @property mid 用户Bilibili MID（用户ID）
     * @property sessdata 会话认证Cookie
     * @property buvid3 设备标识Cookie
     * @property biliJct CSRF保护令牌
     * @property refreshToken 用于Cookie自动刷新的令牌
     * 
     * @since 1.0.0
     * @author BilibiliVideo
     */
    data class LoginInfo(
        val mid: Long,
        val sessdata: String,
        val buvid3: String,
        val biliJct: String,
        val refreshToken: String
    )
}