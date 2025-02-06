package online.bingzi.bilibili.video.internal.interceptor

import okhttp3.Interceptor
import okhttp3.Response

// UserAgentInterceptor 类用于拦截 HTTP 请求并设置自定义的 User-Agent 头部信息。
// 它实现了 Interceptor 接口，主要功能是在请求中替换默认的 User-Agent 头部为指定的值。
class UserAgentInterceptor(private val userAgent: String) : Interceptor {

    // intercept 方法用于处理拦截的请求。
    // 它接受一个 Interceptor.Chain 对象，代表请求链，返回一个 Response 对象。
    // 参数:
    //   chain: Interceptor.Chain - 代表请求链的对象，可以用来获取当前请求和继续请求。
    // 返回值:
    //   Response - 返回处理后的 HTTP 响应对象。
    // 可能抛出的异常:
    //   IOException - 在网络请求过程中可能会出现 I/O 异常。
    override fun intercept(chain: Interceptor.Chain): Response {
        // 使用链对象获取当前请求，并创建一个新的请求构建器。
        val request = chain.request().newBuilder()
            // 移除现有的 User-Agent 头部。
            .removeHeader("User-Agent")
            // 添加自定义的 User-Agent 头部。
            .header("User-Agent", userAgent)
            // 构建新的请求对象。
            .build()

        // 继续请求链，发送修改后的请求，并返回响应。
        return chain.proceed(request)
    }
}