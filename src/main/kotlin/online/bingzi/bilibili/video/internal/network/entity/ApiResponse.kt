package online.bingzi.bilibili.video.internal.network.entity

import okhttp3.Headers

/**
 * API 响应封装类
 */
data class ApiResponse(
    val success: Boolean,
    val data: String,
    val errorMessage: String?,
    val responseHeaders: Headers?
) {
    companion object {
        fun success(data: String, headers: Headers? = null): ApiResponse {
            return ApiResponse(true, data, null, headers)
        }

        fun failure(error: String): ApiResponse {
            return ApiResponse(false, "", error, null)
        }
    }

    /**
     * 检查响应是否成功
     */
    fun isSuccess(): Boolean = success

    /**
     * 获取响应数据，如果失败则抛出异常
     */
    fun getDataOrThrow(): String {
        if (!success) {
            throw RuntimeException("API请求失败: $errorMessage")
        }
        return data
    }

    /**
     * 获取错误信息
     */
    fun getError(): String? = errorMessage

    /**
     * 获取响应头
     */
    fun getHeaders(): Headers? = responseHeaders
}