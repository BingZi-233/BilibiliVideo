package online.bingzi.bilibili.video.internal.http

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * B 站 HTTP 底座。
 *
 * - 统一 OkHttpClient 配置（超时等）
 * - 统一 JSON 解析（Gson）
 * - 负责根据凭证构建 Cookie / Header
 */
internal object BilibiliHttpClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = Gson()

    private val jsonMediaType = "application/json;charset=UTF-8".toMediaType()

    inline fun <reified T> get(
        url: String,
        credentialCookieHeader: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", defaultUserAgent())
            .header("Referer", "https://www.bilibili.com/")
            .header("Cookie", credentialCookieHeader)
            .apply {
                headers.forEach { (k, v) -> header(k, v) }
            }
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                error("HTTP GET failed: $url, code=${resp.code}, body=$body")
            }
            val type = object : TypeToken<T>() {}.type
            return gson.fromJson(body, type)
        }
    }

    inline fun <reified T> postForm(
        url: String,
        credentialCookieHeader: String,
        form: Map<String, String>,
        headers: Map<String, String> = emptyMap()
    ): T {
        val formBody = form.entries.joinToString("&") { (k, v) ->
            "${k}=${v}"
        }.toRequestBody("application/x-www-form-urlencoded;charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", defaultUserAgent())
            .header("Referer", "https://www.bilibili.com/")
            .header("Cookie", credentialCookieHeader)
            .apply {
                headers.forEach { (k, v) -> header(k, v) }
            }
            .post(formBody)
            .build()

        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                error("HTTP POST failed: $url, code=${resp.code}, body=$body")
            }
            val type = object : TypeToken<T>() {}.type
            return gson.fromJson(body, type)
        }
    }

    fun buildCookieHeader(sessData: String, biliJct: String, buvid3: String?, accessKey: String?): String {
        val list = mutableListOf<String>()
        list += "SESSDATA=$sessData"
        list += "bili_jct=$biliJct"
        buvid3?.let { list += "buvid3=$it" }
        accessKey?.let { list += "access_key=$it" }
        return list.joinToString("; ")
    }

    private fun defaultUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
    }
}
