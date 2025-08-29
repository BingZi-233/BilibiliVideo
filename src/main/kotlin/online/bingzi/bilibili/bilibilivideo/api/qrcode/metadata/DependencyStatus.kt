package online.bingzi.bilibili.bilibilivideo.api.qrcode.metadata

enum class DependencyStatus {
    PRESENT,            // 依赖存在且可用
    MISSING,            // 依赖缺失
    VERSION_MISMATCH,   // 版本不匹配
    DISABLED            // 依赖被禁用
}