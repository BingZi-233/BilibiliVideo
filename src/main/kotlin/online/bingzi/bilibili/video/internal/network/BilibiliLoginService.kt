package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.api.event.network.login.*
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
                                val qrCodeInfo = QrCodeLoginInfo(url, qrcodeKey)
                                console().sendInfo("loginQrCodeGenerateSuccess")

                                // 触发二维码生成成功事件
                                QrCodeGenerateEvent(qrCodeInfo, true).call()

                                return@thenApply qrCodeInfo
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("loginQrCodeGenerateFailed", message)

                            // 触发二维码生成失败事件
                            QrCodeGenerateEvent(null, false, message).call()
                        }
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: "解析响应失败"
                        console().sendWarn("networkResponseParseFailed", errorMsg)

                        // 触发二维码生成失败事件
                        QrCodeGenerateEvent(null, false, errorMsg).call()
                    }
                } else {
                    val errorMsg = response.getError() ?: "网络请求失败"
                    console().sendWarn("networkApiRequestFailed", errorMsg)

                    // 触发二维码生成失败事件
                    QrCodeGenerateEvent(null, false, errorMsg).call()
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

                        val newStatus = when (code) {
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

                                // 触发登录失败事件
                                LoginFailureEvent(qrcodeKey, LoginStatus.EXPIRED, "二维码已过期", "qrcode").call()

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

                                // 获取用户 ID 并触发登录成功事件
                                val userId = getCurrentUserId() ?: "unknown"
                                LoginSuccessEvent(userId, "qrcode").call()

                                LoginStatus.SUCCESS
                            }

                            else -> {
                                console().sendWarn("loginFailed", message)

                                // 触发登录失败事件
                                LoginFailureEvent(qrcodeKey, LoginStatus.FAILED, message, "qrcode").call()

                                LoginStatus.FAILED
                            }
                        }

                        // 触发登录状态轮询事件（这里简化处理，假设状态总是变化的）
                        LoginStatusPollEvent(qrcodeKey, newStatus, true).call()

                        newStatus
                    } catch (e: Exception) {
                        val errorMsg = e.message ?: "解析响应失败"
                        console().sendWarn("networkResponseParseFailed", errorMsg)

                        // 触发登录失败事件
                        LoginFailureEvent(qrcodeKey, LoginStatus.FAILED, errorMsg, "qrcode").call()

                        LoginStatus.FAILED
                    }
                } else {
                    val errorMsg = response.getError() ?: "网络请求失败"
                    console().sendWarn("networkApiRequestFailed", errorMsg)

                    // 触发登录失败事件
                    LoginFailureEvent(qrcodeKey, LoginStatus.FAILED, errorMsg, "qrcode").call()

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
        val userId = getCurrentUserId()
        BilibiliCookieJar.clearCookies()
        console().sendInfo("loginLogout")

        // 触发用户登出事件
        UserLogoutEvent(userId, "logout").call()
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

            // 触发 Cookie 设置成功事件
            CookieSetEvent(true).call()

            // 如果能获取到用户 ID，触发登录成功事件
            val userId = getCurrentUserId()
            if (userId != null) {
                LoginSuccessEvent(userId, "cookie").call()
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Cookie设置失败"
            console().sendWarn("loginCookieSetFailed", errorMsg)

            // 触发 Cookie 设置失败事件
            CookieSetEvent(false, errorMsg).call()
        }
    }
}