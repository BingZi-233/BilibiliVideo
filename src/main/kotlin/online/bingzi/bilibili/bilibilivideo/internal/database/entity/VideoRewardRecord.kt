package online.bingzi.bilibili.bilibilivideo.internal.database.entity

/**
 * 视频奖励记录实体类
 * 
 * 用于记录玩家对特定视频领取奖励的历史记录。
 * 防止玩家重复领取同一视频的奖励。
 * 
 * @param id 记录ID（自增主键）
 * @param bvid 视频BV号
 * @param mid 用户Bilibili MID
 * @param playerUuid 玩家UUID
 * @param rewardType 奖励类型（specific: 特定视频奖励, default: 默认奖励）
 * @param rewardData 奖励详细数据（JSON格式）
 * @param isLiked 当时的点赞状态
 * @param isCoined 当时的投币状态
 * @param isFavorited 当时的收藏状态
 * @param createTime 创建时间（毫秒时间戳）
 * @param updateTime 更新时间（毫秒时间戳）
 * @param createPlayer 创建记录的玩家名
 * @param updatePlayer 更新记录的玩家名
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
data class VideoRewardRecord(
    val id: Long = 0L,
    val bvid: String,
    val mid: Long,
    val playerUuid: String,
    val rewardType: String,
    val rewardData: String? = null,
    val isLiked: Boolean,
    val isCoined: Boolean,
    val isFavorited: Boolean,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis(),
    val createPlayer: String,
    val updatePlayer: String
) {
    /**
     * 检查当时是否完成了完整三连
     * 
     * @return Boolean 是否完成点赞、投币、收藏三个操作
     */
    fun hasCompleteTriple(): Boolean {
        return isLiked && isCoined && isFavorited
    }
    
    /**
     * 检查当时是否有任何三连操作
     * 
     * @return Boolean 是否有任何一项操作（点赞、投币或收藏）
     */
    fun hasAnyTripleAction(): Boolean {
        return isLiked || isCoined || isFavorited
    }
    
    /**
     * 获取三连状态描述
     * 
     * @return String 状态描述文本
     */
    fun getTripleStatusDescription(): String {
        val actions = mutableListOf<String>()
        if (isLiked) actions.add("点赞")
        if (isCoined) actions.add("投币") 
        if (isFavorited) actions.add("收藏")
        
        return if (actions.isEmpty()) {
            "无操作"
        } else {
            actions.joinToString("、")
        }
    }
}