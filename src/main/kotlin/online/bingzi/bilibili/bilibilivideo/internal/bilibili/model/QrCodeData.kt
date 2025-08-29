package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

import com.google.gson.annotations.SerializedName

data class QrCodeGenerateResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodeData?
)

data class QrCodeData(
    val url: String,
    @SerializedName("qrcode_key")
    val qrcodeKey: String
)

data class QrCodePollResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodePollData?
)

data class QrCodePollData(
    val url: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    val timestamp: Long,
    val code: Int,
    val message: String
)

enum class LoginStatus(val code: Int) {
    SUCCESS(0),           // 登录成功
    NOT_SCANNED(86101),   // 未扫码
    SCANNED_WAITING(86090), // 已扫码但未确认
    EXPIRED(86038);       // 二维码已失效
    
    companion object {
        fun fromCode(code: Int): LoginStatus {
            return values().find { it.code == code } ?: EXPIRED
        }
    }
}