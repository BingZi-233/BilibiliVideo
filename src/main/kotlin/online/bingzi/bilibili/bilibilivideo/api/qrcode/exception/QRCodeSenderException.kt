package online.bingzi.bilibili.bilibilivideo.api.qrcode.exception

import online.bingzi.bilibili.bilibilivideo.api.qrcode.result.ErrorCode

class QRCodeSenderException(
    message: String,                                    // 异常消息
    cause: Throwable? = null,                          // 异常原因
    val errorCode: ErrorCode                           // 错误代码
) : Exception(message, cause)