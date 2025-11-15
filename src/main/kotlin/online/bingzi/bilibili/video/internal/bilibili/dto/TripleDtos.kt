package online.bingzi.bilibili.video.internal.bilibili.dto

/**
 * 通用的 B 站 API 响应包装。
 */
data class BilibiliResponse<T>(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: T?
)

typealias HasLikeData = Int

data class CoinInfoData(
    val multiply: Int
)

data class FavouredData(
    val favoured: Boolean
)

/**
 * 汇总三连状态。
 */
data class TripleStatusResult(
    val liked: Boolean,
    val coinCount: Int,
    val favoured: Boolean
) {
    val isTriple: Boolean
        get() = liked && coinCount > 0 && favoured
}
