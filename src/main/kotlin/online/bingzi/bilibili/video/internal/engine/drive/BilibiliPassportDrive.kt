package online.bingzi.bilibili.video.internal.engine.drive

import okhttp3.ResponseBody
import online.bingzi.bilibili.video.internal.entity.BilibiliResult
import online.bingzi.bilibili.video.internal.entity.QRCodeGenerateData
import online.bingzi.bilibili.video.internal.entity.QRCodeScanningData
import retrofit2.Call
import retrofit2.http.*

/**
 * Bilibili passport drive
 * 哔哩哔哩通行证驱动
 *
 * 该接口定义了与哔哩哔哩通行证相关的操作，包括二维码生成、扫描、cookie 刷新等功能。
 * 主要用于处理用户登录及认证过程中的相关请求。
 */
interface BilibiliPassportDrive {
    /**
     * 申请二维码生成
     *
     * @return 返回一个 Call 对象，包含 BilibiliResult<QRCodeGenerateData> 类型的结果，
     *         其中包含了生成的二维码信息。
     */
    @GET("passport-login/web/qrcode/generate")
    fun applyQRCodeGenerate(): Call<BilibiliResult<QRCodeGenerateData>>

    /**
     * 扫描二维码
     *
     * @param key 二维码的唯一标识，类型为 String，通常由二维码生成接口返回。
     *            该参数用于查询二维码的扫描状态。
     * @return 返回一个 Call 对象，包含 BilibiliResult<QRCodeScanningData> 类型的结果，
     *         其中包含了二维码扫描的状态信息。
     */
    @GET("passport-login/web/qrcode/poll")
    fun scanningQRCode(@Query("qrcode_key") key: String): Call<BilibiliResult<QRCodeScanningData>>

    /**
     * 检查 cookie 刷新令牌
     *
     * @param csrf 跨站请求伪造令牌，类型为 String，通常在用户登录时提供。
     *             该参数用于验证请求的合法性。
     * @return 返回一个 Call 对象，包含 ResponseBody 类型的结果，
     *         其中包含了当前 cookie 的信息。
     */
    @GET("passport-login/web/cookie/info")
    fun checkCookieRefreshToken(@Query("csrf") csrf: String): Call<ResponseBody>

    /**
     * 刷新 cookie
     *
     * @param csrf 跨站请求伪造令牌，类型为 String，通常在用户登录时提供。
     *             用于验证请求的合法性。
     * @param refreshCsrf 刷新用的 csrf 令牌，类型为 String，通常在用户刷新 cookie 时提供。
     * @param source 源信息，类型为 String，默认为 "main_web"。用于标识请求来源。
     * @param refreshToken 刷新令牌，类型为 String，用于请求刷新 cookie。
     * @return 返回一个 Call 对象，包含 ResponseBody 类型的结果，
     *         其中包含了刷新操作的结果信息。
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("passport-login/web/cookie/refresh")
    fun refreshCookie(
        @Field("csrf") csrf: String,
        @Field("refresh_csrf") refreshCsrf: String,
        @Field("source") source: String = "main_web",
        @Field("refresh_token") refreshToken: String
    ): Call<ResponseBody>

    /**
     * 确认刷新 cookie
     *
     * @param csrf 跨站请求伪造令牌，类型为 String，通常在用户登录时提供。
     *             用于验证请求的合法性。
     * @param refreshToken 刷新令牌，类型为 String，用于请求确认刷新 cookie。
     * @return 返回一个 Call 对象，包含 ResponseBody 类型的结果，
     *         其中包含了确认操作的结果信息。
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("passport-login/web/confirm/refresh")
    fun confirmRefreshCookie(
        @Field("csrf") csrf: String,
        @Field("refresh_token") refreshToken: String
    ): Call<ResponseBody>
}