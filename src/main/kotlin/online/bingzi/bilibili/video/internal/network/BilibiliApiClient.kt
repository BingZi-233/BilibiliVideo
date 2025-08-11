package online.bingzi.bilibili.video.internal.network

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import online.bingzi.bilibili.video.internal.network.entity.ApiResponse
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
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
        .addInterceptor(UserAgentInterceptor())
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
 * User-Agent 拦截器
 * 为所有请求添加合适的 User-Agent
 */
private class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}