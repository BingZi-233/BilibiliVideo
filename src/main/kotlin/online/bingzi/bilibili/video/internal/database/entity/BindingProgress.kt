package online.bingzi.bilibili.video.internal.database.entity

/**
 * 绑定进度数据类
 * 清晰展示用户当前的绑定状态
 */
data class BindingProgress(
    val player: Player,
    val qqBinding: QQBinding?,
    val bilibiliBinding: BilibiliBinding?,
    val bilibiliCookie: BilibiliCookie?
) {
    /**
     * 检查各个绑定步骤的完成状态
     */
    fun hasPlayerInfo(): Boolean = player.isActive
    fun hasQQBinding(): Boolean = qqBinding?.isValidBinding() == true
    fun hasBilibiliBinding(): Boolean = bilibiliBinding?.isValidBinding() == true
    fun hasValidCookie(): Boolean = bilibiliCookie?.isValidCookie() == true

    /**
     * 获取绑定完成度（0-4）
     */
    fun getCompletionStep(): Int {
        var step = 0
        if (hasPlayerInfo()) step++
        if (hasQQBinding()) step++
        if (hasBilibiliBinding()) step++
        if (hasValidCookie()) step++
        return step
    }

    /**
     * 检查是否完成所有绑定
     */
    fun isFullyBound(): Boolean = getCompletionStep() == 4

    /**
     * 获取下一步提示
     */
    fun getNextStepHint(): String {
        return when (getCompletionStep()) {
            0 -> "需要创建玩家档案"
            1 -> "需要绑定QQ号"
            2 -> "需要绑定Bilibili账号"
            3 -> "需要登录Bilibili获取Cookie"
            else -> "绑定已完成"
        }
    }

    /**
     * 获取Cookie字符串（如果有效）
     */
    fun getCookieString(): String? = if (hasValidCookie()) bilibiliCookie?.toCookieString() else null
}