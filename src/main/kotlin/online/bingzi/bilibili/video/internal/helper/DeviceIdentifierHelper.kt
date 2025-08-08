package online.bingzi.bilibili.video.internal.helper

import com.github.benmanes.caffeine.cache.Caffeine
import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.entity.Buvid3Data
import online.bingzi.bilibili.video.internal.entity.BilibiliResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.TimeUnit
import taboolib.common.platform.function.warning

/**
 * 设备标识助手
 * 
 * 用于管理Bilibili的设备标识（buvid3、buvid4等）。
 * 这些标识是风控系统的关键部分，缺失或无效的buvid3可能会触发风控。
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object DeviceIdentifierHelper {
    
    /**
     * buvid3缓存
     * 缓存24小时，避免频繁请求
     */
    private val buvid3Cache = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()
    
    /**
     * buvid4缓存
     * 缓存24小时
     */
    private val buvid4Cache = Caffeine.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, String>()
    
    /**
     * 全局buvid3（用于未登录用户）
     */
    private var globalBuvid3: String? = null
    
    /**
     * 全局buvid4（用于未登录用户）
     */
    private var globalBuvid4: String? = null
    
    /**
     * 获取buvid3
     * 
     * @param forceRefresh 是否强制刷新
     * @return buvid3字符串，如果获取失败返回null
     */
    fun getBuvid3(forceRefresh: Boolean = false): String? {
        // 如果不是强制刷新且缓存存在，直接返回
        if (!forceRefresh && globalBuvid3 != null) {
            return globalBuvid3
        }
        
        // 同步请求获取buvid3
        return try {
            val response = NetworkEngine.bilibiliAPI.getBuvid3().execute()
            if (response.isSuccessful) {
                response.body()?.data?.let { data ->
                    globalBuvid3 = data.buvid3
                    globalBuvid4 = data.buvid4
                    data.buvid3
                }
            } else {
                warning("获取buvid3失败: HTTP ${response.code()}")
                null
            }
        } catch (e: Exception) {
            warning("获取buvid3异常: ${e.message}")
            null
        }
    }
    
    /**
     * 异步获取buvid3
     * 
     * @param callback 回调函数，参数为获取到的buvid3（可能为null）
     */
    fun getBuvid3Async(callback: (String?) -> Unit) {
        NetworkEngine.bilibiliAPI.getBuvid3().enqueue(object : Callback<BilibiliResult<Buvid3Data>> {
            override fun onResponse(
                call: Call<BilibiliResult<Buvid3Data>>,
                response: Response<BilibiliResult<Buvid3Data>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.data?.let { data ->
                        globalBuvid3 = data.buvid3
                        globalBuvid4 = data.buvid4
                        callback(data.buvid3)
                        return
                    }
                }
                callback(null)
            }
            
            override fun onFailure(call: Call<BilibiliResult<Buvid3Data>>, t: Throwable) {
                warning("异步获取buvid3失败: ${t.message}")
                callback(null)
            }
        })
    }
    
    /**
     * 获取用户特定的buvid3
     * 
     * @param userId 用户ID
     * @return buvid3字符串
     */
    fun getUserBuvid3(userId: String): String? {
        return buvid3Cache.getIfPresent(userId) ?: globalBuvid3
    }
    
    /**
     * 设置用户特定的buvid3
     * 
     * @param userId 用户ID
     * @param buvid3 buvid3值
     */
    fun setUserBuvid3(userId: String, buvid3: String) {
        buvid3Cache.put(userId, buvid3)
    }
    
    /**
     * 获取用户特定的buvid4
     * 
     * @param userId 用户ID
     * @return buvid4字符串
     */
    fun getUserBuvid4(userId: String): String? {
        return buvid4Cache.getIfPresent(userId) ?: globalBuvid4
    }
    
    /**
     * 设置用户特定的buvid4
     * 
     * @param userId 用户ID
     * @param buvid4 buvid4值
     */
    fun setUserBuvid4(userId: String, buvid4: String) {
        buvid4Cache.put(userId, buvid4)
    }
    
    /**
     * 构建包含buvid3的Cookie字符串
     * 
     * @param buvid3 buvid3值
     * @return 格式化的Cookie字符串
     */
    fun buildBuvid3Cookie(buvid3: String?): String {
        return buvid3?.let { "buvid3=$it" } ?: ""
    }
    
    /**
     * 构建包含buvid4的Cookie字符串
     * 
     * @param buvid4 buvid4值
     * @return 格式化的Cookie字符串
     */
    fun buildBuvid4Cookie(buvid4: String?): String {
        return buvid4?.let { "buvid4=$it" } ?: ""
    }
    
    /**
     * 初始化设备标识
     * 在插件启动时调用，预先获取全局buvid3
     */
    fun initialize() {
        getBuvid3Async { buvid3 ->
            if (buvid3 != null) {
                taboolib.common.platform.function.info("设备标识初始化成功: buvid3=$buvid3")
            } else {
                warning("设备标识初始化失败，部分功能可能受到影响")
            }
        }
    }
    
    /**
     * 清理缓存
     * 清除所有缓存的设备标识
     */
    fun clearCache() {
        buvid3Cache.invalidateAll()
        buvid4Cache.invalidateAll()
        globalBuvid3 = null
        globalBuvid4 = null
    }
}