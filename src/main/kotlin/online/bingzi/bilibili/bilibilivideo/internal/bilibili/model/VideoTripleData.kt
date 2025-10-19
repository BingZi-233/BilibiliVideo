package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

/**
 * 点赞状态API响应数据类
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 点赞状态，0=未点赞，1=已点赞
 */
data class LikeStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: Int
)

/**
 * 投币状态API响应数据类
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 投币状态数据
 */
data class CoinStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: CoinStatusData?
)

/**
 * 投币状态详细数据
 * 
 * @property multiply 投币数量，0表示未投币，1-2表示投币个数
 */
data class CoinStatusData(
    val multiply: Int
)

/**
 * 收藏状态API响应数据类
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 收藏状态数据
 */
data class FavoriteStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: FavoriteStatusData?
)

/**
 * 收藏状态详细数据
 * 
 * @property count 收藏夹数量
 * @property favoured 是否已收藏，true=已收藏，false=未收藏
 */
data class FavoriteStatusData(
    val count: Int,
    val favoured: Boolean
)

/**
 * 视频三连数据类
 * 
 * 表示用户对特定视频的三连操作状态（点赞、投币、收藏）。
 * 提供状态检查和格式化显示功能。
 * 
 * @property bvid 视频BV号
 * @property playerUuid Minecraft玩家UUID
 * @property mid Bilibili用户MID
 * @property isLiked 是否已点赞
 * @property coinCount 投币数量（0-2）
 * @property isFavorited 是否已收藏
 * @property timestamp 数据获取时间戳（毫秒）
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class VideoTripleData(
    val bvid: String,
    val playerUuid: String,
    val mid: Long,
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 检查是否完成三连操作
     * 
     * 三连指同时点赞、投币和收藏视频。
     * 
     * @return true 如果用户对该视频进行了完整的三连操作
     */
    fun hasTripleAction(): Boolean = isLiked && coinCount > 0 && isFavorited
    
    /**
     * 获取状态的文字描述
     * 
     * @return 格式化的状态描述字符串，如"已点赞 | 已投币2个 | 已收藏"
     */
    fun getStatusMessage(): String {
        val likeStatus = if (isLiked) "已点赞" else "未点赞"
        val coinStatus = if (coinCount > 0) "已投币${coinCount}个" else "未投币"
        val favoriteStatus = if (isFavorited) "已收藏" else "未收藏"
        
        return "$likeStatus | $coinStatus | $favoriteStatus"
    }
}