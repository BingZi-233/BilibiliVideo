package online.bingzi.bilibili.bilibilivideo.internal.database.dao

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.VideoTripleStatus

/**
 * 视频三连状态数据访问接口
 * 定义视频三连状态的数据库操作
 */
interface VideoTripleStatusDao {
    
    /**
     * 插入或更新视频三连状态
     * @param status 三连状态对象
     * @return 是否成功
     */
    fun insertOrUpdate(status: VideoTripleStatus): Boolean
    
    /**
     * 根据BV号、MID和玩家UUID查找三连状态
     * @param bvid 视频BV号
     * @param mid Bilibili MID
     * @param playerUuid 玩家UUID
     * @return 三连状态对象，不存在返回null
     */
    fun findByBvidMidAndPlayer(bvid: String, mid: Long, playerUuid: String): VideoTripleStatus?
    
    /**
     * 根据玩家UUID查找所有三连状态
     * @param playerUuid 玩家UUID
     * @return 三连状态列表
     */
    fun findByPlayer(playerUuid: String): List<VideoTripleStatus>
    
    /**
     * 根据BV号查找所有三连状态
     * @param bvid 视频BV号
     * @return 三连状态列表
     */
    fun findByBvid(bvid: String): List<VideoTripleStatus>
    
    /**
     * 更新三连状态
     * @param status 三连状态对象
     * @return 是否成功
     */
    fun update(status: VideoTripleStatus): Boolean
    
    /**
     * 删除三连状态
     * @param id 主键
     * @return 是否成功
     */
    fun delete(id: Long): Boolean
    
    /**
     * 检查三连状态是否存在
     * @param bvid 视频BV号
     * @param mid Bilibili MID
     * @param playerUuid 玩家UUID
     * @return 是否存在
     */
    fun exists(bvid: String, mid: Long, playerUuid: String): Boolean
    
    /**
     * 更新单个三连状态
     * @param bvid 视频BV号
     * @param mid Bilibili MID
     * @param playerUuid 玩家UUID
     * @param isLiked 是否点赞
     * @param isCoined 是否投币
     * @param isFavorited 是否收藏
     * @param updatePlayer 更新玩家
     * @return 是否成功
     */
    fun updateTripleStatus(
        bvid: String,
        mid: Long,
        playerUuid: String,
        isLiked: Boolean,
        isCoined: Boolean,
        isFavorited: Boolean,
        updatePlayer: String
    ): Boolean
}