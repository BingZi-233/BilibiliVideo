package online.bingzi.bilibili.video.api.event.reward

import taboolib.common.platform.event.ProxyEvent

/**
 * 奖励检查事件
 * 在系统检查奖励资格时触发
 */
class RewardCheckEvent(
    /**
     * 玩家UUID
     */
    val playerUuid: String,
    
    /**
     * 视频BV号
     */
    val bvId: String,
    
    /**
     * 检查类型
     */
    val checkType: CheckType,
    
    /**
     * 检查结果
     */
    var result: CheckResult? = null,
    
    /**
     * 事件时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
) : ProxyEvent() {

    /**
     * 检查类型枚举
     */
    enum class CheckType {
        /**
         * 尝试领取奖励
         */
        CLAIM_ATTEMPT,
        
        /**
         * 查询奖励列表
         */
        LIST_QUERY,
        
        /**
         * 定期检查
         */
        SCHEDULED_CHECK,
        
        /**
         * 手动检查
         */
        MANUAL_CHECK
    }

    /**
     * 检查结果
     */
    data class CheckResult(
        /**
         * 是否符合条件
         */
        val eligible: Boolean,
        
        /**
         * 检查原因
         */
        val reason: String,
        
        /**
         * 检查详情
         */
        val details: Map<String, Any> = emptyMap()
    )

    /**
     * 设置检查结果
     */
    fun setResult(eligible: Boolean, reason: String, details: Map<String, Any> = emptyMap()) {
        this.result = CheckResult(eligible, reason, details)
    }

    /**
     * 是否通过检查
     */
    fun isPassed(): Boolean = result?.eligible ?: false

    /**
     * 获取检查原因
     */
    fun getReason(): String = result?.reason ?: "未知"

    /**
     * 获取事件描述
     */
    fun getDescription(): String {
        return "奖励检查事件：玩家 $playerUuid，视频 $bvId，类型 $checkType"
    }

    override fun toString(): String {
        return "RewardCheckEvent(playerUuid='$playerUuid', bvId='$bvId', checkType=$checkType, result=$result)"
    }
}