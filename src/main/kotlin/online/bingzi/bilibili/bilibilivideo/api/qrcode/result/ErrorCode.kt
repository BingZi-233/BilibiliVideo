package online.bingzi.bilibili.bilibilivideo.api.qrcode.result

enum class ErrorCode {
    DEPENDENCY_MISSING,         // 依赖缺失
    SENDER_NOT_AVAILABLE,       // 发送器不可用
    INITIALIZATION_FAILED,      // 初始化失败
    SEND_FAILED,               // 发送失败
    INVALID_OPTIONS,           // 选项无效
    PLAYER_OFFLINE             // 玩家离线
}