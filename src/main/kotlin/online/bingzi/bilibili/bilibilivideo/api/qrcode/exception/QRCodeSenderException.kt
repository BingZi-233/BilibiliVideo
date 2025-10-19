package online.bingzi.bilibili.bilibilivideo.api.qrcode.exception

import online.bingzi.bilibili.bilibilivideo.api.qrcode.result.ErrorCode

/**
 * 二维码发送器异常
 * 
 * 二维码发送过程中抛出的特定异常类型，包含结构化的错误代码便于错误处理。
 * 
 * @param message 异常消息
 * @param cause 异常原因，可为null
 * @param errorCode 结构化的错误代码
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
class QRCodeSenderException(
    message: String,
    cause: Throwable? = null,
    val errorCode: ErrorCode
) : Exception(message, cause)