package online.bingzi.bilibili.video.internal.engine.drive

import online.bingzi.bilibili.video.internal.entity.*
import retrofit2.Call
import retrofit2.http.*

/**
 * Bilibili api drive
 * 哔哩哔哩API驱动
 *
 * 该接口定义了与哔哩哔哩视频网站交互的API，提供了获取用户信息、点赞、投币、收藏和关注等功能。
 */
interface BilibiliApiDrive {
    companion object {
        /**
         * Buvid3
         * 预制的Buvid3默认值，用于请求头中标识用户会话
         */
        private const val BUVID3 = "buvid3=BUVID3"
    }

    /**
     * 获取用户信息
     *
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<UserInfoData>，表示用户信息的结果。
     */
    @GET("web-interface/nav")
    fun getUserInfo(
        @Header("Cookie") sessData: String,
    ): Call<BilibiliResult<UserInfoData>>

    /**
     * 点赞操作
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param csrf 跨站请求伪造令牌，类型为String，用于安全验证。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<TripleData>，表示点赞操作的结果。
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("web-interface/archive/like/triple")
    fun actionLikeTriple(
        @Field("bvid") bvid: String,
        @Field("csrf") csrf: String,
        @Header("Cookie") sessData: String,
    ): Call<BilibiliResult<TripleData>>

    /**
     * 检查视频是否被点赞
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<Int>，表示点赞状态，1表示已点赞，0表示未点赞。
     */
    @GET("web-interface/archive/has/like")
    fun hasLike(
        @Query("bvid") bvid: String,
        @Header("Cookie") sessData: String
    ): Call<BilibiliResult<Int>>

    /**
     * 检查视频是否被投币
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<CoinsData>，表示投币状态及相关数据。
     */
    @GET("web-interface/archive/coins")
    fun hasCoins(
        @Query("bvid") bvid: String,
        @Header("Cookie") sessData: String
    ): Call<BilibiliResult<CoinsData>>

    /**
     * 检查视频是否被收藏
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<FavouredData>，表示收藏状态及相关数据。
     */
    @GET("v2/fav/video/favoured")
    fun hasFavoured(
        @Query("aid") bvid: String,
        @Header("Cookie") sessData: String
    ): Call<BilibiliResult<FavouredData>>

    /**
     * 检查用户是否关注了视频作者
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<FollowingData>，表示关注状态及相关数据。
     */
    @GET("web-interface/view/detail")
    fun hasFollowing(
        @Query("bvid") bvid: String,
        @Header("Cookie") sessData: String,
    ): Call<BilibiliResult<FollowingData>>
}