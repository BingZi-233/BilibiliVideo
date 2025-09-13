package online.bingzi.bilibili.bilibilivideo.api.qrcode.result

/**
 * 二维码发送错误代码枚举
 * 
 * 定义了二维码发送过程中可能出现的各种错误类型，便于错误分类和处理。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
enum class ErrorCode {
    /** 依赖缺失，发送器所需的依赖插件未安装或不可用 */
    DEPENDENCY_MISSING,
    /** 发送器不可用，发送器未正确初始化或已被禁用 */
    SENDER_NOT_AVAILABLE,
    /** 初始化失败，发送器在初始化过程中发生错误 */
    INITIALIZATION_FAILED,
    /** 发送失败，在发送过程中发生错误 */
    SEND_FAILED,
    /** 选项无效，提供的发送选项参数不正确 */
    INVALID_OPTIONS,
    /** 玩家离线，目标玩家不在线无法接收二维码 */
    PLAYER_OFFLINE
}