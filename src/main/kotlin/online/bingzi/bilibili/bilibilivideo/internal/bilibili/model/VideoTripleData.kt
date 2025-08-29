package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

data class LikeStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: Int // 0=未点赞，1=已点赞
)

data class CoinStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: CoinStatusData?
)

data class CoinStatusData(
    val multiply: Int // 投币数量，0表示未投币
)

data class FavoriteStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: FavoriteStatusData?
)

data class FavoriteStatusData(
    val count: Int,
    val favoured: Boolean // true=已收藏，false=未收藏
)

data class VideoTripleData(
    val bvid: String,
    val playerUuid: String,
    val mid: Long,
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun hasTripleAction(): Boolean = isLiked && coinCount > 0 && isFavorited
    
    fun getStatusMessage(): String {
        val likeStatus = if (isLiked) "已点赞" else "未点赞"
        val coinStatus = if (coinCount > 0) "已投币${coinCount}个" else "未投币"
        val favoriteStatus = if (isFavorited) "已收藏" else "未收藏"
        
        return "$likeStatus | $coinStatus | $favoriteStatus"
    }
}