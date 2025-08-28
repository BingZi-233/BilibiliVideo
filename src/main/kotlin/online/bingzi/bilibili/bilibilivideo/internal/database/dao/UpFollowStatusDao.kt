package online.bingzi.bilibili.bilibilivideo.internal.database.dao

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.UpFollowStatus

/**
 * UP主关注状态数据访问接口
 * 定义UP主关注状态的数据库操作
 */
interface UpFollowStatusDao {
    
    /**
     * 插入或更新UP主关注状态
     * @param status 关注状态对象
     * @return 是否成功
     */
    fun insertOrUpdate(status: UpFollowStatus): Boolean
    
    /**
     * 根据UP主MID、关注者MID和玩家UUID查找关注状态
     * @param upMid UP主MID
     * @param followerMid 关注者MID
     * @param playerUuid 玩家UUID
     * @return 关注状态对象，不存在返回null
     */
    fun findByUpMidFollowerAndPlayer(upMid: Long, followerMid: Long, playerUuid: String): UpFollowStatus?
    
    /**
     * 根据玩家UUID查找所有关注状态
     * @param playerUuid 玩家UUID
     * @return 关注状态列表
     */
    fun findByPlayer(playerUuid: String): List<UpFollowStatus>
    
    /**
     * 根据UP主MID查找所有关注状态
     * @param upMid UP主MID
     * @return 关注状态列表
     */
    fun findByUpMid(upMid: Long): List<UpFollowStatus>
    
    /**
     * 根据关注者MID查找所有关注状态
     * @param followerMid 关注者MID
     * @return 关注状态列表
     */
    fun findByFollowerMid(followerMid: Long): List<UpFollowStatus>
    
    /**
     * 更新关注状态
     * @param status 关注状态对象
     * @return 是否成功
     */
    fun update(status: UpFollowStatus): Boolean
    
    /**
     * 删除关注状态
     * @param id 主键
     * @return 是否成功
     */
    fun delete(id: Long): Boolean
    
    /**
     * 检查关注状态是否存在
     * @param upMid UP主MID
     * @param followerMid 关注者MID
     * @param playerUuid 玩家UUID
     * @return 是否存在
     */
    fun exists(upMid: Long, followerMid: Long, playerUuid: String): Boolean
    
    /**
     * 更新关注状态
     * @param upMid UP主MID
     * @param followerMid 关注者MID
     * @param playerUuid 玩家UUID
     * @param isFollowing 是否关注
     * @param updatePlayer 更新玩家
     * @return 是否成功
     */
    fun updateFollowStatus(
        upMid: Long,
        followerMid: Long,
        playerUuid: String,
        isFollowing: Boolean,
        updatePlayer: String
    ): Boolean
}