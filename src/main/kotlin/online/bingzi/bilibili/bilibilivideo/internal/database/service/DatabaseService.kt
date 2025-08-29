package online.bingzi.bilibili.bilibilivideo.internal.database.service

import online.bingzi.bilibili.bilibilivideo.internal.database.entity.*
import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import taboolib.common.platform.function.submitAsync

/**
 * 数据库服务类
 * 提供统一的数据库操作API，直接使用Table API进行操作
 */
object DatabaseService {
    
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
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 使用插入或更新逻辑
                val existingRecord = table.select(dataSource) {
                    where("player_uuid", playerUuid)
                }.firstOrNull()
                
                val result = if (existingRecord != null) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("mid", mid)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where("player_uuid", playerUuid)
                    } > 0
                } else {
                    // 插入新记录
                    table.insert(dataSource) {
                        value("player_uuid", playerUuid)
                        value("mid", mid)
                        value("create_time", currentTime)
                        value("update_time", currentTime)
                        value("create_player", playerName)
                        value("update_player", playerName)
                    } > 0
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * 根据玩家UUID获取绑定的MID
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun getPlayerMid(playerUuid: String, callback: (Long?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("player_uuid", playerUuid)
                }.firstOrNull()?.let { row ->
                    row["mid"].asLong()
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
    
    /**
     * 根据MID获取绑定的玩家UUID
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun getPlayerByMid(mid: Long, callback: (String?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("mid", mid)
                }.firstOrNull()?.let { row ->
                    row["player_uuid"].asString()
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
        }
    }
    
    /**
     * 解除玩家绑定
     * @param playerUuid 玩家UUID
     * @param callback 结果回调
     */
    fun unbindPlayer(playerUuid: String, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.delete(dataSource) {
                    where("player_uuid", playerUuid)
                } > 0
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
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
            try {
                val table = TableFactory.getBilibiliAccountTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 使用插入或更新逻辑
                val existingRecord = table.select(dataSource) {
                    where("mid", mid)
                }.firstOrNull()
                
                val result = if (existingRecord != null) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("nickname", nickname)
                        set("sessdata", sessdata)
                        set("buvid3", buvid3)
                        set("bili_jct", biliJct)
                        set("refresh_token", refreshToken)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where("mid", mid)
                    } > 0
                } else {
                    // 插入新记录
                    table.insert(dataSource) {
                        value("mid", mid)
                        value("nickname", nickname)
                        value("sessdata", sessdata)
                        value("buvid3", buvid3)
                        value("bili_jct", biliJct)
                        value("refresh_token", refreshToken)
                        value("create_time", currentTime)
                        value("update_time", currentTime)
                        value("create_player", playerName)
                        value("update_player", playerName)
                    } > 0
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * 根据MID获取Bilibili账户信息
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun getBilibiliAccount(mid: Long, callback: (BilibiliAccount?) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getBilibiliAccountTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("mid", mid)
                }.firstOrNull()?.let { row ->
                    BilibiliAccount(
                        mid = row["mid"].asLong(),
                        nickname = row["nickname"].asString(),
                        sessdata = row["sessdata"].asString(),
                        buvid3 = row["buvid3"].asString(),
                        biliJct = row["bili_jct"].asString(),
                        refreshToken = row["refresh_token"].asString(),
                        createTime = row["create_time"].asLong(),
                        updateTime = row["update_time"].asLong(),
                        createPlayer = row["create_player"].asString(),
                        updatePlayer = row["update_player"].asString()
                    )
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
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
                    where("mid", mid)
                } > 0
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
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
            try {
                val table = TableFactory.getVideoTripleStatusTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 使用插入或更新逻辑
                val existingRecord = table.select(dataSource) {
                    where("bvid", bvid)
                    where("mid", mid)
                    where("player_uuid", playerUuid)
                }.firstOrNull()
                
                val result = if (existingRecord != null) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("is_liked", if (isLiked) 1 else 0)
                        set("is_coined", if (isCoined) 1 else 0)
                        set("is_favorited", if (isFavorited) 1 else 0)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where("bvid", bvid)
                        where("mid", mid)
                        where("player_uuid", playerUuid)
                    } > 0
                } else {
                    // 插入新记录
                    table.insert(dataSource) {
                        value("bvid", bvid)
                        value("mid", mid)
                        value("player_uuid", playerUuid)
                        value("is_liked", if (isLiked) 1 else 0)
                        value("is_coined", if (isCoined) 1 else 0)
                        value("is_favorited", if (isFavorited) 1 else 0)
                        value("create_time", currentTime)
                        value("update_time", currentTime)
                        value("create_player", playerName)
                        value("update_player", playerName)
                    } > 0
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
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
            try {
                val table = TableFactory.getVideoTripleStatusTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("bvid", bvid)
                    where("mid", mid)
                    where("player_uuid", playerUuid)
                }.firstOrNull()?.let { row ->
                    VideoTripleStatus(
                        bvid = row["bvid"].asString(),
                        mid = row["mid"].asLong(),
                        playerUuid = row["player_uuid"].asString(),
                        isLiked = row["is_liked"].asInt() == 1,
                        isCoined = row["is_coined"].asInt() == 1,
                        isFavorited = row["is_favorited"].asInt() == 1,
                        createTime = row["create_time"].asLong(),
                        updateTime = row["update_time"].asLong(),
                        createPlayer = row["create_player"].asString(),
                        updatePlayer = row["update_player"].asString()
                    )
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
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
            try {
                val table = TableFactory.getUpFollowStatusTable()
                val dataSource = TableFactory.getDataSource()
                val currentTime = System.currentTimeMillis()
                
                // 使用插入或更新逻辑
                val existingRecord = table.select(dataSource) {
                    where("up_mid", upMid)
                    where("follower_mid", followerMid)
                    where("player_uuid", playerUuid)
                }.firstOrNull()
                
                val result = if (existingRecord != null) {
                    // 更新现有记录
                    table.update(dataSource) {
                        set("is_following", if (isFollowing) 1 else 0)
                        set("update_time", currentTime)
                        set("update_player", playerName)
                        where("up_mid", upMid)
                        where("follower_mid", followerMid)
                        where("player_uuid", playerUuid)
                    } > 0
                } else {
                    // 插入新记录
                    table.insert(dataSource) {
                        value("up_mid", upMid)
                        value("follower_mid", followerMid)
                        value("player_uuid", playerUuid)
                        value("is_following", if (isFollowing) 1 else 0)
                        value("create_time", currentTime)
                        value("update_time", currentTime)
                        value("create_player", playerName)
                        value("update_player", playerName)
                    } > 0
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
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
            try {
                val table = TableFactory.getUpFollowStatusTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("up_mid", upMid)
                    where("follower_mid", followerMid)
                    where("player_uuid", playerUuid)
                }.firstOrNull()?.let { row ->
                    UpFollowStatus(
                        upMid = row["up_mid"].asLong(),
                        followerMid = row["follower_mid"].asLong(),
                        playerUuid = row["player_uuid"].asString(),
                        isFollowing = row["is_following"].asInt() == 1,
                        createTime = row["create_time"].asLong(),
                        updateTime = row["update_time"].asLong(),
                        createPlayer = row["create_player"].asString(),
                        updatePlayer = row["update_player"].asString()
                    )
                }
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(null)
            }
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
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("player_uuid", playerUuid)
                }.isNotEmpty()
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * 检查MID是否已被绑定
     * @param mid Bilibili MID
     * @param callback 结果回调
     */
    fun isMidBound(mid: Long, callback: (Boolean) -> Unit) {
        submitAsync {
            try {
                val table = TableFactory.getPlayerBindingTable()
                val dataSource = TableFactory.getDataSource()
                
                val result = table.select(dataSource) {
                    where("mid", mid)
                }.isNotEmpty()
                
                callback(result)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
}