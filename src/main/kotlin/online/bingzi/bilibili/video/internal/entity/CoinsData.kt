package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * CoinsData 类
 * 该类用于表示投币数据，包含了用户投币的相关信息。
 * 主要功能是通过反序列化 JSON 数据来获取投币的倍数信息。
 */
data class CoinsData(
    /**
     * 投币倍数
     * 表示用户在平台上投币的倍数。该值为整数类型，通常为正整数。
     * 例如，如果用户投币的倍数为 3，则值为 3。
     */
    @SerializedName("multiply")
    val multiply: Int
)