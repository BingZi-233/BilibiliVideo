package online.bingzi.bilibili.bilibilivideo.internal.bilibili.client

import okhttp3.Interceptor
import okhttp3.Response
import taboolib.common.platform.function.warning
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * OkHttp 重试拦截器
 *
 * 自动重试失败的网络请求，使用指数退避策略减轻服务器压力。
 *
 * @property maxRetries 最大重试次数（不包括首次请求），默认为 3
 * @property baseDelayMs 基础延迟时间（毫秒），默认为 1000ms
 *
 * @author BingZi-233
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val baseDelayMs: Long = 1000
) : Interceptor {

    companion object {
        /**
         * 可重试的 HTTP 状态码
         */
        private val RETRIABLE_STATUS_CODES = setOf(
            429, // Too Many Requests
            503, // Service Unavailable
            504  // Gateway Timeout
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var lastException: IOException? = null
        var lastResponse: Response? = null

        // 尝试执行请求，包括首次请求和重试
        for (attempt in 1..maxRetries) {
            try {
                // 如果不是首次请求，需要先关闭上一个响应
                lastResponse?.close()

                // 执行请求
                val response = chain.proceed(request)

                // 检查是否需要重试
                if (shouldRetry(response, attempt)) {
                    lastResponse = response

                    // 如果还有重试机会，则等待后继续
                    if (attempt < maxRetries) {
                        val delay = calculateDelay(attempt)
                        warning("请求失败（状态码: ${response.code}），正在重试（${attempt}/${maxRetries}）在 ${delay}ms 后: ${request.url}")
                        Thread.sleep(delay)
                        continue
                    }
                }

                // 请求成功或达到最大重试次数
                return response

            } catch (e: IOException) {
                lastException = e

                // 检查异常是否可重试
                if (isRetriableException(e)) {
                    // 如果还有重试机会，则等待后继续
                    if (attempt < maxRetries) {
                        val delay = calculateDelay(attempt)
                        warning("请求失败（${e.javaClass.simpleName}: ${e.message}），正在重试（${attempt}/${maxRetries}）在 ${delay}ms 后: ${request.url}")
                        Thread.sleep(delay)
                        continue
                    }
                }

                // 不可重试的异常或达到最大重试次数，直接抛出
                throw e
            }
        }

        // 所有重试都失败，抛出最后一个异常或返回最后一个响应
        lastException?.let { throw it }
        return lastResponse ?: throw IOException("所有重试都失败，但没有捕获到异常（不应该发生）")
    }

    /**
     * 判断响应是否需要重试
     *
     * @param response HTTP 响应
     * @param attempt 当前尝试次数（从 1 开始）
     * @return true 表示需要重试，false 表示不需要重试
     */
    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        // 如果已达到最大重试次数，不再重试
        if (attempt >= maxRetries) {
            return false
        }

        // 检查状态码是否在可重试列表中
        return response.code in RETRIABLE_STATUS_CODES
    }

    /**
     * 判断异常是否可重试
     *
     * 可重试的异常类型包括：
     * - SocketTimeoutException: 套接字超时
     * - IOException 且消息包含 "Connection reset": 连接重置
     * - IOException 且消息包含 "Broken pipe": 管道损坏
     *
     * @param e IO 异常
     * @return true 表示可重试，false 表示不可重试
     */
    private fun isRetriableException(e: IOException): Boolean {
        // 超时异常总是可以重试
        if (e is SocketTimeoutException) {
            return true
        }

        // 检查异常消息
        val message = e.message?.lowercase() ?: return false

        return when {
            "connection reset" in message -> true
            "broken pipe" in message -> true
            else -> false
        }
    }

    /**
     * 计算重试延迟时间（指数退避）
     *
     * 公式: baseDelayMs * 2^(attempt-1)
     * - 第 1 次重试: 1000ms
     * - 第 2 次重试: 2000ms
     * - 第 3 次重试: 4000ms
     *
     * @param attempt 当前尝试次数（从 1 开始）
     * @return 延迟时间（毫秒）
     */
    private fun calculateDelay(attempt: Int): Long {
        // 使用位运算计算 2^(attempt-1)
        val multiplier = 1 shl (attempt - 1)
        return baseDelayMs * multiplier
    }
}
