package online.bingzi.bilibili.video.internal.entity

import com.google.gson.annotations.SerializedName

/**
 * 表示二维码生成所需的数据结构。
 * 该类存储二维码生成的相关信息，包括二维码的URL和二维码的唯一键。
 * 在整个系统中，QRCodeGenerateData 用于与二维码生成相关的操作和数据传输。
 *
 * @property url 二维码指向的目标URL，类型为String。
 * @property qrCodeKey 二维码的唯一键，用于标识该二维码，类型为String。
 * @constructor 创建一个空的二维码生成数据对象。
 */
data class QRCodeGenerateData(
    @SerializedName("url")
    val url: String,  // 二维码指向的目标URL，类型为String。
    
    @SerializedName("qrcode_key")
    val qrCodeKey: String  // 二维码的唯一键，用于标识该二维码，类型为String。
)