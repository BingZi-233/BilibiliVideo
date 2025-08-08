package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * Buvid3数据
 * 
 * 从 /x/web-frontend/getbuvid 接口返回的数据
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
data class Buvid3Data(
    /**
     * buvid3值
     * API返回的字段名为"b_3"
     */
    @SerializedName("b_3")
    val buvid3: String,
    
    /**
     * buvid4值（可选）
     * API返回的字段名为"b_4"
     */
    @SerializedName("b_4")
    val buvid4: String? = null
)