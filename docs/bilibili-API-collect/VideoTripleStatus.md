# Bilibili 视频三连状态获取 - Minecraft 插件版本

## 概述
本文档提供了在 Minecraft 插件中使用 Kotlin 获取 Bilibili 视频三连状态（点赞、投币、收藏）的完整实现，支持每个玩家使用自己的 Bilibili 账户查询状态，包含风控机制规避。

## API 端点

### 1. 点赞状态查询
- **端点**: `https://api.bilibili.com/x/web-interface/archive/has/like`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA) + buvid3 (风控必需)
- **参数**: `aid` 或 `bvid`

### 2. 投币状态查询
- **端点**: `https://api.bilibili.com/x/web-interface/archive/coins`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA) + buvid3 (风控必需)
- **参数**: `aid` 或 `bvid`

### 3. 收藏状态查询
- **端点**: `https://api.bilibili.com/x/v2/fav/video/favoured`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA) + buvid3 (风控必需)
- **参数**: `aid`

## 状态说明

| 状态类型 | 返回值 | 说明 |
|---------|-------|------|
| 点赞状态 | 0/1 | 0=未点赞，1=已点赞 |
| 投币状态 | 0/1/2 | 投币数量 (0=未投币) |
| 收藏状态 | true/false | 是否已收藏 |

## 风控机制说明

**重要提示**: buvid3 是 Bilibili 的主要设备标识符，用于风控机制。所有需要登录的 API 请求都应该包含 buvid3 Cookie，否则可能触发风控导致请求失败。

## Kotlin 实现

### 数据类定义

```kotlin
import com.google.gson.annotations.SerializedName

// 通用API响应格式
data class BilibiliResponse<T>(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: T?
)

// 点赞状态响应
data class LikeStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: Int // 0=未点赞，1=已点赞
)

// 投币状态响应
data class CoinStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: CoinStatusData
)

data class CoinStatusData(
    val multiply: Int // 投币数量，0表示未投币
)

// 收藏状态响应
data class FavoriteStatusResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: FavoriteStatusData
)

data class FavoriteStatusData(
    val count: Int,
    val favoured: Boolean // true=已收藏，false=未收藏
)

// 视频三连状态数据类（支持玩家标识）
data class VideoTripleStatus(
    val videoId: String,
    val playerName: String, // 添加玩家名称标识
    val isLiked: Boolean,
    val coinCount: Int,
    val isFavorited: Boolean
) {
    fun hasTripleAction(): Boolean = isLiked && coinCount > 0 && isFavorited
    
    override fun toString(): String {
        return "VideoTripleStatus(玩家=$playerName, 视频=$videoId, 点赞=$isLiked, 投币=${coinCount}个, 收藏=$isFavorited)"
    }
}
```

### HTTP 客户端（使用 OkHttp3，包含风控机制）

```kotlin
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.Gson
import java.util.concurrent.TimeUnit
import org.bukkit.entity.Player

class BilibiliHttpClient {
    private val gson = Gson()
    private var sessionData: String = ""
    private var buvid3: String = ""
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()
    
    fun setCredentials(sessdata: String, buvid3: String = "") {
        this.sessionData = sessdata
        this.buvid3 = buvid3
    }
    
    fun buildRequest(url: String): Request.Builder {
        val builder = Request.Builder().url(url)
        
        // 添加必要的Cookie（包含风控所需的buvid3）
        if (sessionData.isNotEmpty()) {
            val cookieValue = buildString {
                append("SESSDATA=$sessionData")
                if (buvid3.isNotEmpty()) {
                    append("; buvid3=$buvid3")
                }
            }
            builder.addHeader("Cookie", cookieValue)
        }
        
        // 添加User-Agent避免风控
        builder.addHeader("User-Agent", 
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
        
        // 添加Referer头部
        builder.addHeader("Referer", "https://www.bilibili.com")
        
        return builder
    }
    
    inline fun <reified T> getJson(url: String): T? {
        return try {
            val request = buildRequest(url).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                gson.fromJson(responseBody, T::class.java)
            } else {
                println("请求失败: ${response.code} - ${response.message}")
                // 如果是403或412，可能触发了风控
                if (response.code in listOf(403, 412)) {
                    println("可能触发风控机制，请检查buvid3 Cookie是否正确设置")
                }
                null
            }
        } catch (e: Exception) {
            println("请求异常: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
```

### 玩家账户管理（复用之前的实现）

```kotlin
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * 管理每个 Minecraft 玩家的 Bilibili 账户信息
 */
class PlayerBilibiliAccountManager {
    
    private val playerAccounts = ConcurrentHashMap<UUID, PlayerBilibiliAccount>()
    
    data class PlayerBilibiliAccount(
        val playerUuid: UUID,
        val playerName: String,
        val sessdata: String,
        val buvid3: String = "",  // 风控必需
        val dedeUserId: String = "",
        val biliJct: String = "",
        val refreshToken: String = "",
        val loginTime: Long = System.currentTimeMillis(),
        var lastActiveTime: Long = System.currentTimeMillis()
    )
    
    fun getPlayerAccount(player: Player): PlayerBilibiliAccount? {
        val account = playerAccounts[player.uniqueId]
        account?.lastActiveTime = System.currentTimeMillis()
        return account
    }
    
    fun isPlayerLoggedIn(player: Player): Boolean {
        return playerAccounts.containsKey(player.uniqueId)
    }
    
    // 其他方法...
}
```

### 视频三连状态查询实现（支持每个玩家独立查询）

```kotlin
import kotlinx.coroutines.*
import java.util.regex.Pattern
import org.bukkit.entity.Player

class VideoTripleStatusChecker(
    private val httpClient: BilibiliHttpClient = BilibiliHttpClient(),
    private val accountManager: PlayerBilibiliAccountManager
) {
    
    companion object {
        private const val LIKE_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/has/like"
        private const val COIN_STATUS_URL = "https://api.bilibili.com/x/web-interface/archive/coins"
        private const val FAVORITE_STATUS_URL = "https://api.bilibili.com/x/v2/fav/video/favoured"
        
        // BV号转AV号的规律
        private val BV_PATTERN = Pattern.compile("^BV([A-Za-z0-9]+)$")
    }
    
    /**
     * 为玩家设置认证信息
     */
    private fun setupPlayerAuth(player: Player): Boolean {
        val account = accountManager.getPlayerAccount(player)
        if (account == null) {
            println("玩家 ${player.name} 未登录Bilibili账户")
            return false
        }
        
        // 检查必需的buvid3
        if (account.buvid3.isEmpty()) {
            println("玩家 ${player.name} 缺少buvid3信息，可能触发风控")
        }
        
        httpClient.setCredentials(account.sessdata, account.buvid3)
        return true
    }
    
    /**
     * 检查视频点赞状态
     * @param player Minecraft玩家
     * @param videoId 视频ID (支持av号、BV号)
     * @return 是否已点赞
     */
    suspend fun checkLikeStatus(player: Player, videoId: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (!setupPlayerAuth(player)) return@withContext false
            
            try {
                val url = buildUrl(LIKE_STATUS_URL, videoId)
                val response = httpClient.getJson<LikeStatusResponse>(url)
                
                if (response?.code == 0) {
                    response.data == 1
                } else {
                    when (response?.code) {
                        -412 -> println("玩家 ${player.name} 触发风控，请检查buvid3")
                        -101 -> println("玩家 ${player.name} 登录状态失效")
                        else -> println("获取点赞状态失败: ${response?.message}")
                    }
                    false
                }
            } catch (e: Exception) {
                println("检查点赞状态异常: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 检查视频投币状态
     * @param player Minecraft玩家
     * @param videoId 视频ID (支持av号、BV号)
     * @return 投币数量 (0表示未投币)
     */
    suspend fun checkCoinStatus(player: Player, videoId: String): Int {
        return withContext(Dispatchers.IO) {
            if (!setupPlayerAuth(player)) return@withContext 0
            
            try {
                val url = buildUrl(COIN_STATUS_URL, videoId)
                val response = httpClient.getJson<CoinStatusResponse>(url)
                
                if (response?.code == 0) {
                    response.data.multiply
                } else {
                    when (response?.code) {
                        -412 -> println("玩家 ${player.name} 触发风控，请检查buvid3")
                        -101 -> println("玩家 ${player.name} 登录状态失效")
                        else -> println("获取投币状态失败: ${response?.message}")
                    }
                    0
                }
            } catch (e: Exception) {
                println("检查投币状态异常: ${e.message}")
                0
            }
        }
    }
    
    /**
     * 检查视频收藏状态
     * @param player Minecraft玩家
     * @param videoId 视频ID (支持av号、BV号)
     * @return 是否已收藏
     */
    suspend fun checkFavoriteStatus(player: Player, videoId: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (!setupPlayerAuth(player)) return@withContext false
            
            try {
                val aid = extractAid(videoId)
                val url = "$FAVORITE_STATUS_URL?aid=$aid"
                val response = httpClient.getJson<FavoriteStatusResponse>(url)
                
                if (response?.code == 0) {
                    response.data.favoured
                } else {
                    when (response?.code) {
                        -412 -> println("玩家 ${player.name} 触发风控，请检查buvid3")
                        -101 -> println("玩家 ${player.name} 登录状态失效")
                        else -> println("获取收藏状态失败: ${response?.message}")
                    }
                    false
                }
            } catch (e: Exception) {
                println("检查收藏状态异常: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 获取视频完整的三连状态
     * @param player Minecraft玩家
     * @param videoId 视频ID (支持av号、BV号)
     * @return 三连状态对象
     */
    suspend fun getVideoTripleStatus(player: Player, videoId: String): VideoTripleStatus? {
        return withContext(Dispatchers.IO) {
            if (!setupPlayerAuth(player)) return@withContext null
            
            try {
                // 并发查询三种状态
                val likeDeferred = async { checkLikeStatus(player, videoId) }
                val coinDeferred = async { checkCoinStatus(player, videoId) }
                val favoriteDeferred = async { checkFavoriteStatus(player, videoId) }
                
                val isLiked = likeDeferred.await()
                val coinCount = coinDeferred.await()
                val isFavorited = favoriteDeferred.await()
                
                VideoTripleStatus(
                    videoId = videoId,
                    playerName = player.name,
                    isLiked = isLiked,
                    coinCount = coinCount,
                    isFavorited = isFavorited
                )
            } catch (e: Exception) {
                println("获取三连状态异常: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 批量获取多个视频的三连状态
     * @param player Minecraft玩家
     * @param videoIds 视频ID列表
     * @param concurrentLimit 并发限制 (默认3，避免触发风控)
     * @return 三连状态列表
     */
    suspend fun getMultipleVideoTripleStatus(
        player: Player,
        videoIds: List<String>, 
        concurrentLimit: Int = 3
    ): List<VideoTripleStatus> {
        return withContext(Dispatchers.IO) {
            if (!setupPlayerAuth(player)) return@withContext emptyList()
            
            // 分批处理，避免风控
            val results = mutableListOf<VideoTripleStatus>()
            videoIds.chunked(concurrentLimit).forEach { chunk ->
                val chunkResults = chunk.map { videoId ->
                    async { 
                        // 添加延迟避免请求过于频繁
                        delay(500)
                        getVideoTripleStatus(player, videoId)
                    }
                }.awaitAll().filterNotNull()
                
                results.addAll(chunkResults)
                
                // 批次间延迟
                if (chunk.size == concurrentLimit) {
                    delay(1000)
                }
            }
            
            results
        }
    }
    
    /**
     * 构建请求URL
     */
    private fun buildUrl(baseUrl: String, videoId: String): String {
        return if (videoId.startsWith("BV")) {
            "$baseUrl?bvid=$videoId"
        } else {
            val aid = extractAid(videoId)
            "$baseUrl?aid=$aid"
        }
    }
    
    /**
     * 从视频ID中提取AV号
     */
    private fun extractAid(videoId: String): String {
        return when {
            videoId.startsWith("av") -> videoId.substring(2)
            videoId.startsWith("BV") -> {
                // BV号需要转换为AV号，这里简化处理
                // 实际项目中可能需要实现BV->AV的转换算法
                bv2av(videoId) ?: videoId
            }
            videoId.all { it.isDigit() } -> videoId
            else -> videoId
        }
    }
    
    /**
     * BV号转AV号 (简化版本)
     * 实际使用中建议使用完整的转换算法
     */
    private fun bv2av(bvid: String): String? {
        // 这里应该实现完整的BV->AV转换算法
        // 为了示例简化，直接返回null让调用方使用BV号
        return null
    }
}
```

### 辅助工具类

```kotlin
/**
 * 三连状态统计工具
 */
class TripleStatusAnalyzer {
    
    fun analyzeTripleStatus(statusList: List<VideoTripleStatus>): TripleStatusSummary {
        val totalVideos = statusList.size
        val likedCount = statusList.count { it.isLiked }
        val coinedCount = statusList.count { it.coinCount > 0 }
        val favoritedCount = statusList.count { it.isFavorited }
        val tripleActionCount = statusList.count { it.hasTripleAction() }
        val totalCoins = statusList.sumOf { it.coinCount }
        
        return TripleStatusSummary(
            totalVideos = totalVideos,
            likedCount = likedCount,
            coinedCount = coinedCount,
            favoritedCount = favoritedCount,
            tripleActionCount = tripleActionCount,
            totalCoins = totalCoins,
            likeRate = if (totalVideos > 0) likedCount.toDouble() / totalVideos else 0.0,
            coinRate = if (totalVideos > 0) coinedCount.toDouble() / totalVideos else 0.0,
            favoriteRate = if (totalVideos > 0) favoritedCount.toDouble() / totalVideos else 0.0,
            tripleRate = if (totalVideos > 0) tripleActionCount.toDouble() / totalVideos else 0.0
        )
    }
}

data class TripleStatusSummary(
    val totalVideos: Int,
    val likedCount: Int,
    val coinedCount: Int,
    val favoritedCount: Int,
    val tripleActionCount: Int,
    val totalCoins: Int,
    val likeRate: Double,
    val coinRate: Double,
    val favoriteRate: Double,
    val tripleRate: Double
) {
    override fun toString(): String {
        return """
            |三连状态统计:
            |总视频数: $totalVideos
            |点赞数: $likedCount (${String.format("%.1f", likeRate * 100)}%)
            |投币数: $coinedCount (${String.format("%.1f", coinRate * 100)}%)
            |收藏数: $favoritedCount (${String.format("%.1f", favoriteRate * 100)}%)
            |三连数: $tripleActionCount (${String.format("%.1f", tripleRate * 100)}%)
            |总投币量: $totalCoins
        """.trimMargin()
    }
}
```

### 使用示例

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val tripleChecker = VideoTripleStatusChecker()
    
    // 设置认证信息
    tripleChecker.setCredentials(
        sessdata = "你的SESSDATA",
        buvid3 = "你的buvid3"
    )
    
    // 单个视频查询
    println("=== 单个视频三连状态查询 ===")
    val videoId = "BV1xx411c7mD"
    val status = tripleChecker.getVideoTripleStatus(videoId)
    println(status)
    
    // 单独查询各状态
    println("\n=== 单独状态查询 ===")
    val isLiked = tripleChecker.checkLikeStatus(videoId)
    val coinCount = tripleChecker.checkCoinStatus(videoId)
    val isFavorited = tripleChecker.checkFavoriteStatus(videoId)
    
    println("视频 $videoId:")
    println("  点赞状态: ${if (isLiked) "已点赞" else "未点赞"}")
    println("  投币状态: ${if (coinCount > 0) "已投${coinCount}币" else "未投币"}")
    println("  收藏状态: ${if (isFavorited) "已收藏" else "未收藏"}")
    
    // 批量查询
    println("\n=== 批量视频查询 ===")
    val videoIds = listOf(
        "BV1xx411c7mD",
        "BV1yy411c7mE", 
        "av123456",
        "BV1zz411c7mF"
    )
    
    val batchResults = tripleChecker.getMultipleVideoTripleStatus(videoIds)
    batchResults.forEach { result ->
        println(result)
    }
    
    // 统计分析
    println("\n=== 统计分析 ===")
    val analyzer = TripleStatusAnalyzer()
    val summary = analyzer.analyzeTripleStatus(batchResults)
    println(summary)
}
```

### 在 TabooLib 插件中的使用

```kotlin
import taboolib.common.platform.command.*
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendError
import kotlinx.coroutines.launch
import taboolib.common.platform.Plugin

object BilibiliVideoCommand : Plugin() {
    
    private val tripleChecker = VideoTripleStatusChecker()
    
    override fun onEnable() {
        // 初始化时设置认证信息
        val sessdata = config.getString("bilibili.sessdata", "")
        val buvid3 = config.getString("bilibili.buvid3", "")
        
        if (sessdata.isNotEmpty()) {
            tripleChecker.setCredentials(sessdata, buvid3)
        }
    }
    
    @CommandBody
    object VideoCommands {
        
        @CommandHeader(name = "bvideo", permission = "bilibili.video")
        fun main(sender: ProxyCommandSender) {
            sender.sendInfo("videoCommandHelp")
        }
        
        @SubCommand("status")
        fun checkStatus(
            sender: ProxyCommandSender,
            @CommandArgument("videoId") videoId: String
        ) {
            launch {
                try {
                    sender.sendInfo("checkingVideoStatus", "video" to videoId)
                    
                    val status = tripleChecker.getVideoTripleStatus(videoId)
                    
                    sender.sendInfo("videoStatusResult",
                        "video" to videoId,
                        "liked" to if (status.isLiked) "已点赞" else "未点赞",
                        "coins" to "${status.coinCount}币",
                        "favorited" to if (status.isFavorited) "已收藏" else "未收藏"
                    )
                    
                    if (status.hasTripleAction()) {
                        sender.sendInfo("videoTripleCompleted")
                    }
                    
                } catch (e: Exception) {
                    sender.sendError("videoStatusError", "error" to e.message.orEmpty())
                }
            }
        }
        
        @SubCommand("batch")
        fun batchCheck(
            sender: ProxyCommandSender,
            @CommandArgument("videoIds") videoIds: String
        ) {
            launch {
                try {
                    val ids = videoIds.split(",").map { it.trim() }
                    sender.sendInfo("batchCheckingStatus", "count" to ids.size)
                    
                    val results = tripleChecker.getMultipleVideoTripleStatus(ids)
                    val analyzer = TripleStatusAnalyzer()
                    val summary = analyzer.analyzeTripleStatus(results)
                    
                    sender.sendInfo("batchStatusResults",
                        "total" to summary.totalVideos,
                        "liked" to summary.likedCount,
                        "coined" to summary.coinedCount,
                        "favorited" to summary.favoritedCount,
                        "tripled" to summary.tripleActionCount
                    )
                    
                } catch (e: Exception) {
                    sender.sendError("batchStatusError", "error" to e.message.orEmpty())
                }
            }
        }
    }
}
```

## 注意事项

1. **认证要求**: 所有API都需要有效的SESSDATA Cookie
2. **风控机制**: 建议添加buvid3 Cookie避免触发风控
3. **请求频率**: 避免过于频繁的请求，建议添加请求间隔
4. **错误处理**: 需要处理各种API错误和网络异常
5. **BV号转换**: 收藏状态API需要AV号，可能需要实现BV->AV转换
6. **批量限制**: 批量查询时建议限制并发数量，避免触发限制

## 依赖项

```kotlin
// build.gradle.kts (除了之前的依赖外，还需要)
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
```