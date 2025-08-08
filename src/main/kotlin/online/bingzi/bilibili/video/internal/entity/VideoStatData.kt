package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * 视频统计数据
 * 
 * 包含视频的各项统计信息，如播放量、点赞数、投币数等
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
data class VideoStatData(
    /**
     * 视频AV号
     */
    @SerializedName("aid")
    val aid: Long,
    
    /**
     * 播放量
     */
    @SerializedName("view")
    val view: Int,
    
    /**
     * 弹幕数
     */
    @SerializedName("danmaku")
    val danmaku: Int,
    
    /**
     * 评论数
     */
    @SerializedName("reply")
    val reply: Int,
    
    /**
     * 收藏数
     */
    @SerializedName("favorite")
    val favorite: Int,
    
    /**
     * 投币数
     */
    @SerializedName("coin")
    val coin: Int,
    
    /**
     * 分享数
     */
    @SerializedName("share")
    val share: Int,
    
    /**
     * 当前排名
     */
    @SerializedName("now_rank")
    val nowRank: Int,
    
    /**
     * 历史最高排名
     */
    @SerializedName("his_rank")
    val hisRank: Int,
    
    /**
     * 点赞数
     */
    @SerializedName("like")
    val like: Int,
    
    /**
     * 点踩数（目前恒为0，B站已隐藏此数据）
     */
    @SerializedName("dislike")
    val dislike: Int = 0,
    
    /**
     * 视频评价（官方星级评分）
     */
    @SerializedName("evaluation")
    val evaluation: String? = null,
    
    /**
     * 争议原因
     */
    @SerializedName("argue_msg")
    val argueMsg: String? = null
) {
    /**
     * 获取格式化的播放量
     * 
     * @return 格式化后的播放量字符串（如：1.2万）
     */
    fun getFormattedView(): String {
        return formatNumber(view)
    }
    
    /**
     * 获取格式化的弹幕数
     * 
     * @return 格式化后的弹幕数字符串
     */
    fun getFormattedDanmaku(): String {
        return formatNumber(danmaku)
    }
    
    /**
     * 获取格式化的点赞数
     * 
     * @return 格式化后的点赞数字符串
     */
    fun getFormattedLike(): String {
        return formatNumber(like)
    }
    
    /**
     * 获取格式化的投币数
     * 
     * @return 格式化后的投币数字符串
     */
    fun getFormattedCoin(): String {
        return formatNumber(coin)
    }
    
    /**
     * 获取格式化的收藏数
     * 
     * @return 格式化后的收藏数字符串
     */
    fun getFormattedFavorite(): String {
        return formatNumber(favorite)
    }
    
    /**
     * 格式化数字
     * 
     * @param num 原始数字
     * @return 格式化后的字符串
     */
    private fun formatNumber(num: Int): String {
        return when {
            num >= 100000000 -> String.format("%.1f亿", num / 100000000.0)
            num >= 10000 -> String.format("%.1f万", num / 10000.0)
            else -> num.toString()
        }
    }
    
    /**
     * 计算互动率
     * 
     * @return 互动率百分比
     */
    fun getInteractionRate(): Double {
        if (view == 0) return 0.0
        val interactions = like + coin + favorite + share
        return (interactions.toDouble() / view) * 100
    }
    
    /**
     * 判断视频是否热门
     * 播放量超过10万或点赞超过1万即视为热门
     * 
     * @return 是否热门
     */
    fun isPopular(): Boolean {
        return view >= 100000 || like >= 10000
    }
    
    /**
     * 获取视频质量评分
     * 根据点赞投币比等指标计算
     * 
     * @return 质量评分（0-100）
     */
    fun getQualityScore(): Int {
        if (view == 0) return 0
        
        // 点赞率权重40%
        val likeRate = (like.toDouble() / view) * 100
        val likeScore = minOf(likeRate * 2, 40.0)
        
        // 投币率权重30%
        val coinRate = (coin.toDouble() / view) * 100
        val coinScore = minOf(coinRate * 3, 30.0)
        
        // 收藏率权重20%
        val favoriteRate = (favorite.toDouble() / view) * 100
        val favoriteScore = minOf(favoriteRate * 2, 20.0)
        
        // 弹幕参与度权重10%
        val danmakuRate = (danmaku.toDouble() / view) * 100
        val danmakuScore = minOf(danmakuRate, 10.0)
        
        return (likeScore + coinScore + favoriteScore + danmakuScore).toInt()
    }
    
    /**
     * 转换为简要统计信息字符串
     * 
     * @return 统计信息摘要
     */
    fun toSummaryString(): String {
        return """
            播放: ${getFormattedView()}
            点赞: ${getFormattedLike()}
            投币: ${getFormattedCoin()}
            收藏: ${getFormattedFavorite()}
            弹幕: ${getFormattedDanmaku()}
            评论: ${formatNumber(reply)}
            分享: ${formatNumber(share)}
        """.trimIndent()
    }
}