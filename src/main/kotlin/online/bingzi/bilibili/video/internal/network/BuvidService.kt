package online.bingzi.bilibili.video.internal.network

import com.google.gson.JsonParser
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Bilibili buvid 服务
 * 负责获取和管理 buvid3、buvid4 等设备标识符
 */
object BuvidService {

    // 专用于获取 buvid 的客户端，避免循环依赖
    private val buvidClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(30))
        .readTimeout(Duration.ofSeconds(30))
        .writeTimeout(Duration.ofSeconds(30))
        .addInterceptor(BuvidUserAgentInterceptor())
        .build()

    /**
     * 异步获取 buvid3
     * @return CompletableFuture<String?> buvid3 值，失败时返回 null
     */
    fun getBuvid3Async(): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()

        val request = Request.Builder()
            .url("https://api.bilibili.com/x/web-frontend/getbuvid".toHttpUrl())
            .get()
            .build()

        buvidClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                console().sendWarn("buvidRequestFailed", "buvid3", e.message ?: "")
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val body = resp.body.string()
                        if (resp.isSuccessful) {
                            val jsonResponse = JsonParser.parseString(body).asJsonObject
                            val code = jsonResponse.get("code")?.asInt ?: -1

                            if (code == 0) {
                                val data = jsonResponse.getAsJsonObject("data")
                                val buvid = data?.get("buvid")?.asString
                                if (buvid != null) {
                                    console().sendInfo("buvidObtained", "buvid3", buvid)
                                    future.complete(buvid)
                                } else {
                                    console().sendWarn("buvidDataNull", "buvid3")
                                    future.complete(null)
                                }
                            } else {
                                console().sendWarn("buvidApiError", "buvid3", code.toString())
                                future.complete(null)
                            }
                        } else {
                            console().sendWarn("buvidHttpError", "buvid3", resp.code.toString())
                            future.complete(null)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("buvidParseFailed", "buvid3", e.message ?: "")
                        future.complete(null)
                    }
                }
            }
        })

        return future
    }

    /**
     * 异步同时获取 buvid3 和 buvid4
     * @return CompletableFuture<Pair<String?, String?>> (buvid3, buvid4)，失败时对应项为 null
     */
    fun getBuvidAsync(): CompletableFuture<Pair<String?, String?>> {
        val future = CompletableFuture<Pair<String?, String?>>()

        val request = Request.Builder()
            .url("https://api.bilibili.com/x/frontend/finger/spi".toHttpUrl())
            .get()
            .build()

        buvidClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                console().sendWarn("buvidRequestFailed", "buvid3/buvid4", e.message ?: "")
                future.complete(Pair(null, null))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val body = resp.body.string()
                        if (resp.isSuccessful) {
                            val jsonResponse = JsonParser.parseString(body).asJsonObject
                            val code = jsonResponse.get("code")?.asInt ?: -1

                            if (code == 0) {
                                val data = jsonResponse.getAsJsonObject("data")
                                val buvid3 = data?.get("b_3")?.asString
                                val buvid4 = data?.get("b_4")?.asString

                                if (buvid3 != null || buvid4 != null) {
                                    console().sendInfo("buvidObtained", "buvid3/buvid4", "$buvid3, $buvid4")
                                    future.complete(Pair(buvid3, buvid4))
                                } else {
                                    console().sendWarn("buvidDataNull", "buvid3/buvid4")
                                    future.complete(Pair(null, null))
                                }
                            } else {
                                console().sendWarn("buvidApiError", "buvid3/buvid4", code.toString())
                                future.complete(Pair(null, null))
                            }
                        } else {
                            console().sendWarn("buvidHttpError", "buvid3/buvid4", resp.code.toString())
                            future.complete(Pair(null, null))
                        }
                    } catch (e: Exception) {
                        console().sendWarn("buvidParseFailed", "buvid3/buvid4", e.message ?: "")
                        future.complete(Pair(null, null))
                    }
                }
            }
        })

        return future
    }

    /**
     * 从 bilibili.com 主页的响应头中获取 buvid3
     * @return CompletableFuture<String?> buvid3 值，失败时返回 null
     */
    fun getBuvid3FromHeaderAsync(): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()

        val request = Request.Builder()
            .url("https://www.bilibili.com/".toHttpUrl())
            .head() // 使用 HEAD 请求减少数据传输
            .build()

        buvidClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                console().sendWarn("buvidHeaderRequestFailed", e.message ?: "")
                future.complete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    try {
                        val setCookieHeaders = resp.headers("Set-Cookie")
                        for (setCookieHeader in setCookieHeaders) {
                            if (setCookieHeader.startsWith("buvid3=")) {
                                val buvid3 = setCookieHeader.substringAfter("buvid3=").substringBefore(";")
                                console().sendInfo("buvidObtained", "buvid3 (header)", buvid3)
                                future.complete(buvid3)
                                return
                            }
                        }
                        console().sendWarn("buvidNotFoundInHeader")
                        future.complete(null)
                    } catch (e: Exception) {
                        console().sendWarn("buvidHeaderParseFailed", e.message ?: "")
                        future.complete(null)
                    }
                }
            }
        })

        return future
    }

    /**
     * 同步获取 buvid3
     * 注意：此方法会阻塞当前线程，建议在异步环境中使用
     */
    fun getBuvid3Sync(): String? {
        return getBuvid3Async().get()
    }

    /**
     * 同步获取 buvid3 和 buvid4
     * 注意：此方法会阻塞当前线程，建议在异步环境中使用
     */
    fun getBuvidSync(): Pair<String?, String?> {
        return getBuvidAsync().get()
    }

    /**
     * 为指定用户自动获取并设置 buvid
     * @param playerUuid 用户UUID
     * @return CompletableFuture<Boolean> 是否成功设置
     */
    fun ensureBuvid(playerUuid: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        // 检查是否已经有 buvid3
        val existingBuvid3 = BilibiliCookieJar.getCookie(playerUuid, "buvid3")
        if (existingBuvid3 != null && existingBuvid3.isNotEmpty()) {
            // 已经有 buvid3，无需重新获取
            future.complete(true)
            return future
        }

        // 尝试获取 buvid
        getBuvidAsync().thenAccept { (buvid3, buvid4) ->
            var success = false

            if (buvid3 != null) {
                BilibiliCookieJar.setCookie(playerUuid, "buvid3", buvid3)
                success = true
            }

            if (buvid4 != null) {
                BilibiliCookieJar.setCookie(playerUuid, "buvid4", buvid4)
                success = true
            }

            if (success) {
                console().sendInfo("buvidSetSuccess", playerUuid)
            } else {
                console().sendWarn("buvidSetFailed", playerUuid)
            }

            future.complete(success)
        }.exceptionally { throwable ->
            console().sendWarn("buvidEnsureFailed", playerUuid, throwable.message ?: "")
            future.complete(false)
            null
        }

        return future
    }

    /**
     * 为当前活动用户自动获取并设置 buvid
     * @return CompletableFuture<Boolean> 是否成功设置
     */
    fun ensureBuvid(): CompletableFuture<Boolean> {
        val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        return if (playerUuid != null) {
            ensureBuvid(playerUuid)
        } else {
            CompletableFuture.completedFuture(false)
        }
    }

    /**
     * 验证 buvid 是否有效
     * @param buvid buvid 值
     * @return 是否有效
     */
    fun isValidBuvid(buvid: String?): Boolean {
        return buvid != null && buvid.isNotEmpty() && buvid.length > 10
    }
}

/**
 * User-Agent 拦截器
 * 为请求添加合适的 User-Agent，避免敏感关键词
 */
private class BuvidUserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestWithUserAgent = originalRequest.newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        return chain.proceed(requestWithUserAgent)
    }
}