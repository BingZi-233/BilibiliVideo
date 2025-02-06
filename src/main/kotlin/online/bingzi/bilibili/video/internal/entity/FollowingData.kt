package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * FollowingData 类用于表示用户关注的数据结构。
 * 该类包含一个 CardData 对象，表示与用户关注相关的信息。
 */
data class FollowingData(
    /**
     * card 属性表示与用户关注相关的卡片数据。
     * 该属性是一个 CardData 类型的对象，包含详细的关注信息。
     */
    @SerializedName("Card")
    val card: CardData
)