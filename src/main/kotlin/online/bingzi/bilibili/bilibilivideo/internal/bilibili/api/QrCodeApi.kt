package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.helper.CookieHelper
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.*
import taboolib.common.platform.function.submitAsync

object QrCodeApi {
    
    private const val GENERATE_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    private const val POLL_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    
    private val gson = Gson()
    private val httpClient = HttpClientFactory.httpClient
    
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
                        val qrResponse = gson.fromJson(responseBody, QrCodeGenerateResponse::class.java)
                        if (qrResponse.code == 0 && qrResponse.data != null) {
                            callback(qrResponse.data)
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
    
    data class LoginInfo(
        val mid: Long,
        val sessdata: String,
        val buvid3: String,
        val biliJct: String,
        val refreshToken: String
    )
}