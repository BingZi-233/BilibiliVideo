package online.bingiz.bilibili.video.internal.engine.drive

import okhttp3.ResponseBody
import online.bingiz.bilibili.video.internal.entity.BilibiliResult
import online.bingiz.bilibili.video.internal.entity.QRCodeGenerateData
import online.bingiz.bilibili.video.internal.entity.TripleData
import retrofit2.Call
import retrofit2.http.*

/**
 * Bilibili passport drive
 * 哔哩哔哩通行证驱动
 *
 * @constructor Create empty Bilibili passport drive
 */
interface BilibiliPassportDrive {
    /**
     * Apply QRCode generate
     *
     * @return
     */
    @GET("/passport-login/web/qrcode/generate")
    fun applyQRCodeGenerate(): Call<BilibiliResult<QRCodeGenerateData>>

    /**
     * Action like triple
     *
     * @param aid
     * @param bvid
     * @param csrf
     * @return
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("/web-interface/archive/like/triple")
    fun actionLikeTriple(
        @Field("aid") aid: String,
        @Field("bvid") bvid: String,
        @Header("Cookie") csrf: String
    ): Call<BilibiliResult<TripleData>>

    /**
     * Check cookie refresh token
     *
     * @param csrf
     * @return
     */
    @GET("/passport-login/web/cookie/info")
    fun checkCookieRefreshToken(@Query("csrf") csrf: String): Call<ResponseBody>

    /**
     * Refresh cookie
     *
     * @return
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("/passport-login/web/cookie/refresh")
    fun refreshCookie(
        @Field("csrf") csrf: String,
        @Field("refresh_csrf") refreshCsrf: String,
        @Field("source") source: String = "main_web",
        @Field("refresh_token") refreshToken: String
    ): Call<ResponseBody>

    /**
     * Confirm refresh cookie
     *
     * @return
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("/passport-login/web/confirm/refresh")
    fun confirmRefreshCookie(
        @Field("csrf") csrf: String,
        @Field("refresh_token") refreshToken: String
    ): Call<ResponseBody>
}