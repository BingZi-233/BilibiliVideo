package online.bingzi.bilibili.video.internal.engine.drive

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * BilibiliWebsiteDrive 接口
 * 该接口定义了与哔哩哔哩网站交互的驱动方法。
 * 主要功能是提供获取 CSRF 刷新令牌的 API。
 */
interface BilibiliWebsiteDrive {

    /**
     * 获取刷新 CSRF 令牌
     * 该方法通过网络请求获取当前的 CSRF 令牌，以便进行后续的安全请求。
     *
     * @param correspondPath 需要替换的路径参数，类型为 String，表示具体的对应路径。
     *                        此参数的值应当遵循 API 文档的要求。
     * @return 返回一个 Call<ResponseBody> 对象，表示网络请求的结果。
     *         ResponseBody 中包含了服务器响应的内容。
     *         如果请求成功，响应体中将包含新的 CSRF 令牌信息。
     */
    @GET("correspond/1/{correspondPath}")
    fun getRefreshCSRF(@Path("correspondPath") correspondPath: String): Call<ResponseBody>
}