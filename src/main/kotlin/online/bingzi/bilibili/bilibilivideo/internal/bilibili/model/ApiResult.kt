package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

/**
 * API 结果封装,用于统一处理成功和失败情况
 *
 * @param T 成功时返回的数据类型
 */
sealed class ApiResult<out T> {

    /**
     * 成功结果
     *
     * @property data 返回的数据
     */
    data class Success<out T>(val data: T) : ApiResult<T>()

    /**
     * 失败结果
     *
     * @property errorCode 错误码
     * @property message 错误消息
     * @property cause 原始异常(可选)
     * @property httpCode HTTP 状态码(可选)
     */
    data class Failure(
        val errorCode: ErrorCode,
        val message: String,
        val cause: Throwable? = null,
        val httpCode: Int? = null
    ) : ApiResult<Nothing>()

    /**
     * 判断结果是否成功
     *
     * @return 成功返回 true,失败返回 false
     */
    fun isSuccess(): Boolean = this is Success

    /**
     * 判断结果是否失败
     *
     * @return 失败返回 true,成功返回 false
     */
    fun isFailure(): Boolean = this is Failure

    /**
     * 获取成功时的数据,失败时返回 null
     *
     * @return 成功时的数据或 null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }

    /**
     * 获取成功时的数据,失败时抛出异常
     *
     * @return 成功时的数据
     * @throws BilibiliApiException 失败时抛出
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw BilibiliApiException(
            "API 调用失败: $message (错误码: $errorCode)",
            cause
        )
    }

    /**
     * 成功时执行操作
     *
     * @param action 要执行的操作
     * @return 返回自身以支持链式调用
     */
    inline fun onSuccess(action: (T) -> Unit): ApiResult<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * 失败时执行操作
     *
     * @param action 要执行的操作
     * @return 返回自身以支持链式调用
     */
    inline fun onFailure(action: (Failure) -> Unit): ApiResult<T> {
        if (this is Failure) {
            action(this)
        }
        return this
    }
}

/**
 * API 错误码枚举
 */
enum class ErrorCode {
    /** 网络超时 */
    NETWORK_TIMEOUT,

    /** 网络不可达 */
    NETWORK_UNREACHABLE,

    /** 未知网络错误 */
    NETWORK_UNKNOWN,

    /** 认证失败 */
    AUTH_FAILED,

    /** Cookie 过期 */
    COOKIE_EXPIRED,

    /** 请求限流 */
    RATE_LIMITED,

    /** 资源不存在 */
    RESOURCE_NOT_FOUND,

    /** 参数无效 */
    INVALID_PARAMETER,

    /** 参数缺失 */
    MISSING_PARAMETER,

    /** JSON 解析错误 */
    JSON_PARSE_ERROR,

    /** 未知错误 */
    UNKNOWN_ERROR
}

/**
 * Bilibili API 异常
 *
 * @property message 错误消息
 * @property cause 原始异常
 */
class BilibiliApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
