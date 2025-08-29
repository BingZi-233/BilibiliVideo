package online.bingzi.bilibili.bilibilivideo.internal.bilibili.helper

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

object CookieHelper {
    
    fun extractCookieMap(cookies: List<Cookie>): Map<String, String> {
        return cookies.associate { it.name to it.value }
    }
    
    fun extractSessdata(cookies: List<Cookie>): String? {
        return cookies.find { it.name == "SESSDATA" }?.value
    }
    
    fun extractBuvid3(cookies: List<Cookie>): String? {
        return cookies.find { it.name == "buvid3" }?.value
    }
    
    fun extractBiliJct(cookies: List<Cookie>): String? {
        return cookies.find { it.name == "bili_jct" }?.value
    }
    
    fun extractDedeUserId(cookies: List<Cookie>): String? {
        return cookies.find { it.name == "DedeUserID" }?.value
    }
    
    fun isCookieValid(sessdata: String?): Boolean {
        if (sessdata.isNullOrEmpty()) return false
        
        // 基本格式验证
        return sessdata.length > 20 && sessdata.matches(Regex("[a-zA-Z0-9%]+"))
    }
    
    fun createCookieString(
        sessdata: String? = null,
        buvid3: String? = null,
        biliJct: String? = null,
        dedeUserId: String? = null
    ): String {
        val cookies = mutableListOf<String>()
        
        sessdata?.let { cookies.add("SESSDATA=$it") }
        buvid3?.let { cookies.add("buvid3=$it") }
        biliJct?.let { cookies.add("bili_jct=$it") }
        dedeUserId?.let { cookies.add("DedeUserID=$it") }
        
        return cookies.joinToString("; ")
    }
    
    fun parseCookieString(cookieString: String): Map<String, String> {
        return cookieString.split(";")
            .mapNotNull { cookie ->
                val parts = cookie.trim().split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            .toMap()
    }
    
    fun createBilibiliCookies(cookieMap: Map<String, String>): List<Cookie> {
        val bilibiliUrl = "https://www.bilibili.com".toHttpUrl()
        
        return cookieMap.map { (name, value) ->
            Cookie.Builder()
                .domain(".bilibili.com")
                .name(name)
                .value(value)
                .path("/")
                .build()
        }
    }
}