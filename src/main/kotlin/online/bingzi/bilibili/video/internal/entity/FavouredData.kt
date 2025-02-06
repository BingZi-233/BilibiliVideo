package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * FavouredData 类表示用户的收藏数据。
 * 主要用于存储用户在平台上收藏的视频或内容的信息。
 * 该类包含两个属性：收藏数量和收藏状态。
 */
data class FavouredData(
    /**
     * 收藏数量，表示用户收藏的项目总数。
     * 类型为 Int，取值范围为 0 到正整数。
     */
    @SerializedName("count")
    val count: Int,

    /**
     * 收藏状态，表示该视频或内容是否被用户收藏。
     * 类型为 Boolean，取值为 true（已收藏）或 false（未收藏）。
     */
    @SerializedName("favoured")
    val favoured: Boolean
)