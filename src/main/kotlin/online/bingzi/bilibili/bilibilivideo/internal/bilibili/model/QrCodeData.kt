package online.bingzi.bilibili.bilibilivideo.internal.bilibili.model

import com.google.gson.annotations.SerializedName

/**
 * 二维码生成API响应数据类
 * 
 * 对应Bilibili二维码生成API的响应结构。
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 二维码数据，成功时不为null
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class QrCodeGenerateResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodeData?
)

/**
 * 二维码数据类
 * 
 * 包含生成的二维码URL和用于轮询的密钥。
 * 
 * @property url 二维码图片URL，用于显示给用户扫描
 * @property qrcodeKey 二维码密钥，用于后续轮询登录状态
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class QrCodeData(
    val url: String,
    @SerializedName("qrcode_key")
    val qrcodeKey: String
)

/**
 * 二维码轮询API响应数据类
 * 
 * 对应Bilibili二维码登录状态轮询API的响应结构。
 * 
 * @property code 响应状态码，0表示成功
 * @property message 响应消息
 * @property ttl 生存时间
 * @property data 轮询结果数据
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class QrCodePollResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodePollData?
)

/**
 * 二维码轮询结果数据类
 * 
 * 包含登录状态和成功时的认证信息。
 * 
 * @property url 登录成功后的跳转URL，包含Cookie信息
 * @property refreshToken 刷新令牌，用于Cookie自动刷新
 * @property timestamp 时间戳
 * @property code 登录状态码，对应LoginStatus枚举
 * @property message 状态消息
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class QrCodePollData(
    val url: String?,
    @SerializedName("refresh_token")
    val refreshToken: String?,
    val timestamp: Long,
    val code: Int,
    val message: String
)

/**
 * 登录状态枚举
 * 
 * 定义二维码登录过程中的各种状态。
 * 
 * @property code Bilibili API返回的状态码
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
enum class LoginStatus(val code: Int) {
    /** 登录成功 */
    SUCCESS(0),
    /** 二维码未被扫描 */
    NOT_SCANNED(86101),
    /** 已扫描但用户未确认登录 */
    SCANNED_WAITING(86090),
    /** 二维码已过期，需要重新生成 */
    EXPIRED(86038);
    
    companion object {
        /**
         * 根据状态码获取对应的登录状态
         * 
         * @param code Bilibili API返回的状态码
         * @return 对应的LoginStatus，未知状态码返回EXPIRED
         */
        fun fromCode(code: Int): LoginStatus {
            return values().find { it.code == code } ?: EXPIRED
        }
    }
}