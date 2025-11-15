package online.bingzi.bilibili.video.internal.bilibili.dto

import com.google.gson.annotations.SerializedName

data class QrGenerateData(
    val url: String,
    @SerializedName("qrcode_key")
    val qrcodeKey: String
)

data class QrGenerateResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrGenerateData?
)

data class QrPollData(
    val code: Int,
    @SerializedName("refresh_token")
    val refreshToken: String?
)

data class QrPollResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrPollData?
)

data class NavData(
    val mid: Long,
    val uname: String
)

data class NavResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: NavData?
)

