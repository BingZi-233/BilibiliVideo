package online.bingzi.bilibili.bilibilivideo.internal.bilibili.client

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.Response
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ApiResult
import online.bingzi.bilibili.bilibilivideo.internal.bilibili.model.ErrorCode
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * HTTP 响应处理器
 *
 * 提供统一的响应解析和错误处理机制
 */
object ResponseHandler {

    /**
     * 处理 HTTP 响应并转换为 ApiResult
     *
     * @param T 目标数据类型
     * @param response OkHttp 响应对象
     * @return API 结果封装
     */
    inline fun <reified T> handleResponse(response: Response): ApiResult<T> {
        val gson = Gson()
        return try {
            // 检查 HTTP 状态码
            if (!response.isSuccessful) {
                val errorCode = when (response.code) {
                    401, 403 -> ErrorCode.AUTH_FAILED
                    404 -> ErrorCode.RESOURCE_NOT_FOUND
                    429 -> ErrorCode.RATE_LIMITED
                    400 -> ErrorCode.INVALID_PARAMETER
                    else -> ErrorCode.UNKNOWN_ERROR
                }
                return ApiResult.Failure(
                    errorCode = errorCode,
                    message = "HTTP 请求失败: ${response.code} ${response.message}",
                    httpCode = response.code
                )
            }

            // 提取响应体
            val responseBody = response.body?.string()
            if (responseBody.isNullOrEmpty()) {
                return ApiResult.Failure(
                    errorCode = ErrorCode.UNKNOWN_ERROR,
                    message = "响应体为空",
                    httpCode = response.code
                )
            }

            // 解析 JSON
            try {
                val data = gson.fromJson(responseBody, T::class.java)
                ApiResult.Success(data)
            } catch (e: JsonSyntaxException) {
                ApiResult.Failure(
                    errorCode = ErrorCode.JSON_PARSE_ERROR,
                    message = "JSON 解析失败: ${e.message}",
                    cause = e,
                    httpCode = response.code
                )
            }
        } catch (e: Exception) {
            handleException(e)
        } finally {
            response.close()
        }
    }

    /**
     * 处理异常并转换为失败结果
     *
     * @param e 异常对象
     * @return 失败结果
     */
    fun handleException(e: Throwable): ApiResult.Failure {
        return when (e) {
            is SocketTimeoutException -> ApiResult.Failure(
                errorCode = ErrorCode.NETWORK_TIMEOUT,
                message = "网络请求超时: ${e.message}",
                cause = e
            )
            is UnknownHostException -> ApiResult.Failure(
                errorCode = ErrorCode.NETWORK_UNREACHABLE,
                message = "网络不可达: ${e.message}",
                cause = e
            )
            is IOException -> ApiResult.Failure(
                errorCode = ErrorCode.NETWORK_UNKNOWN,
                message = "网络错误: ${e.message}",
                cause = e
            )
            else -> ApiResult.Failure(
                errorCode = ErrorCode.UNKNOWN_ERROR,
                message = "未知错误: ${e.message}",
                cause = e
            )
        }
    }
}
