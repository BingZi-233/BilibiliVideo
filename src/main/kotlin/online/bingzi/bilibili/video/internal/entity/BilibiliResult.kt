package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * BilibiliResult 类
 *
 * 该类用于表示 Bilibili API 返回的结果，包括状态码、消息、TTL
 * 以及实际数据。它是一个泛型类，允许使用不同类型的数据。
 *
 * @param T 表示数据的类型，例如视频信息、用户信息等。
 * @property code 状态码，0 表示成功，1 表示失败。
 * @property message 返回的消息，提供了对请求结果的描述。
 * @property ttl 该结果的生存时间（Time To Live），表示结果的有效性。
 * @property data 实际返回的数据，类型由 T 决定。
 * @constructor 创建一个空的 BilibiliResult 对象。
 */
data class BilibiliResult<T>(
    @SerializedName("code")
    val code: Int, // 状态码，0表示成功，1表示失败
    @SerializedName("message")
    val message: String, // 返回的消息，描述请求结果
    @SerializedName("ttl")
    val ttl: Int, // 结果的生存时间，表示结果的有效性
    @SerializedName("data")
    val data: T // 实际返回的数据，类型为 T
)