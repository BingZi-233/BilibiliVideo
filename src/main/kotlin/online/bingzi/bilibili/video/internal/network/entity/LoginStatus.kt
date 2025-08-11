package online.bingzi.bilibili.video.internal.network.entity

/**
 * 登录状态枚举
 */
enum class LoginStatus {
    WAITING_FOR_SCAN,      // 等待扫码
    WAITING_FOR_CONFIRM,   // 等待确认
    SUCCESS,               // 登录成功
    EXPIRED,               // 二维码过期
    FAILED                 // 登录失败
}