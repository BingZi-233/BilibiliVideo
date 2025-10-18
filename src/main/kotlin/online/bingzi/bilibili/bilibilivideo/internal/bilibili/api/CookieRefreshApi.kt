package online.bingzi.bilibili.bilibilivideo.internal.bilibili.api

import com.google.gson.Gson
import okhttp3.FormBody
import okhttp3.Request
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.HttpClientFactory
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.client.ResponseHandler
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.helper.RSAOAEPHelper
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ApiResult
import taboolib.common.platform.function.submitAsync
import taboolib.common.platform.function.warning
import java.util.regex.Pattern

/**
 * Bilibili Cookie 刷新 API 工具类
 *
 * 提供完整的 Cookie 刷新流程实现，支持自动续期用户登录状态。
 * 刷新流程包含风控机制，需要经过检查、加密、刷新、确认四个步骤。
 * 所有网络请求均为异步执行，通过回调函数返回结果。
 *
 * 主要功能：
 * - 检查 Cookie 是否需要刷新
 * - 执行完整的 Cookie 刷新流程
 * - 自动提取新的认证信息
 * - 风控机制处理
 *
 * 刷新流程：
 * 1. 检查刷新需求，获取服务器时间戳
 * 2. 使用 RSA-OAEP 加密时间戳生成 CorrespondPath
 * 3. 访问 Correspond 页面获取 refresh_csrf
 * 4. 使用 refresh_token 和 refresh_csrf 执行刷新
 * 5. 确认刷新完成（使用旧的 refresh_token）
 *
 * @since 1.0.0
 * @author BingZi-233
 */
object CookieRefreshApi {

    /** Cookie 信息检查 API 端点 */
    private const val COOKIE_INFO_URL = "https://passport.bilibili.com/x/passport-login/web/cookie/info"
    /** Correspond 页面基础 URL */
    private const val CORRESPOND_BASE_URL = "https://www.bilibili.com/correspond/1/"
    /** Cookie 刷新 API 端点 */
    private const val COOKIE_REFRESH_URL = "https://passport.bilibili.com/x/passport-login/web/cookie/refresh"
    /** 确认刷新 API 端点 */
    private const val CONFIRM_REFRESH_URL = "https://passport.bilibili.com/x/passport-login/web/confirm/refresh"

    /** 用于从 HTML 提取 refresh_csrf 的正则表达式 */
    private val REFRESH_CSRF_PATTERN = Pattern.compile("<div id=\"1-name\">([^<]+)</div>")

    /** JSON 解析器实例 */
    private val gson = Gson()

    /**
     * 检查 Cookie 是否需要刷新
     *
     * 查询服务器判断当前 Cookie 是否需要刷新，并获取用于后续加密的时间戳。
     *
     * @param sessdata 用户 SESSDATA Cookie
     * @param buvid3 设备标识 Cookie
     * @param biliJct CSRF 保护令牌
     * @param callback 结果回调，成功时返回检查数据，失败时返回失败信息
     */
    fun checkRefreshNeeded(
        sessdata: String,
        buvid3: String,
        biliJct: String,
        callback: (ApiResult<CookieRefreshCheckData>) -> Unit
    ) {
        submitAsync {
            try {
                val client = HttpClientFactory.createCustomClient(sessdata, buvid3, biliJct)

                val url = if (biliJct.isNotEmpty()) {
                    "$COOKIE_INFO_URL?csrf=$biliJct"
                } else {
                    COOKIE_INFO_URL
                }

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val result = ResponseHandler.handleResponse<CookieInfoResponse>(response)

                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        if (data.code == 0 && data.data != null) {
                            callback(ApiResult.Success(data.data))
                        } else {
                            callback(
                                ApiResult.Failure(
                                    errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                                    message = "检查刷新需求失败: ${data.message}"
                                )
                            )
                        }
                    }
                    is ApiResult.Failure -> {
                        callback(result)
                    }
                }
            } catch (e: Exception) {
                warning("检查 Cookie 刷新需求异常: ${e.message}")
                callback(ResponseHandler.handleException(e))
            }
        }
    }

    /**
     * 执行完整的 Cookie 刷新流程
     *
     * 自动完成检查、加密、刷新、确认的完整流程，并返回新的认证信息。
     * 如果检查发现不需要刷新，则返回原 Cookie 信息。
     *
     * @param sessdata 当前的 SESSDATA Cookie
     * @param buvid3 设备标识 Cookie
     * @param biliJct 当前的 bili_jct
     * @param refreshToken 刷新令牌
     * @param callback 结果回调，成功时返回新的认证信息
     */
    fun refreshCookie(
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String,
        callback: (ApiResult<CookieRefreshResult>) -> Unit
    ) {
        submitAsync {
            try {
                // 步骤 1: 检查是否需要刷新
                val checkResult = checkRefreshInternal(sessdata, buvid3, biliJct)
                if (checkResult == null) {
                    callback(
                        ApiResult.Failure(
                            errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.NETWORK_UNKNOWN,
                            message = "检查刷新需求失败"
                        )
                    )
                    return@submitAsync
                }

                // 如果不需要刷新，返回原 Cookie
                if (!checkResult.refresh) {
                    callback(
                        ApiResult.Success(
                            CookieRefreshResult(
                                needRefresh = false,
                                newSessdata = sessdata,
                                newBiliJct = biliJct,
                                newRefreshToken = refreshToken
                            )
                        )
                    )
                    return@submitAsync
                }

                // 步骤 2: 生成 CorrespondPath
                val correspondPath = RSAOAEPHelper.generateCorrespondPath(checkResult.timestamp)
                if (correspondPath.isEmpty()) {
                    callback(
                        ApiResult.Failure(
                            errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                            message = "生成 CorrespondPath 失败"
                        )
                    )
                    return@submitAsync
                }

                // 步骤 3: 获取 refresh_csrf
                val refreshCsrf = getRefreshCsrf(sessdata, buvid3, correspondPath)
                if (refreshCsrf.isEmpty()) {
                    callback(
                        ApiResult.Failure(
                            errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                            message = "获取 refresh_csrf 失败"
                        )
                    )
                    return@submitAsync
                }

                // 步骤 4: 执行刷新
                val performResult = performRefresh(sessdata, buvid3, biliJct, refreshToken, refreshCsrf)
                when (performResult) {
                    is ApiResult.Success -> {
                        val refreshData = performResult.data

                        // 步骤 5: 确认刷新（使用旧的 refresh_token）
                        val confirmResult = confirmRefresh(
                            refreshData.newSessdata,
                            refreshData.newBiliJct,
                            refreshToken
                        )

                        // 确认失败不影响刷新结果，只记录警告
                        when (confirmResult) {
                            is ApiResult.Success -> {
                                // 确认成功
                            }
                            is ApiResult.Failure -> {
                                warning("确认 Cookie 刷新失败，但刷新流程已完成: ${confirmResult.message}")
                            }
                        }

                        // 返回刷新结果
                        callback(ApiResult.Success(refreshData))
                    }
                    is ApiResult.Failure -> {
                        callback(performResult)
                    }
                }

            } catch (e: Exception) {
                warning("执行 Cookie 刷新流程异常: ${e.message}")
                callback(ResponseHandler.handleException(e))
            }
        }
    }

    /**
     * 内部检查刷新需求（同步方法）
     *
     * @param sessdata 用户 SESSDATA Cookie
     * @param buvid3 设备标识 Cookie
     * @param biliJct CSRF 保护令牌
     * @return 检查结果数据，失败时返回 null
     */
    private fun checkRefreshInternal(
        sessdata: String,
        buvid3: String,
        biliJct: String
    ): CookieRefreshCheckData? {
        return try {
            val client = HttpClientFactory.createCustomClient(sessdata, buvid3, biliJct)

            val url = if (biliJct.isNotEmpty()) {
                "$COOKIE_INFO_URL?csrf=$biliJct"
            } else {
                COOKIE_INFO_URL
            }

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            val result = ResponseHandler.handleResponse<CookieInfoResponse>(response)

            when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (data.code == 0 && data.data != null) {
                        data.data
                    } else {
                        warning("检查刷新需求失败: ${data.message}")
                        null
                    }
                }
                is ApiResult.Failure -> {
                    warning("检查刷新需求失败: ${result.message}")
                    null
                }
            }
        } catch (e: Exception) {
            warning("检查刷新需求异常: ${e.message}")
            null
        }
    }

    /**
     * 获取 refresh_csrf（同步方法）
     *
     * 访问 Correspond 页面，从 HTML 中提取 refresh_csrf 令牌。
     *
     * @param sessdata 用户 SESSDATA Cookie
     * @param buvid3 设备标识 Cookie
     * @param correspondPath 加密后的路径
     * @return refresh_csrf 字符串，失败时返回空字符串
     */
    private fun getRefreshCsrf(
        sessdata: String,
        buvid3: String,
        correspondPath: String
    ): String {
        return try {
            val client = HttpClientFactory.createCustomClient(sessdata, buvid3)
            val url = "$CORRESPOND_BASE_URL$correspondPath"

            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val matcher = REFRESH_CSRF_PATTERN.matcher(html)
                if (matcher.find()) {
                    matcher.group(1) ?: ""
                } else {
                    warning("在 HTML 中未找到 refresh_csrf")
                    ""
                }
            } else {
                warning("获取 refresh_csrf 页面失败: ${response.code}")
                ""
            }
        } catch (e: Exception) {
            warning("获取 refresh_csrf 异常: ${e.message}")
            ""
        }
    }

    /**
     * 执行 Cookie 刷新（同步方法）
     *
     * 向 Bilibili 服务器提交刷新请求，获取新的 Cookie 和 refresh_token。
     *
     * @param sessdata 当前的 SESSDATA Cookie
     * @param buvid3 设备标识 Cookie
     * @param biliJct 当前的 bili_jct
     * @param refreshToken 刷新令牌
     * @param refreshCsrf 从 Correspond 页面获取的 CSRF 令牌
     * @return 刷新结果，包含新的认证信息
     */
    private fun performRefresh(
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String,
        refreshCsrf: String
    ): ApiResult<CookieRefreshResult> {
        return try {
            val client = HttpClientFactory.createCustomClient(sessdata, buvid3, biliJct)

            val formBody = FormBody.Builder()
                .add("csrf", biliJct)
                .add("refresh_csrf", refreshCsrf)
                .add("source", "main_web")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url(COOKIE_REFRESH_URL)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val refreshResponse = gson.fromJson(responseBody, CookieRefreshResponse::class.java)

                    if (refreshResponse.code == 0 && refreshResponse.data != null) {
                        // 从 Set-Cookie 头提取新的 Cookie
                        val cookieHeaders = response.headers("Set-Cookie")
                        val cookieMap = parseCookies(cookieHeaders)

                        val newSessdata = cookieMap["SESSDATA"] ?: sessdata
                        val newBiliJct = cookieMap["bili_jct"] ?: biliJct
                        val newRefreshToken = refreshResponse.data.refresh_token

                        ApiResult.Success(
                            CookieRefreshResult(
                                needRefresh = true,
                                newSessdata = newSessdata,
                                newBiliJct = newBiliJct,
                                newRefreshToken = newRefreshToken
                            )
                        )
                    } else {
                        ApiResult.Failure(
                            errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                            message = "Cookie 刷新响应失败: ${refreshResponse.message}"
                        )
                    }
                } else {
                    ApiResult.Failure(
                        errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                        message = "Cookie 刷新响应体为空"
                    )
                }
            } else {
                ApiResult.Failure(
                    errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.NETWORK_UNKNOWN,
                    message = "Cookie 刷新请求失败: ${response.code}",
                    httpCode = response.code
                )
            }
        } catch (e: Exception) {
            warning("执行 Cookie 刷新异常: ${e.message}")
            ResponseHandler.handleException(e)
        }
    }

    /**
     * 确认 Cookie 刷新（同步方法）
     *
     * 向 Bilibili 服务器确认刷新操作已完成。
     * 注意：此步骤使用旧的 refresh_token 进行确认。
     *
     * @param newSessdata 新的 SESSDATA Cookie
     * @param newBiliJct 新的 bili_jct
     * @param oldRefreshToken 旧的 refresh_token
     * @return 确认结果
     */
    private fun confirmRefresh(
        newSessdata: String,
        newBiliJct: String,
        oldRefreshToken: String
    ): ApiResult<Unit> {
        return try {
            val client = HttpClientFactory.createCustomClient(newSessdata, null, newBiliJct)

            val formBody = FormBody.Builder()
                .add("csrf", newBiliJct)
                .add("refresh_token", oldRefreshToken)
                .build()

            val request = Request.Builder()
                .url(CONFIRM_REFRESH_URL)
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val result = ResponseHandler.handleResponse<ConfirmRefreshResponse>(response)

            when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    if (data.code == 0) {
                        ApiResult.Success(Unit)
                    } else {
                        ApiResult.Failure(
                            errorCode = online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode.UNKNOWN_ERROR,
                            message = "确认刷新失败: ${data.message}"
                        )
                    }
                }
                is ApiResult.Failure -> {
                    result
                }
            }
        } catch (e: Exception) {
            warning("确认刷新异常: ${e.message}")
            ResponseHandler.handleException(e)
        }
    }

    /**
     * 解析 Set-Cookie 响应头
     *
     * 从 HTTP 响应头中提取 Cookie 的键值对。
     * 只提取 Cookie 的名称和值，忽略 Path、Domain 等属性。
     *
     * @param cookieHeaders Set-Cookie 响应头列表
     * @return Cookie 键值对映射
     */
    private fun parseCookies(cookieHeaders: List<String>): Map<String, String> {
        val cookieMap = mutableMapOf<String, String>()

        cookieHeaders.forEach { cookieHeader ->
            // Set-Cookie 格式: "SESSDATA=xxx; Path=/; Domain=.bilibili.com; ..."
            // 只提取第一个分号之前的部分
            val cookiePart = cookieHeader.split(";")[0].trim()
            val parts = cookiePart.split("=", limit = 2)

            if (parts.size == 2) {
                cookieMap[parts[0].trim()] = parts[1].trim()
            }
        }

        return cookieMap
    }
}

// ==================== 数据模型定义 ====================

/**
 * Cookie 信息检查响应
 *
 * @property code 响应码，0 表示成功
 * @property message 响应消息
 * @property data 检查结果数据
 */
data class CookieInfoResponse(
    val code: Int,
    val message: String,
    val data: CookieRefreshCheckData?
)

/**
 * Cookie 刷新检查数据
 *
 * @property refresh 是否需要刷新
 * @property timestamp 服务器时间戳（毫秒），用于后续加密
 */
data class CookieRefreshCheckData(
    val refresh: Boolean,
    val timestamp: Long
)

/**
 * Cookie 刷新响应
 *
 * @property code 响应码，0 表示成功
 * @property message 响应消息
 * @property data 刷新结果数据
 */
data class CookieRefreshResponse(
    val code: Int,
    val message: String,
    val data: CookieRefreshData?
)

/**
 * Cookie 刷新数据
 *
 * @property refresh_token 新的刷新令牌
 */
data class CookieRefreshData(
    val refresh_token: String
)

/**
 * 确认刷新响应
 *
 * @property code 响应码，0 表示成功
 * @property message 响应消息
 */
data class ConfirmRefreshResponse(
    val code: Int,
    val message: String
)

/**
 * Cookie 刷新结果
 *
 * 封装刷新完成后的所有认证信息。
 *
 * @property needRefresh 是否执行了刷新（false 表示 Cookie 无需刷新）
 * @property newSessdata 新的 SESSDATA Cookie
 * @property newBiliJct 新的 bili_jct
 * @property newRefreshToken 新的刷新令牌
 */
data class CookieRefreshResult(
    val needRefresh: Boolean,
    val newSessdata: String,
    val newBiliJct: String,
    val newRefreshToken: String
)
