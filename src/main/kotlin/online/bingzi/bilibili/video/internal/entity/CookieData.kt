package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * 表示用户的 Cookie 数据类
 *
 * 该类用于存储与用户会话相关的 Cookie 信息，包括会话数据、用户 ID 和其他身份验证信息。
 * 在与 Bilibili 的 API 交互时，通常需要这些 Cookie 数据以维持用户的登录状态。
 *
 * @property sessData 当前会话的标识符，类型为可选字符串，可能为空。
 * @property biliJct 用于身份验证的令牌，类型为可选字符串，可能为空。
 * @property dedeUserID 用户的唯一标识符，类型为可选字符串，可能为空。
 * @property dedeUserIDCkMd5 用户 ID 的 MD5 校验值，类型为可选字符串，可能为空。
 * @property sid 会话 ID，类型为可选字符串，可能为空。
 * @constructor 创建一个空的 Cookie 数据对象
 */
data class CookieData(
    @SerializedName("SESSDATA")
    var sessData: String? = null, // 当前会话的标识符
    @SerializedName("bili_jct")
    var biliJct: String? = null, // 身份验证令牌
    @SerializedName("DedeUserID")
    var dedeUserID: String? = null, // 用户的唯一标识符
    @SerializedName("DedeUserID__ckMd5")
    var dedeUserIDCkMd5: String? = null, // 用户 ID 的 MD5 校验值
    @SerializedName("sid")
    var sid: String? = null // 会话 ID
)