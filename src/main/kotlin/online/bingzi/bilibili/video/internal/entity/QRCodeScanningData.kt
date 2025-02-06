package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * QRCodeScanningData 类用于表示二维码扫描的数据结构。
 *
 * 该类包含了二维码扫描后返回的各项数据，包括 URL、刷新令牌、时间戳、状态码和消息等。
 * 在整个应用中，这个数据类主要用于处理二维码扫描结果的解析和存储。
 *
 * @property url 二维码扫描后获取的 URL 地址，类型为 String。
 * @property refreshToken 刷新令牌，用于后续的身份验证或会话维持，类型为 String。
 * @property timestamp 扫描时的时间戳，表示二维码扫描的时间，类型为 Long。
 * @property code 状态码，0 表示成功，1 表示失败，类型为 Int。
 * @property message 返回的信息，通常用于描述操作结果或错误信息，类型为 String。
 * @constructor 创建一个空的 QRCodeScanningData 实例。
 */
data class QRCodeScanningData(
    @SerializedName("url")
    val url: String, // 二维码扫描后获取的 URL 地址

    @SerializedName("refresh_token")
    val refreshToken: String, // 刷新令牌，用于后续的身份验证或会话维持

    @SerializedName("timestamp")
    val timestamp: Long, // 扫描时的时间戳

    @SerializedName("code")
    val code: Int, // 状态码，0 表示成功，1 表示失败

    @SerializedName("message")
    val message: String // 返回的信息，描述操作结果或错误信息
)