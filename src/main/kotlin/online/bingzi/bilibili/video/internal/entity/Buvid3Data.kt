package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * Buvid3data
 * <p>
 * Buvid3数据
 *
 * @property buVid buvid3
 * @constructor Create empty Buvid3data
 *
 * @author BingZi-233
 * @since 2.0.0
 */
data class Buvid3Data(
    @SerializedName("buvid")
    val buVid: String
)