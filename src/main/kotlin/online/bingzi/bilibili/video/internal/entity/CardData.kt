package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * CardData类用于表示一个卡片数据对象，主要用于存储与用户关注状态相关的信息。
 * 它包含一个属性来指示当前用户是否关注了某个特定的用户。
 *
 * @property following 表示当前用户是否关注此用户，值为true表示已关注，false表示未关注。
 * @constructor 创建一个空的CardData对象，可以通过提供following参数进行初始化。
 */
data class CardData(
    @SerializedName("following") // 使用Gson库中@SerializedName注解，指定JSON字段名
    val following: Boolean // 当前用户是否关注此用户的状态，true表示已关注，false表示未关注
)