package online.bingzi.bilibili.video.internal.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import online.bingzi.bilibili.video.internal.network.entity.ApiResponse
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import taboolib.module.lang.sendInfo
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Bilibili API 客户端
 * 提供对 Bilibili API 的网络访问功能
 */
object BilibiliApiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .cookieJar(BilibiliCookieJar)
        .addInterceptor(BuvidInterceptor())
        .build()

    /**
     * 异步执行 GET 请求
     * @param url 请求地址
     * @param headers 请求头
     * @return CompletableFuture<ApiResponse>
     */
    fun getAsync(url: String, headers: Map<String, String> = emptyMap()): CompletableFuture<ApiResponse> {
        val future = CompletableFuture<ApiResponse>()

        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                console().sendWarn("networkRequestFailed", e.message ?: "")
                future.complete(ApiResponse.failure(e.message ?: "未知错误"))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        future.complete(ApiResponse.success(body, response.headers))
                    } else {
                        console().sendWarn("httpStatusCode", response.code.toString())
                        future.complete(ApiResponse.failure("HTTP ${response.code}: ${response.message}"))
                    }
                } catch (e: Exception) {
                    console().sendWarn("networkResponseParseFailed", e.message ?: "")
                    future.complete(ApiResponse.failure(e.message ?: "响应处理失败"))
                }
            }
        })

        return future
    }

    /**
     * 异步执行 POST 请求
     * @param url 请求地址
     * @param data 请求数据
     * @param headers 请求头
     * @param contentType 内容类型
     * @return CompletableFuture<ApiResponse>
     */
    fun postAsync(
        url: String,
        data: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String = "application/json; charset=utf-8"
    ): CompletableFuture<ApiResponse> {
        val future = CompletableFuture<ApiResponse>()

        val requestBuilder = Request.Builder()
            .url(url)
            .post(data.toRequestBody(contentType.toMediaType()))

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        val request = requestBuilder.build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                console().sendWarn("networkRequestFailed", e.message ?: "")
                future.complete(ApiResponse.failure(e.message ?: "未知错误"))
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        future.complete(ApiResponse.success(body, response.headers))
                    } else {
                        console().sendWarn("httpStatusCode", response.code.toString())
                        future.complete(ApiResponse.failure("HTTP ${response.code}: ${response.message}"))
                    }
                } catch (e: Exception) {
                    console().sendWarn("networkResponseParseFailed", e.message ?: "")
                    future.complete(ApiResponse.failure(e.message ?: "响应处理失败"))
                }
            }
        })

        return future
    }

    /**
     * 同步执行 GET 请求
     * 注意：此方法会阻塞当前线程，建议在异步环境中使用
     */
    fun getSync(url: String, headers: Map<String, String> = emptyMap()): ApiResponse {
        return getAsync(url, headers).get()
    }

    /**
     * 同步执行 POST 请求
     * 注意：此方法会阻塞当前线程，建议在异步环境中使用
     */
    fun postSync(
        url: String,
        data: String,
        headers: Map<String, String> = emptyMap(),
        contentType: String = "application/json; charset=utf-8"
    ): ApiResponse {
        return postAsync(url, data, headers, contentType).get()
    }

    /**
     * 设置自定义 Cookie
     */
    fun setCookies(cookies: Map<String, String>) {
        BilibiliCookieJar.setCookies(cookies)
    }

    /**
     * 获取当前 Cookie
     */
    fun getCookies(): Map<String, String> {
        return BilibiliCookieJar.getCookies()
    }

    /**
     * 清除所有 Cookie
     */
    fun clearCookies() {
        BilibiliCookieJar.clearCookies()
    }
}

/**
 * Buvid 和 User-Agent 拦截器
 * 同时处理 User-Agent 设置和 buvid 自动获取
 */
private class BuvidInterceptor : Interceptor {
    
    // 需要 buvid3 的 API 路径列表
    private val buvidRequiredPaths = setOf(
        "/x/web-interface/wbi/search/all/v2",    // 综合搜索
        "/x/web-interface/wbi/search/type",     // 分类搜索
        "/x/web-interface/archive/like",        // 点赞视频
        "/x/web-interface/coin/add",            // 投币视频
        "/x/web-interface/archive/like/triple", // 一键三连
        "/x/web-interface/share/add",           // 分享视频
        "/x/space/wbi/acc/info",                // 用户空间详细信息
        "/x/player/wbi/playurl",                // 获取视频流地址
        "/x/resource/laser2"                    // 播放反馈
    )
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // 添加 User-Agent
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        
        val url = requestWithUserAgent.url.toString()
        val path = requestWithUserAgent.url.encodedPath
        
        // 只对需要 buvid 的特定 API 进行处理
        if (isApiRequiresBuvid(url, path)) {
            val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
            
            // 如果有当前用户且没有有效的 buvid3，尝试预先获取
            if (playerUuid != null && !BilibiliCookieJar.hasValidBuvid3(playerUuid)) {
                try {
                    // 使用异步方式获取，避免阻塞当前请求
                    BuvidService.ensureBuvid(playerUuid).get(5, TimeUnit.SECONDS)
                } catch (e: Exception) {
                    // 如果获取失败，记录警告但继续请求
                    console().sendWarn("buvidEnsureTimeout", playerUuid, e.message ?: "")
                }
            }
        }
        
        return chain.proceed(requestWithUserAgent)
    }
    
    /**
     * 判断 API 是否需要 buvid
     */
    private fun isApiRequiresBuvid(url: String, path: String): Boolean {
        // 检查是否是 Bilibili 域名
        if (!url.contains("bilibili.com") && !url.contains("bilivideo.com")) {
            return false
        }
        
        // 排除获取 buvid 的 API，避免递归
        if (path.contains("/x/web-frontend/getbuvid") || path.contains("/x/frontend/finger/spi")) {
            return false
        }
        
        // 检查是否是需要 buvid 的 API
        return buvidRequiredPaths.any { requiredPath ->
            path.contains(requiredPath)
        }
    }
}