package online.bingzi.bilibili.bilibilivideo.internal.database.service

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.BilibiliAccount
import online.bingzi.bilibili.bilibilivideo.internal.database.entity.UpFollowStatus
import online.bingzi.bilibili.bilibilivideo.internal.database.entity.VideoTripleStatus
import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import taboolib.common.platform.function.severe
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.submitAsync
import taboolib.module.database.Table
import javax.sql.DataSource

/**
 * 数据库服务类
 * 
 * 提供统一的异步数据库操作API，封装所有数据访问逻辑。
 * 直接使用TabooLib Table API进行数据库操作，支持MySQL和SQLite。
 * 所有操作均为异步执行，通过回调函数返回结果。
 * 
 * 主要功能：
 * - 玩家-MID绑定管理
 * - Bilibili账户信息存储
 * - 视频三连状态跟踪
 * - UP主关注状态管理
 * - 便捷查询方法
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object DatabaseService {
    
    // ===== 玩家绑定相关操作 =====
    
    /**
     * 绑定玩家与Bilibili MID
     * 
     * 在数据库中建立Minecraft玩家UUID与Bilibili MID的一对一绑定关系。
     * 如果已存在绑定记录则更新，否则创建新记录。
     * 
     * @param playerUuid 玩家UUID字符串
     * @param mid Bilibili用户MID
     * @param playerName 玩家名称（用于审计日志）
     * @param callback 异步回调，参数为操作是否成功
     */
    fun bindPlayer(playerUuid: String, mid: Long, playerName: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 查询是否已存在记录
                val existingRecords = table.select(dataSource) {
                    where { "player_uuid" eq playerUuid }
                }
                
                val result = if (existingRecords.find()) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("mid", mid)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where { "player_uuid" eq playerUuid }
                    } > 0
                } else {
                    // 插入新记录
                    table.insertRow(
                        dataSource,
                        "player_uuid" to playerUuid,
                        "mid" to mid,
                        "create_time" to currentTime,
                        "update_time" to currentTime,
                        "create_player" to playerName,
                        "update_player" to playerName
                    ) > 0
                }
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("绑定玩家失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * 根据玩家UUID获取绑定的MID
     */
    fun getPlayerMid(playerUuid: String, callback: (Long?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "player_uuid" eq playerUuid }
                }
                
                val mid = results.firstOrNull { 
                    getLong("mid")
                }
                
                submit { callback(mid) }
            } catch (e: Exception) {
                submit {
                    severe("查询玩家MID失败: ${e.message}")
                    callback(null)
                }
            }
        }
    }
    
    /**
     * 根据MID获取绑定的玩家UUID
     */
    fun getPlayerByMid(mid: Long, callback: (String?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "mid" eq mid }
                }
                
                val playerUuid = results.firstOrNull {
                    getString("player_uuid")
                }
                
                submit { callback(playerUuid) }
            } catch (e: Exception) {
                submit {
                    severe("根据MID查询玩家失败: ${e.message}")
                    callback(null)
                }
            }
        }
    }
    
    /**
     * 解除玩家绑定
     */
    fun unbindPlayer(playerUuid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.delete(dataSource) {
                    where { "player_uuid" eq playerUuid }
                } > 0
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("解除玩家绑定失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    // ===== Bilibili账户相关操作 =====
    
    /**
     * 保存或更新Bilibili账户信息
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
            try {
                val table = TableFactory.getBilibiliAccountTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 查询是否已存在记录
                val existingRecords = table.select(dataSource) {
                    where { "mid" eq mid }
                }
                
                val result = if (existingRecords.find()) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("nickname", nickname)
                        set("sessdata", sessdata)
                        set("buvid3", buvid3)
                        set("bili_jct", biliJct)
                        set("refresh_token", refreshToken)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where { "mid" eq mid }
                    } > 0
                } else {
                    // 插入新记录
                    table.insertRow(
                        dataSource,
                        "mid" to mid,
                        "nickname" to nickname,
                        "sessdata" to sessdata,
                        "buvid3" to buvid3,
                        "bili_jct" to biliJct,
                        "refresh_token" to refreshToken,
                        "create_time" to currentTime,
                        "update_time" to currentTime,
                        "create_player" to playerName,
                        "update_player" to playerName
                    ) > 0
                }
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("保存Bilibili账户信息失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * 根据MID获取Bilibili账户信息
     */
    fun getBilibiliAccount(mid: Long, callback: (BilibiliAccount?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getBilibiliAccountTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "mid" eq mid }
                }
                
                val account = results.firstOrNull {
                    BilibiliAccount(
                        mid = getLong("mid"),
                        nickname = getString("nickname"),
                        sessdata = getString("sessdata"),
                        buvid3 = getString("buvid3"),
                        biliJct = getString("bili_jct"),
                        refreshToken = getString("refresh_token"),
                        createTime = getLong("create_time"),
                        updateTime = getLong("update_time"),
                        createPlayer = getString("create_player"),
                        updatePlayer = getString("update_player")
                    )
                }
                
                submit { callback(account) }
            } catch (e: Exception) {
                submit {
                    severe("获取Bilibili账户信息失败: ${e.message}")
                    callback(null)
                }
            }
        }
    }
    
    /**
     * 更新Cookie信息
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
            try {
                val table = TableFactory.getBilibiliAccountTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                val result = table.update(dataSource) {
                    set("sessdata", sessdata)
                    set("buvid3", buvid3)
                    set("bili_jct", biliJct)
                    set("refresh_token", refreshToken)
                    set("update_time", currentTime)
                    set("update_player", playerName)
                    where { "mid" eq mid }
                } > 0
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("更新Cookie信息失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    // ===== 视频三连状态相关操作 =====
    
    /**
     * 保存或更新视频三连状态
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
            try {
                val table = TableFactory.getVideoTripleStatusTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 查询是否已存在记录
                val existingRecords = table.select(dataSource) {
                    where { "bvid" eq bvid; "mid" eq mid; "player_uuid" eq playerUuid }
                }
                
                val result = if (existingRecords.find()) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("is_liked", if (isLiked) 1 else 0)
                        set("is_coined", if (isCoined) 1 else 0)
                        set("is_favorited", if (isFavorited) 1 else 0)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where { "bvid" eq bvid; "mid" eq mid; "player_uuid" eq playerUuid }
                    } > 0
                } else {
                    // 插入新记录
                    table.insertRow(
                        dataSource,
                        "bvid" to bvid,
                        "mid" to mid,
                        "player_uuid" to playerUuid,
                        "is_liked" to if (isLiked) 1 else 0,
                        "is_coined" to if (isCoined) 1 else 0,
                        "is_favorited" to if (isFavorited) 1 else 0,
                        "create_time" to currentTime,
                        "update_time" to currentTime,
                        "create_player" to playerName,
                        "update_player" to playerName
                    ) > 0
                }
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("保存视频三连状态失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * 获取视频三连状态
     */
    fun getVideoTripleStatus(
        bvid: String,
        mid: Long,
        playerUuid: String,
        callback: (VideoTripleStatus?) -> Unit
    ) {
        submitAsync {
            try {
                val table = TableFactory.getVideoTripleStatusTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "bvid" eq bvid; "mid" eq mid; "player_uuid" eq playerUuid }
                }
                
                val status = results.firstOrNull {
                    VideoTripleStatus(
                        id = getLong("id"),
                        bvid = getString("bvid"),
                        mid = getLong("mid"),
                        playerUuid = getString("player_uuid"),
                        isLiked = getInt("is_liked") == 1,
                        isCoined = getInt("is_coined") == 1,
                        isFavorited = getInt("is_favorited") == 1,
                        createTime = getLong("create_time"),
                        updateTime = getLong("update_time"),
                        createPlayer = getString("create_player"),
                        updatePlayer = getString("update_player")
                    )
                }
                
                submit { callback(status) }
            } catch (e: Exception) {
                submit {
                    severe("获取视频三连状态失败: ${e.message}")
                    callback(null)
                }
            }
        }
    }
    
    // ===== UP主关注状态相关操作 =====
    
    /**
     * 保存或更新UP主关注状态
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
            try {
                val table = TableFactory.getUpFollowStatusTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 查询是否已存在记录
                val existingRecords = table.select(dataSource) {
                    where { "up_mid" eq upMid; "follower_mid" eq followerMid; "player_uuid" eq playerUuid }
                }
                
                val result = if (existingRecords.find()) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("is_following", if (isFollowing) 1 else 0)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where { "up_mid" eq upMid; "follower_mid" eq followerMid; "player_uuid" eq playerUuid }
                    } > 0
                } else {
                    // 插入新记录
                    table.insertRow(
                        dataSource,
                        "up_mid" to upMid,
                        "follower_mid" to followerMid,
                        "player_uuid" to playerUuid,
                        "is_following" to if (isFollowing) 1 else 0,
                        "create_time" to currentTime,
                        "update_time" to currentTime,
                        "create_player" to playerName,
                        "update_player" to playerName
                    ) > 0
                }
                
                submit { callback(result) }
            } catch (e: Exception) {
                submit {
                    severe("保存UP主关注状态失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * 获取UP主关注状态
     */
    fun getUpFollowStatus(
        upMid: Long,
        followerMid: Long,
        playerUuid: String,
        callback: (UpFollowStatus?) -> Unit
    ) {
        submitAsync {
            try {
                val table = TableFactory.getUpFollowStatusTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "up_mid" eq upMid; "follower_mid" eq followerMid; "player_uuid" eq playerUuid }
                }
                
                val status = results.firstOrNull {
                    UpFollowStatus(
                        id = getLong("id"),
                        upMid = getLong("up_mid"),
                        followerMid = getLong("follower_mid"),
                        playerUuid = getString("player_uuid"),
                        isFollowing = getInt("is_following") == 1,
                        createTime = getLong("create_time"),
                        updateTime = getLong("update_time"),
                        createPlayer = getString("create_player"),
                        updatePlayer = getString("update_player")
                    )
                }
                
                submit { callback(status) }
            } catch (e: Exception) {
                submit {
                    severe("获取UP主关注状态失败: ${e.message}")
                    callback(null)
                }
            }
        }
    }
    
    // ===== 便捷方法 =====
    
    /**
     * 检查玩家是否已绑定MID
     */
    fun isPlayerBound(playerUuid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "player_uuid" eq playerUuid }
                }
                
                submit { callback(results.find()) }
            } catch (e: Exception) {
                submit {
                    severe("检查玩家绑定状态失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }
    
    /**
     * 检查MID是否已被绑定
     */
    fun isMidBound(mid: Long, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val results = table.select(dataSource) {
                    where { "mid" eq mid }
                }
                
                submit { callback(results.find()) }
            } catch (e: Exception) {
                submit {
                    severe("检查MID绑定状态失败: ${e.message}")
                    callback(false)
                }
            }
        }
    }

    private fun Table<*, *>.insertRow(
        dataSource: DataSource,
        vararg entries: Pair<String, Any?>
    ): Int {
        if (entries.isEmpty()) {
            return 0
        }
        val keyArray = entries.map { it.first }.toTypedArray()
        val valueArray = entries.map { it.second }.toTypedArray()
        return insert(dataSource, *keyArray) {
            value(*valueArray)
        }
    }
}
