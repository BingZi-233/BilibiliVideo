package online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata

/**
 * 依赖状态枚举
 * 
 * 定义了二维码发送器依赖项的各种状态，用于依赖检查和诊断。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
enum class DependencyStatus {
    /** 依赖存在且可用 */
    PRESENT,
    /** 依赖缺失，未安装或找不到 */
    MISSING,
    /** 版本不匹配，已安装但版本不符合要求 */
    VERSION_MISMATCH,
    /** 依赖被禁用，已安装但被人工禁用 */
    DISABLED
}