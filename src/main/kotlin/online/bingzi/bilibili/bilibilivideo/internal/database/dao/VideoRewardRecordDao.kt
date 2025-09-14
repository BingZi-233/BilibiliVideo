package online.bingzi.bilibili.bilibilivideo.internal.database.dao

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.VideoRewardRecord

/**
 * 视频奖励记录数据访问接口
 * 
 * 定义视频奖励记录的基本数据操作方法。
 * 支持MySQL和SQLite两种数据库实现。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
interface VideoRewardRecordDao {
    
    /**
     * 创建奖励记录
     * 
     * @param record 奖励记录实体
     * @return Boolean 是否创建成功
     */
    suspend fun createRecord(record: VideoRewardRecord): Boolean
    
    /**
     * 根据玩家和BV号查询奖励记录
     * 
     * @param bvid 视频BV号
     * @param playerUuid 玩家UUID
     * @return VideoRewardRecord? 奖励记录，如果不存在则返回null
     */
    suspend fun getRecordByBvidAndPlayer(bvid: String, playerUuid: String): VideoRewardRecord?
    
    /**
     * 检查玩家是否已领取过该视频的奖励
     * 
     * @param bvid 视频BV号
     * @param playerUuid 玩家UUID
     * @return Boolean 是否已领取过奖励
     */
    suspend fun hasRewardRecord(bvid: String, playerUuid: String): Boolean
    
    /**
     * 根据玩家UUID查询所有奖励记录
     * 
     * @param playerUuid 玩家UUID
     * @param limit 限制返回数量，默认100
     * @param offset 偏移量，默认0
     * @return List<VideoRewardRecord> 奖励记录列表
     */
    suspend fun getRecordsByPlayer(
        playerUuid: String, 
        limit: Int = 100, 
        offset: Int = 0
    ): List<VideoRewardRecord>
    
    /**
     * 根据BV号查询所有奖励记录
     * 
     * @param bvid 视频BV号
     * @param limit 限制返回数量，默认100
     * @param offset 偏移量，默认0
     * @return List<VideoRewardRecord> 奖励记录列表
     */
    suspend fun getRecordsByBvid(
        bvid: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<VideoRewardRecord>
    
    /**
     * 统计玩家的奖励记录总数
     * 
     * @param playerUuid 玩家UUID
     * @return Long 记录总数
     */
    suspend fun countRecordsByPlayer(playerUuid: String): Long
    
    /**
     * 统计某个BV号的奖励记录总数
     * 
     * @param bvid 视频BV号
     * @return Long 记录总数
     */
    suspend fun countRecordsByBvid(bvid: String): Long
    
    /**
     * 获取最新的奖励记录
     * 
     * @param limit 限制返回数量，默认10
     * @return List<VideoRewardRecord> 最新的奖励记录列表
     */
    suspend fun getLatestRecords(limit: Int = 10): List<VideoRewardRecord>
    
    /**
     * 根据奖励类型查询记录
     * 
     * @param rewardType 奖励类型
     * @param limit 限制返回数量，默认100
     * @param offset 偏移量，默认0
     * @return List<VideoRewardRecord> 奖励记录列表
     */
    suspend fun getRecordsByRewardType(
        rewardType: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<VideoRewardRecord>
    
    /**
     * 删除指定时间之前的记录（数据清理）
     * 
     * @param beforeTime 时间戳（毫秒），删除此时间之前的记录
     * @return Long 删除的记录数量
     */
    suspend fun deleteRecordsBefore(beforeTime: Long): Long
    
    /**
     * 更新记录
     * 
     * @param record 要更新的记录
     * @return Boolean 是否更新成功
     */
    suspend fun updateRecord(record: VideoRewardRecord): Boolean
    
    /**
     * 根据ID删除记录
     * 
     * @param id 记录ID
     * @return Boolean 是否删除成功
     */
    suspend fun deleteRecordById(id: Long): Boolean
}