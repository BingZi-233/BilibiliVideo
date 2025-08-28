package online.bingzi.bilibili.bilibilivideo.internal.database.service

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.*
import online.bingzi.bilibili.bilibilivideo.internal.database.factory.DaoFactory
import taboolib.common.platform.function.submitAsync

/**
 * 数据库服务类
 * 提供统一的数据库操作API，整合所有DAO功能
 */
object DatabaseService {
    
    // DAO实例（延迟初始化）
    private val playerBindingDao by lazy { DaoFactory.getPlayerBindingDao() }
    private val bilibiliAccountDao by lazy { DaoFactory.getBilibiliAccountDao() }
    private val videoTripleStatusDao by lazy { DaoFactory.getVideoTripleStatusDao() }
    private val upFollowStatusDao by lazy { DaoFactory.getUpFollowStatusDao() }
    
    // ===== 玩家绑定相关操作 =====
    
    /**
     * 绑定玩家与MID
     * @param playerUuid 玩家UUID
     * @param mid Bilibili MID
     * @param playerName 玩家名称
     * @param callback 结果回调
     */
    fun bindPlayer(playerUuid: String, mid: Long, playerName: String, callback: (Boolean) -> Unit) {
        submitAsync {
            val currentTime = System.currentTimeMillis()
            val binding = PlayerBinding(
                playerUuid = playerUuid,
                mid = mid,
                createTime = currentTime,
                updateTime = currentTime,
                createPlayer = playerName,
                updatePlayer = playerName
            )
            
            val result = playerBindingDao.insertOrUpdate(binding)
            callback(result)
        }
    }
    
    /**
     * 根据玩家UUID获取绑定的MID
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun getPlayerMid(playerUuid: String, callback: (Long?) -> Unit) {
        submitAsync {
            val binding = playerBindingDao.findByPlayerUuid(playerUuid)
            callback(binding?.mid)
        }
    }
    
    /**
     * 根据MID获取绑定的玩家UUID
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun getPlayerByMid(mid: Long, callback: (String?) -> Unit) {
        submitAsync {
            val binding = playerBindingDao.findByMid(mid)
            callback(binding?.playerUuid)
        }
    }
    
    /**
     * 解除玩家绑定
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun unbindPlayer(playerUuid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            val result = playerBindingDao.delete(playerUuid)
            callback(result)
        }
    }
    
    // ===== Bilibili账户相关操作 =====
    
    /**
     * 保存或更新Bilibili账户信息
     * @param mid Bilibili MID
     * @param nickname 昵称
     * @param sessdata SESSDATA Cookie
     * @param buvid3 buvid3 Cookie
     * @param biliJct bili_jct Cookie
     * @param refreshToken 刷新令牌
     * @param playerName 操作的玩家名称
     * @param callback 结果回调
     */
    fun saveBilibiliAccount(
        mid: Long,
        nickname: String,
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String,
        playerName: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            val currentTime = System.currentTimeMillis()
            val account = BilibiliAccount(
                mid = mid,
                nickname = nickname,
                sessdata = sessdata,
                buvid3 = buvid3,
                biliJct = biliJct,
                refreshToken = refreshToken,
                createTime = currentTime,
                updateTime = currentTime,
                createPlayer = playerName,
                updatePlayer = playerName
            )
            
            val result = bilibiliAccountDao.insertOrUpdate(account)
            callback(result)
        }
    }
    
    /**
     * 根据MID获取Bilibili账户信息
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun getBilibiliAccount(mid: Long, callback: (BilibiliAccount?) -> Unit) {
        submitAsync {
            val account = bilibiliAccountDao.findByMid(mid)
            callback(account)
        }
    }
    
    /**
     * 更新Cookie信息
     * @param mid Bilibili MID
     * @param sessdata SESSDATA Cookie
     * @param buvid3 buvid3 Cookie
     * @param biliJct bili_jct Cookie
     * @param refreshToken 刷新令牌
     * @param playerName 操作的玩家名称
     * @param callback 结果回调
     */
    fun updateCookies(
        mid: Long,
        sessdata: String,
        buvid3: String,
        biliJct: String,
        refreshToken: String,
        playerName: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            val result = bilibiliAccountDao.updateCookies(
                mid, sessdata, buvid3, biliJct, refreshToken, playerName
            )
            callback(result)
        }
    }
    
    // ===== 视频三连状态相关操作 =====
    
    /**
     * 保存或更新视频三连状态
     * @param bvid 视频BV号
     * @param mid Bilibili MID
     * @param playerUuid 玩家UUID
     * @param isLiked 是否点赞
     * @param isCoined 是否投币
     * @param isFavorited 是否收藏
     * @param playerName 操作的玩家名称
     * @param callback 结果回调
     */
    fun saveVideoTripleStatus(
        bvid: String,
        mid: Long,
        playerUuid: String,
        isLiked: Boolean,
        isCoined: Boolean,
        isFavorited: Boolean,
        playerName: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            val currentTime = System.currentTimeMillis()
            val status = VideoTripleStatus(
                bvid = bvid,
                mid = mid,
                playerUuid = playerUuid,
                isLiked = isLiked,
                isCoined = isCoined,
                isFavorited = isFavorited,
                createTime = currentTime,
                updateTime = currentTime,
                createPlayer = playerName,
                updatePlayer = playerName
            )
            
            val result = videoTripleStatusDao.insertOrUpdate(status)
            callback(result)
        }
    }
    
    /**
     * 获取视频三连状态
     * @param bvid 视频BV号
     * @param mid Bilibili MID
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun getVideoTripleStatus(
        bvid: String,
        mid: Long,
        playerUuid: String,
        callback: (VideoTripleStatus?) -> Unit
    ) {
        submitAsync {
            val status = videoTripleStatusDao.findByBvidMidAndPlayer(bvid, mid, playerUuid)
            callback(status)
        }
    }
    
    // ===== UP主关注状态相关操作 =====
    
    /**
     * 保存或更新UP主关注状态
     * @param upMid UP主MID
     * @param followerMid 关注者MID
     * @param playerUuid 玩家UUID
     * @param isFollowing 是否关注
     * @param playerName 操作的玩家名称
     * @param callback 结果回调
     */
    fun saveUpFollowStatus(
        upMid: Long,
        followerMid: Long,
        playerUuid: String,
        isFollowing: Boolean,
        playerName: String,
        callback: (Boolean) -> Unit
    ) {
        submitAsync {
            val currentTime = System.currentTimeMillis()
            val status = UpFollowStatus(
                upMid = upMid,
                followerMid = followerMid,
                playerUuid = playerUuid,
                isFollowing = isFollowing,
                createTime = currentTime,
                updateTime = currentTime,
                createPlayer = playerName,
                updatePlayer = playerName
            )
            
            val result = upFollowStatusDao.insertOrUpdate(status)
            callback(result)
        }
    }
    
    /**
     * 获取UP主关注状态
     * @param upMid UP主MID
     * @param followerMid 关注者MID
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun getUpFollowStatus(
        upMid: Long,
        followerMid: Long,
        playerUuid: String,
        callback: (UpFollowStatus?) -> Unit
    ) {
        submitAsync {
            val status = upFollowStatusDao.findByUpMidFollowerAndPlayer(upMid, followerMid, playerUuid)
            callback(status)
        }
    }
    
    // ===== 便捷方法 =====
    
    /**
     * 检查玩家是否已绑定MID
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun isPlayerBound(playerUuid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            val result = playerBindingDao.exists(playerUuid)
            callback(result)
        }
    }
    
    /**
     * 检查MID是否已被绑定
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun isMidBound(mid: Long, callback: (Boolean) -> Unit) {
        submitAsync {
            val result = playerBindingDao.midExists(mid)
            callback(result)
        }
    }
}