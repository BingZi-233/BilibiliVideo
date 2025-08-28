package online.bingzi.bilibili.bilibilivideo.internal.database.dao

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.BilibiliAccount

/**
 * Bilibili账户信息数据访问接口
 * 定义Bilibili账户信息的数据库操作
 */
interface BilibiliAccountDao {
    
    /**
     * 插入或更新Bilibili账户信息
     * @param account 账户信息对象
     * @return 是否成功
     */
    fun insertOrUpdate(account: BilibiliAccount): Boolean
    
    /**
     * 根据MID查找账户信息
     * @param mid Bilibili MID
     * @return 账户信息对象，不存在返回null
     */
    fun findByMid(mid: Long): BilibiliAccount?
    
    /**
     * 更新账户信息
     * @param account 账户信息对象
     * @return 是否成功
     */
    fun update(account: BilibiliAccount): Boolean
    
    /**
     * 删除账户信息
     * @param mid Bilibili MID
     * @return 是否成功
     */
    fun delete(mid: Long): Boolean
    
    /**
     * 检查账户是否存在
     * @param mid Bilibili MID
     * @return 是否存在
     */
    fun exists(mid: Long): Boolean
    
    /**
     * 更新Cookie信息
     * @param mid Bilibili MID
     * @param sessdata SESSDATA Cookie
     * @param buvid3 buvid3 Cookie
     * @param biliJct bili_jct Cookie
     * @param refreshToken 刷新令牌
     * @param updatePlayer 更新玩家
     * @return 是否成功
     */
    fun updateCookies(
        mid: Long,
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String,
        updatePlayer: String
    ): Boolean
    
    /**
     * 更新昵称
     * @param mid Bilibili MID
     * @param nickname 新昵称
     * @param updatePlayer 更新玩家
     * @return 是否成功
     */
    fun updateNickname(mid: Long, nickname: String, updatePlayer: String): Boolean
}