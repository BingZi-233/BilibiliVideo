package online.bingzi.bilibili.video.internal.helper

import okhttp3.OkHttpClient
import online.bingzi.bilibili.video.internal.interceptor.ReceivedCookiesInterceptor
import online.bingzi.bilibili.video.internal.interceptor.UserAgentInterceptor
import taboolib.common.platform.function.pluginId
import taboolib.common.platform.function.pluginVersion

// 创建一个 OkHttpClient 实例，用于网络请求的配置和执行
// 此客户端将添加两个拦截器：ReceivedCookiesInterceptor 和 UserAgentInterceptor
// 主要用于处理接收到的 Cookies 和设置 User-Agent 头部信息
internal val client = OkHttpClient.Builder()
    // 添加处理接收到的 Cookies 的拦截器
    .addInterceptor(ReceivedCookiesInterceptor())
    // 添加自定义的 User-Agent 拦截器，包含插件的 ID 和版本信息
    .addInterceptor(UserAgentInterceptor("MinecraftPlugin $pluginId/$pluginVersion(lhby233@outlook.com)"))
    // 构建 OkHttpClient 实例
    .build()