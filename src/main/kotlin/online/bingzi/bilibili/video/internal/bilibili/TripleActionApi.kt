package online.bingzi.bilibili.video.internal.bilibili

import online.bingzi.bilibili.video.internal.bilibili.dto.*
import online.bingzi.bilibili.video.internal.http.BilibiliHttpClient

/**
 * 与「三连」相关的 B 站接口封装。
 */
object TripleActionApi {

    private fun buildCookie(sessData: String, biliJct: String, buvid3: String?, accessKey: String?): String {
        return BilibiliHttpClient.buildCookieHeader(sessData, biliJct, buvid3, accessKey)
    }

    /**
     * 查询点赞状态（近期）。
     */
    fun hasLikeByBvid(
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        bvid: String
    ): Boolean {
        val url =
            "https://api.bilibili.com/x/web-interface/archive/has/like?bvid=$bvid"
        val cookie = buildCookie(sessData, biliJct, buvid3, accessKey)
        val resp = BilibiliHttpClient.get<BilibiliResponse<HasLikeData>>(
            url = url,
            credentialCookieHeader = cookie
        )
        val data = resp.data ?: return false
        return data == 1
    }

    /**
     * 查询投币状态。
     */
    fun coinInfoByBvid(
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        bvid: String
    ): Int {
        val url =
            "https://api.bilibili.com/x/web-interface/archive/coins?bvid=$bvid"
        val cookie = buildCookie(sessData, biliJct, buvid3, accessKey)
        val resp = BilibiliHttpClient.get<BilibiliResponse<CoinInfoData>>(
            url = url,
            credentialCookieHeader = cookie
        )
        val data = resp.data ?: return 0
        return data.multiply
    }

    /**
     * 查询收藏状态。
     */
    fun favouredByBvid(
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        bvid: String
    ): Boolean {
        val url =
            "https://api.bilibili.com/x/v2/fav/video/favoured?aid=$bvid"
        val cookie = buildCookie(sessData, biliJct, buvid3, accessKey)
        val resp = BilibiliHttpClient.get<BilibiliResponse<FavouredData>>(
            url = url,
            credentialCookieHeader = cookie
        )
        val data = resp.data ?: return false
        return data.favoured
    }

    /**
     * 综合三项状态，返回是否三连。
     */
    fun queryTripleStatusByBvid(
        sessData: String,
        biliJct: String,
        buvid3: String?,
        accessKey: String?,
        bvid: String
    ): TripleStatusResult {
        val liked = hasLikeByBvid(sessData, biliJct, buvid3, accessKey, bvid)
        val coinCount = coinInfoByBvid(sessData, biliJct, buvid3, accessKey, bvid)
        val favoured = favouredByBvid(sessData, biliJct, buvid3, accessKey, bvid)
        return TripleStatusResult(
            liked = liked,
            coinCount = coinCount,
            favoured = favoured
        )
    }
}
