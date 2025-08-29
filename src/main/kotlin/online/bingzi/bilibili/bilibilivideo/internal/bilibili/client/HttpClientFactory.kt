package online.bingzi.bilibili.bilibilivideo.internal.bilibili.client

import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    private val cookieJar = object : CookieJar {
        private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()
        
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
        }
    }
    
    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(createHeaderInterceptor())
            .addInterceptor(createLoggingInterceptor())
            .build()
    }
    
    private fun createHeaderInterceptor(): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val requestWithHeaders = originalRequest.newBuilder()
                .addHeader("User-Agent", userAgent)
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Cache-Control", "no-cache")
                .addHeader("Connection", "keep-alive")
                .addHeader("Sec-Fetch-Site", "same-site")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Dest", "empty")
                .build()
            
            chain.proceed(requestWithHeaders)
        }
    }
    
    private fun createLoggingInterceptor(): Interceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }
    
    fun createCustomClient(
        sessdata: String? = null,
        buvid3: String? = null,
        biliJct: String? = null
    ): OkHttpClient {
        return httpClient.newBuilder()
            .addInterceptor(createCookieInterceptor(sessdata, buvid3, biliJct))
            .build()
    }
    
    private fun createCookieInterceptor(
        sessdata: String?,
        buvid3: String?,
        biliJct: String?
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            
            val cookieBuilder = StringBuilder()
            
            sessdata?.let { cookieBuilder.append("SESSDATA=$it; ") }
            buvid3?.let { cookieBuilder.append("buvid3=$it; ") }
            biliJct?.let { cookieBuilder.append("bili_jct=$it; ") }
            
            val requestBuilder = originalRequest.newBuilder()
            
            if (cookieBuilder.isNotEmpty()) {
                val cookieString = cookieBuilder.toString().trimEnd(';', ' ')
                requestBuilder.addHeader("Cookie", cookieString)
            }
            
            chain.proceed(requestBuilder.build())
        }
    }
}