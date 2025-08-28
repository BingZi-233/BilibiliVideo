package online.bingzi.bilibili.bilibilivideo.internal.database.dao

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.PlayerBinding

/**
 * 玩家-MID绑定数据访问接口
 * 定义玩家与Bilibili MID的绑定关系的数据库操作
 */
interface PlayerBindingDao {
    
    /**
     * 插入或更新玩家绑定
     * @param binding 玩家绑定对象
     * @return 是否成功
     */
    fun insertOrUpdate(binding: PlayerBinding): Boolean
    
    /**
     * 根据玩家UUID查找绑定
     * @param playerUuid 玩家UUID
     * @return 玩家绑定对象，不存在返回null
     */
    fun findByPlayerUuid(playerUuid: String): PlayerBinding?
    
    /**
     * 根据MID查找绑定
     * @param mid Bilibili MID
     * @return 玩家绑定对象，不存在返回null
     */
    fun findByMid(mid: Long): PlayerBinding?
    
    /**
     * 更新玩家绑定
     * @param binding 玩家绑定对象
     * @return 是否成功
     */
    fun update(binding: PlayerBinding): Boolean
    
    /**
     * 删除玩家绑定
     * @param playerUuid 玩家UUID
     * @return 是否成功
     */
    fun delete(playerUuid: String): Boolean
    
    /**
     * 检查玩家是否已绑定
     * @param playerUuid 玩家UUID
     * @return 是否已绑定
     */
    fun exists(playerUuid: String): Boolean
    
    /**
     * 检查MID是否已绑定
     * @param mid Bilibili MID
     * @return 是否已绑定
     */
    fun midExists(mid: Long): Boolean
}