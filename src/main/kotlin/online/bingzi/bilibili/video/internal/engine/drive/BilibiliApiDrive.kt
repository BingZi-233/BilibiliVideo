package online.bingzi.bilibili.video.internal.engine.drive

import online.bingzi.bilibili.video.internal.cache.buvid3Cache
import online.bingzi.bilibili.video.internal.entity.*
import retrofit2.Call
import retrofit2.http.*

/**
 * Bilibili api drive
 * 哔哩哔哩API驱动
 *
 * 该接口定义了与哔哩哔哩视频网站交互的API，提供了获取用户信息、点赞、投币、收藏和关注等功能。
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
interface BilibiliApiDrive {

    /**
     * 获取用户导航信息（包含WBI密钥）
     * 
     * 该接口返回的wbi_img字段包含img_key和sub_key，用于WBI签名
     *
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @return 返回一个Call对象，包含BilibiliResult<UserInfoData>，表示用户信息的结果。
     */
    @GET("web-interface/nav")
    fun getUserInfo(
        @Header("Cookie") sessData: String,
    ): Call<BilibiliResult<UserInfoData>>

    /**
     * 获取视频详情
     * 
     * @param bvid 视频BV号
     * @param sessData 用户会话数据
     * @param buvid3 设备标识
     * @return 视频详情数据
     */
    @GET("web-interface/view")
    fun getVideoDetail(
        @Query("bvid") bvid: String,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<VideoDetailData>>
    
    /**
     * 获取视频详情（需要WBI签名）
     * 
     * @param params 包含bvid和WBI签名参数的Map
     * @param sessData 用户会话数据
     * @param buvid3 设备标识
     * @return 视频详情数据
     */
    @GET("web-interface/wbi/view")
    fun getVideoDetailWbi(
        @QueryMap params: Map<String, @JvmSuppressWildcards Any>,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<VideoDetailData>>
    
    /**
     * 视频点赞
     * 
     * @param bvid 视频BV号
     * @param like 1为点赞，2为取消点赞
     * @param csrf CSRF令牌
     * @param sessData 用户会话数据
     * @param buvid3 设备标识
     * @return 操作结果
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("web-interface/archive/like")
    fun likeVideo(
        @Field("bvid") bvid: String,
        @Field("like") like: Int,
        @Field("csrf") csrf: String,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<Any>>
    
    /**
     * 视频投币
     * 
     * @param bvid 视频BV号
     * @param multiply 投币数量（1或2）
     * @param selectLike 是否同时点赞（0否1是）
     * @param csrf CSRF令牌
     * @param sessData 用户会话数据
     * @param buvid3 设备标识
     * @return 操作结果
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("web-interface/coin/add")
    fun coinVideo(
        @Field("bvid") bvid: String,
        @Field("multiply") multiply: Int,
        @Field("select_like") selectLike: Int,
        @Field("csrf") csrf: String,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<Any>>
    
    /**
     * 一键三连操作
     *
     * @param bvid 视频的唯一标识符，类型为String。
     * @param csrf 跨站请求伪造令牌，类型为String，用于安全验证。
     * @param sessData 用户的会话数据，类型为String，代表用户的登录状态。
     * @param buvid3 设备标识
     * @return 返回一个Call对象，包含BilibiliResult<TripleData>，表示三连操作的结果。
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("web-interface/archive/like/triple")
    fun actionLikeTriple(
        @Field("bvid") bvid: String,
        @Field("csrf") csrf: String,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
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
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
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
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
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
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
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
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<FollowingData>>

    /**
     * 获取视频统计信息
     * 
     * @param bvid 视频BV号
     * @return 视频统计数据
     */
    @GET("web-interface/archive/stat")
    fun getVideoStat(
        @Query("bvid") bvid: String
    ): Call<BilibiliResult<VideoStatData>>
    
    /**
     * 收藏视频到默认收藏夹
     * 
     * @param rid 视频ID（可以是aid）
     * @param type 类型（2为视频）
     * @param addMediaIds 要添加到的收藏夹ID列表，逗号分隔
     * @param delMediaIds 要移除的收藏夹ID列表，逗号分隔
     * @param csrf CSRF令牌
     * @param sessData 用户会话数据
     * @param buvid3 设备标识
     * @return 操作结果
     */
    @FormUrlEncoded
    @Headers("Content-Type: application/x-www-form-urlencoded")
    @POST("medialist/gateway/coll/resource/deal")
    fun favoriteVideo(
        @Field("rid") rid: Long,
        @Field("type") type: Int = 2,
        @Field("add_media_ids") addMediaIds: String,
        @Field("del_media_ids") delMediaIds: String = "",
        @Field("csrf") csrf: String,
        @Header("Cookie") sessData: String,
        @Header("Cookie") buvid3: String = buvid3Cache
    ): Call<BilibiliResult<Any>>
    
    /**
     * Get buvid3
     * <p>
     * 获取Buvid3
     *
     * @return [Buvid3Data]
     */
    @GET("web-frontend/getbuvid")
    fun getBuvid3(): Call<BilibiliResult<Buvid3Data>>
}