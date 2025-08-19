package online.bingzi.bilibili.video.internal.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import online.bingzi.bilibili.video.internal.cache.qrCodeKeyCache

/**
 * ReceivedCookiesInterceptor 类用于拦截 HTTP 响应，提取并缓存其中的 Set-Cookie 头部信息。
 * 该类实现了 okhttp3 的 Interceptor 接口，主要用于在请求中处理与二维码相关的 cookie 信息。
 */
class ReceivedCookiesInterceptor : Interceptor {

    /**
     * 拦截请求并处理响应中的 Set-Cookie 头部信息。
     *
     * @param chain Interceptor.Chain 用于获取请求和响应。
     * @return Response 返回原始响应对象，包括所有的头部信息和主体内容。
     *
     * @throws IOException 当网络请求失败或其他 I/O 错误发生时抛出此异常。
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // 获取原始请求的响应
        val originalResponse: Response = chain.proceed(chain.request())

        // 检查响应头部中是否存在 Set-Cookie 信息
        if (originalResponse.headers("Set-Cookie").isNotEmpty()) {
            // 获取请求的 URL 中的查询参数 qrcode_key
            chain.request().url.queryParameter("qrcode_key")?.let {
                // 将 Set-Cookie 信息缓存，与 qrcode_key 关联
                qrCodeKeyCache.put(it, originalResponse.headers("Set-Cookie"))
            }
        }

        // 返回原始响应
        return originalResponse
    }
}