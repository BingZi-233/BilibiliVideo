package online.bingzi.bilibili.video.internal.network

import com.google.gson.JsonParser
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * WBI 签名服务
 * 用于处理 Bilibili API 的 WBI 签名鉴权
 * WBI 签名是 B 站 Web 端部分接口采用的一种鉴权方式
 */
object WbiService {

    /**
     * MIXIN_KEY_ENC_TAB 打乱重排表
     * 用于将 imgKey + subKey 打乱重排生成 mixinKey
     */
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )

    /**
     * WBI 密钥对数据类
     */
    data class WbiKeys(
        val imgKey: String,
        val subKey: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * 检查密钥是否过期（超过23小时）
         */
        fun isExpired(): Boolean {
            return System.currentTimeMillis() - timestamp > TimeUnit.HOURS.toMillis(23)
        }

        /**
         * 生成 mixinKey
         * 将 imgKey 和 subKey 拼接后，按照 MIXIN_KEY_ENC_TAB 重排，取前32位
         */
        fun getMixinKey(): String {
            val rawKey = imgKey + subKey
            return buildString {
                for (i in 0 until 32) {
                    append(rawKey[MIXIN_KEY_ENC_TAB[i]])
                }
            }
        }
    }

    /**
     * 缓存的 WBI 密钥
     * 使用 playerUuid 作为 key，为每个用户单独缓存密钥
     */
    private val wbiKeysCache = ConcurrentHashMap<String, WbiKeys>()

    /**
     * 获取当前用户的 WBI 密钥
     * @param forceRefresh 是否强制刷新
     * @return WBI 密钥对或 null
     */
    fun getWbiKeys(forceRefresh: Boolean = false): CompletableFuture<WbiKeys?> {
        val playerUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        if (playerUuid == null) {
            console().sendWarn("wbiNoCurrentUser")
            return CompletableFuture.completedFuture(null)
        }

        return getWbiKeysForUser(playerUuid, forceRefresh)
    }

    /**
     * 获取指定用户的 WBI 密钥
     * @param playerUuid 玩家 UUID
     * @param forceRefresh 是否强制刷新
     * @return WBI 密钥对或 null
     */
    fun getWbiKeysForUser(playerUuid: String, forceRefresh: Boolean = false): CompletableFuture<WbiKeys?> {
        // 检查缓存
        if (!forceRefresh) {
            val cached = wbiKeysCache[playerUuid]
            if (cached != null && !cached.isExpired()) {
                console().sendInfo("wbiKeysUsedCache", playerUuid)
                return CompletableFuture.completedFuture(cached)
            }
        }

        // 从 nav 接口获取
        return fetchWbiKeysFromNav(playerUuid)
    }

    /**
     * 从 nav 接口获取 WBI 密钥
     * @param playerUuid 玩家 UUID
     * @return WBI 密钥对或 null
     */
    private fun fetchWbiKeysFromNav(playerUuid: String): CompletableFuture<WbiKeys?> {
        // 临时切换到指定用户的 Cookie
        val originalUuid = BilibiliCookieJar.getCurrentPlayerUuid()
        BilibiliCookieJar.switchUser(playerUuid)

        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/nav")
            .thenApply { response ->
                try {
                    if (response.isSuccess()) {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")
                            val wbiImg = data.getAsJsonObject("wbi_img")

                            if (wbiImg != null) {
                                val imgUrl = wbiImg.get("img_url")?.asString
                                val subUrl = wbiImg.get("sub_url")?.asString

                                if (imgUrl != null && subUrl != null) {
                                    // 提取文件名部分（去掉 .png 后缀）
                                    val imgKey = imgUrl.substringAfterLast("/").substringBeforeLast(".")
                                    val subKey = subUrl.substringAfterLast("/").substringBeforeLast(".")

                                    val keys = WbiKeys(imgKey, subKey)
                                    
                                    // 缓存密钥
                                    wbiKeysCache[playerUuid] = keys
                                    
                                    console().sendInfo("wbiKeysFetched", playerUuid)
                                    return@thenApply keys
                                } else {
                                    console().sendWarn("wbiKeysNotFound")
                                }
                            } else {
                                console().sendWarn("wbiImgNotFound")
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("wbiKeysFetchFailed", message)
                        }
                    } else {
                        console().sendWarn("wbiKeysFetchNetworkError", response.getError() ?: "")
                    }
                } catch (e: Exception) {
                    console().sendWarn("wbiKeysParseError", e.message ?: "")
                } finally {
                    // 恢复原来的用户
                    if (originalUuid != null && originalUuid != playerUuid) {
                        BilibiliCookieJar.switchUser(originalUuid)
                    }
                }
                null
            }
    }

    /**
     * 对请求参数进行 WBI 签名
     * @param params 原始请求参数
     * @param wbiKeys WBI 密钥对（如果为 null，将自动获取）
     * @return 签名后的参数 Map
     */
    fun signParams(params: Map<String, Any?>, wbiKeys: WbiKeys? = null): CompletableFuture<Map<String, String>> {
        val keysFuture = if (wbiKeys != null) {
            CompletableFuture.completedFuture(wbiKeys)
        } else {
            getWbiKeys()
        }

        return keysFuture.thenApply { keys ->
            if (keys == null) {
                console().sendWarn("wbiSignNoKeys")
                return@thenApply emptyMap<String, String>()
            }

            try {
                // 过滤掉 null 值，并转换为 String
                val filteredParams = params.filterValues { it != null }
                    .mapValues { it.value.toString() }
                    .toMutableMap()

                // 添加 wts 时间戳（秒）
                val wts = System.currentTimeMillis() / 1000
                filteredParams["wts"] = wts.toString()

                // 按键名升序排序
                val sortedParams = filteredParams.toSortedMap()

                // 构建查询字符串
                val queryString = buildQueryString(sortedParams)

                // 获取 mixinKey
                val mixinKey = keys.getMixinKey()

                // 计算 w_rid (MD5)
                val wrid = md5(queryString + mixinKey)

                // 添加 w_rid 到参数中
                sortedParams["w_rid"] = wrid

                console().sendInfo("wbiSignSuccess")
                return@thenApply sortedParams
            } catch (e: Exception) {
                console().sendWarn("wbiSignError", e.message ?: "")
                return@thenApply emptyMap<String, String>()
            }
        }
    }

    /**
     * 构建符合 WBI 签名要求的查询字符串
     * @param params 参数 Map
     * @return URL 编码的查询字符串
     */
    private fun buildQueryString(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "${encodeURIComponent(key)}=${encodeURIComponent(value)}"
        }
    }

    /**
     * URL 编码组件
     * 符合 WBI 签名的编码要求：
     * - 字母大写
     * - 空格编码为 %20
     * - 过滤 !'()* 字符
     * @param value 要编码的值
     * @return 编码后的字符串
     */
    private fun encodeURIComponent(value: String): String {
        // 使用 URLEncoder 进行基础编码
        var encoded = URLEncoder.encode(value, "UTF-8")
        
        // 将 + 替换为 %20（空格的正确编码）
        encoded = encoded.replace("+", "%20")
        
        // 还原不需要编码的字符（过滤 !'()* ）
        encoded = encoded.replace("%21", "!")
        encoded = encoded.replace("%27", "'")
        encoded = encoded.replace("%28", "(")
        encoded = encoded.replace("%29", ")")
        encoded = encoded.replace("%2A", "*")
        
        return encoded
    }

    /**
     * 计算字符串的 MD5 哈希值
     * @param input 输入字符串
     * @return MD5 哈希值（小写十六进制）
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * 清除指定用户的 WBI 密钥缓存
     * @param playerUuid 玩家 UUID
     */
    fun clearCache(playerUuid: String) {
        wbiKeysCache.remove(playerUuid)
        console().sendInfo("wbiCacheCleared", playerUuid)
    }

    /**
     * 清除所有 WBI 密钥缓存
     */
    fun clearAllCache() {
        wbiKeysCache.clear()
        console().sendInfo("wbiAllCacheCleared")
    }

    /**
     * 构建带 WBI 签名的 URL
     * @param baseUrl 基础 URL
     * @param params 请求参数
     * @return 带签名参数的完整 URL
     */
    fun buildSignedUrl(baseUrl: String, params: Map<String, Any?>): CompletableFuture<String> {
        return signParams(params).thenApply { signedParams ->
            if (signedParams.isEmpty()) {
                return@thenApply baseUrl
            }

            val queryString = buildQueryString(signedParams)
            val separator = if (baseUrl.contains("?")) "&" else "?"
            return@thenApply "$baseUrl$separator$queryString"
        }
    }

    /**
     * 检查 URL 是否需要 WBI 签名
     * @param url 请求 URL
     * @return 是否需要 WBI 签名
     */
    fun isWbiRequired(url: String): Boolean {
        // 需要 WBI 签名的 API 路径列表
        val wbiRequiredPaths = listOf(
            "/x/v2/reply/wbi/main",          // 评论区主评论（新版）
            "/x/v2/reply/reply",              // 评论区回复
            "/x/web-interface/wbi/search",   // 搜索相关
            "/x/space/wbi/",                 // 用户空间相关
            "/x/player/wbi/",                // 播放器相关
            "/wbi/",                          // 其他 WBI 接口
            "/x/web-interface/popular",      // 热门视频
            "/x/web-interface/ranking"       // 排行榜
        )

        return wbiRequiredPaths.any { path -> url.contains(path) }
    }
}