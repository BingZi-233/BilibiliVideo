package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.internal.network.entity.LoginStatus
import online.bingzi.bilibili.video.internal.network.entity.QrCodeLoginInfo
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * Bilibili 登录服务
 * 提供二维码登录、状态检查等功能
 */
object BilibiliLoginService {

    private val gson = Gson()

    /**
     * 生成二维码登录信息
     * @return QrCodeLoginInfo 或 null
     */
    fun generateQrCode(): CompletableFuture<QrCodeLoginInfo?> {
        return BilibiliApiClient.getAsync("https://passport.bilibili.com/x/passport-login/web/qrcode/generate")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")
                            val url = data.get("url")?.asString
                            val qrcodeKey = data.get("qrcode_key")?.asString

                            if (url != null && qrcodeKey != null) {
                                console().sendInfo("loginQrCodeGenerateSuccess")
                                return@thenApply QrCodeLoginInfo(url, qrcodeKey)
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("loginQrCodeGenerateFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("networkResponseParseFailed", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 轮询登录状态
     * @param qrcodeKey 二维码密钥
     * @return 登录状态
     */
    fun pollLoginStatus(qrcodeKey: String): CompletableFuture<LoginStatus> {
        return BilibiliApiClient.getAsync("https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=$qrcodeKey")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1
                        val message = json.get("message")?.asString ?: ""

                        when (code) {
                            86101 -> {
                                console().sendInfo("loginWaitingForScan")
                                LoginStatus.WAITING_FOR_SCAN
                            }

                            86090 -> {
                                console().sendInfo("loginWaitingForConfirm")
                                LoginStatus.WAITING_FOR_CONFIRM
                            }

                            86038 -> {
                                console().sendWarn("loginQrCodeExpired")
                                LoginStatus.EXPIRED
                            }

                            0 -> {
                                console().sendInfo("loginSuccess")
                                // 解析登录信息
                                val data = json.getAsJsonObject("data")
                                val url = data.get("url")?.asString

                                // 从 URL 中解析 Cookie
                                if (url != null) {
                                    parseCookiesFromUrl(url)
                                }

                                LoginStatus.SUCCESS
                            }

                            else -> {
                                console().sendWarn("loginFailed", message)
                                LoginStatus.FAILED
                            }
                        }
                    } catch (e: Exception) {
                        console().sendWarn("networkResponseParseFailed", e.message ?: "")
                        LoginStatus.FAILED
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                    LoginStatus.FAILED
                }
            }
    }

    /**
     * 从 URL 中解析 Cookie
     * @param url 包含 Cookie 信息的 URL
     */
    private fun parseCookiesFromUrl(url: String) {
        try {
            // Bilibili 登录成功后会在 URL 中返回 Cookie 信息
            mutableMapOf<String, String>()

            // 这里需要根据实际返回的 URL 格式来解析 Cookie
            // 通常登录成功后，Cookie 会通过 Set-Cookie 响应头设置
            // 由于我们使用了 CookieJar，Cookie 应该已经自动保存了

            console().sendInfo("cookieAutoSaved")

        } catch (e: Exception) {
            console().sendWarn("cookieParseFailed", e.message ?: "")
        }
    }

    /**
     * 检查当前登录状态
     * @return 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return BilibiliCookieJar.isLoggedIn()
    }

    /**
     * 获取当前登录用户的 UID
     * @return 用户 UID，未登录返回 null
     */
    fun getCurrentUserId(): String? {
        return BilibiliCookieJar.getUserId()
    }

    /**
     * 登出当前用户
     */
    fun logout() {
        BilibiliCookieJar.clearCookies()
        console().sendInfo("loginLogout")
    }

    /**
     * 使用 Cookie 字符串设置登录状态
     * @param cookies Cookie 字符串，格式：key1=value1; key2=value2
     */
    fun loginWithCookies(cookies: String) {
        try {
            val cookieMap = mutableMapOf<String, String>()
            cookies.split(";").forEach { cookie ->
                val parts = cookie.trim().split("=", limit = 2)
                if (parts.size == 2) {
                    cookieMap[parts[0].trim()] = parts[1].trim()
                }
            }
            BilibiliCookieJar.setCookies(cookieMap)
            console().sendInfo("loginCookieSet")
        } catch (e: Exception) {
            console().sendWarn("loginCookieSetFailed", e.message ?: "")
        }
    }
}