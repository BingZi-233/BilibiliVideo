# Bilibili UP主关注状态获取

## 概述
本文档提供了在 Minecraft 插件中使用 Kotlin 获取 Bilibili UP主关注状态的完整实现，支持每个玩家使用自己的账户查询关注状态。

## API 端点

### 1. 用户信息查询（包含关注状态）
- **端点**: `https://api.bilibili.com/x/space/wbi/acc/info`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA) + WBI签名
- **参数**: `mid`、`w_rid`、`wts`

### 2. 用户卡片信息查询
- **端点**: `https://api.bilibili.com/x/web-interface/card`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA)
- **参数**: `mid`

### 3. 详细关系状态查询
- **端点**: `https://api.bilibili.com/x/space/acc/relation`
- **方法**: `GET`
- **认证**: Cookie (SESSDATA) + WBI签名
- **参数**: `mid`、`w_rid`、`wts`

## 关系状态说明

| 属性值 | 关系状态 |
|--------|---------|
| 0 | 未关注 |
| 2 | 已关注 |
| 6 | 互相关注 |
| 128 | 已拉黑 |

## Kotlin 实现

### 数据类定义

```kotlin
import com.google.gson.annotations.SerializedName

// UP主信息响应
data class UserInfoResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: UserInfoData?
)

data class UserInfoData(
    val mid: Long,
    val name: String,
    val sex: String,
    val face: String,
    val sign: String,
    val level: Int,
    val birthday: String,
    @SerializedName("is_followed")
    val isFollowed: Boolean, // 是否已关注
    val top_photo: String,
    val fans: Long,
    val friend: Long,
    val attention: Long
)

// 用户卡片响应  
data class UserCardResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: UserCardData?
)

data class UserCardData(
    val mid: Long,
    val name: String,
    val approve: Boolean,
    val sex: String,
    val rank: String,
    val face: String,
    val DisplayRank: String,
    val regtime: Long,
    val spacesta: Int,
    val birthday: String,
    val place: String,
    val description: String,
    val article: Int,
    val attentions: List<Long>?,
    val fans: Long,
    val friend: Long,
    val attention: Long,
    val sign: String,
    val level_info: UserLevelInfo,
    val pendant: UserPendant,
    val nameplate: UserNameplate,
    val official: UserOfficial,
    val official_verify: UserOfficialVerify,
    val vip: UserVip,
    val following: Boolean, // 是否正在关注
    val archive: UserArchive,
    val live: UserLive?
)

// 关系状态响应
data class RelationResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: RelationData?
)

data class RelationData(
    val relation: UserRelation,
    @SerializedName("be_relation") 
    val beRelation: UserRelation // 当前用户对目标用户的关系
)

data class UserRelation(
    val mid: Long,
    val attribute: Int, // 关系属性：0=未关注，2=已关注，6=互关，128=拉黑
    val mtime: Long,
    val tag: List<Int>?,
    val special: Int
)

// 关注状态枚举
enum class FollowStatus(val value: Int, val description: String) {
    NOT_FOLLOWING(0, "未关注"),
    FOLLOWING(2, "已关注"),
    MUTUAL_FOLLOWING(6, "互相关注"),
    BLOCKED(128, "已拉黑");
    
    companion object {
        fun fromValue(value: Int): FollowStatus {
            return values().find { it.value == value } ?: NOT_FOLLOWING
        }
    }
}

// UP主关注信息
data class UpFollowInfo(
    val mid: Long,
    val name: String,
    val face: String,
    val followStatus: FollowStatus,
    val fans: Long,
    val isFollowed: Boolean = followStatus != FollowStatus.NOT_FOLLOWING,
    val isMutualFollow: Boolean = followStatus == FollowStatus.MUTUAL_FOLLOWING,
    val isBlocked: Boolean = followStatus == FollowStatus.BLOCKED
) {
    override fun toString(): String {
        val statusText = when (followStatus) {
            FollowStatus.MUTUAL_FOLLOWING -> "互关"
            FollowStatus.FOLLOWING -> "已关注"
            FollowStatus.NOT_FOLLOWING -> "未关注"
            FollowStatus.BLOCKED -> "已拉黑"
        }
        return "UP主: $name (UID:$mid) - $statusText - 粉丝:${formatNumber(fans)}"
    }
    
    private fun formatNumber(num: Long): String {
        return when {
            num >= 10000 -> "${num / 10000}万"
            num >= 1000 -> "${String.format("%.1f", num / 1000.0)}k"
            else -> num.toString()
        }
    }
}
```

### WBI签名工具

```kotlin
import java.security.MessageDigest
import kotlin.text.Charsets

class WbiSigner {
    companion object {
        // WBI签名用的混淆表
        private val MIXIN_KEY_ENC_TAB = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
        )
        
        private fun md5(input: String): String {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
        
        fun getMixinKey(orig: String): String {
            return MIXIN_KEY_ENC_TAB.take(32)
                .map { orig[it] }
                .joinToString("")
        }
        
        fun encWbi(params: Map<String, Any>, imgKey: String, subKey: String): Map<String, String> {
            val mixinKey = getMixinKey(imgKey + subKey)
            val currTime = System.currentTimeMillis() / 1000
            
            val signParams = params.toMutableMap<String, Any>().apply {
                put("wts", currTime)
            }
            
            val query = signParams.toList()
                .sortedBy { it.first }
                .joinToString("&") { "${it.first}=${it.second}" }
            
            val wbiSign = md5(query + mixinKey)
            
            return signParams.mapValues { it.value.toString() }.toMutableMap().apply {
                put("w_rid", wbiSign)
            }
        }
    }
}
```

### 玩家账户管理

```kotlin
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

class PlayerBilibiliAccountManager {
    
    // 存储每个玩家的Bilibili认证信息
    private val playerAccounts = ConcurrentHashMap<String, PlayerBilibiliAccount>()
    
    data class PlayerBilibiliAccount(
        val playerUuid: String,
        val playerName: String,
        val sessdata: String,
        val buvid3: String = "",
        val imgKey: String = "",
        val subKey: String = "",
        val loginTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 设置玩家的Bilibili账户信息
     */
    fun setPlayerAccount(
        player: Player,
        sessdata: String,
        buvid3: String = "",
        imgKey: String = "",
        subKey: String = ""
    ) {
        val account = PlayerBilibiliAccount(
            playerUuid = player.uniqueId.toString(),
            playerName = player.name,
            sessdata = sessdata,
            buvid3 = buvid3,
            imgKey = imgKey,
            subKey = subKey
        )
        
        playerAccounts[player.uniqueId.toString()] = account
    }
    
    /**
     * 获取玩家的Bilibili账户信息
     */
    fun getPlayerAccount(player: Player): PlayerBilibiliAccount? {
        return playerAccounts[player.uniqueId.toString()]
    }
    
    /**
     * 移除玩家的账户信息
     */
    fun removePlayerAccount(player: Player) {
        playerAccounts.remove(player.uniqueId.toString())
    }
    
    /**
     * 检查玩家是否已登录Bilibili
     */
    fun isPlayerLoggedIn(player: Player): Boolean {
        return playerAccounts.containsKey(player.uniqueId.toString())
    }
    
    /**
     * 获取所有已登录的玩家数量
     */
    fun getLoggedInCount(): Int {
        return playerAccounts.size
    }
}
```

### UP主关注状态查询实现

```kotlin
import kotlinx.coroutines.*
import org.bukkit.entity.Player

class UpFollowStatusChecker(
    private val httpClient: BilibiliHttpClient = BilibiliHttpClient(),
    private val accountManager: PlayerBilibiliAccountManager
) {
    
    companion object {
        private const val USER_INFO_URL = "https://api.bilibili.com/x/space/wbi/acc/info"
        private const val USER_CARD_URL = "https://api.bilibili.com/x/web-interface/card"
        private const val RELATION_URL = "https://api.bilibili.com/x/space/acc/relation"
    }
    
    /**
     * 使用用户卡片接口检查关注状态（简单版本）
     */
    suspend fun checkFollowStatusSimple(player: Player, upMid: Long): UpFollowInfo? {
        return withContext(Dispatchers.IO) {
            val account = accountManager.getPlayerAccount(player)
            if (account == null) {
                println("玩家 ${player.name} 未登录Bilibili账户")
                return@withContext null
            }
            
            try {
                // 配置认证信息
                httpClient.setCredentials(account.sessdata, account.buvid3)
                
                val url = "$USER_CARD_URL?mid=$upMid"
                val response = httpClient.getJson<UserCardResponse>(url)
                
                if (response?.code == 0 && response.data != null) {
                    val data = response.data
                    val followStatus = if (data.following) FollowStatus.FOLLOWING else FollowStatus.NOT_FOLLOWING
                    
                    UpFollowInfo(
                        mid = data.mid,
                        name = data.name,
                        face = data.face,
                        followStatus = followStatus,
                        fans = data.fans
                    )
                } else {
                    println("获取UP主信息失败: ${response?.message}")
                    null
                }
            } catch (e: Exception) {
                println("检查关注状态异常: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 使用关系接口检查详细关注状态（包含互关、拉黑等）
     */
    suspend fun checkFollowStatusDetailed(player: Player, upMid: Long): UpFollowInfo? {
        return withContext(Dispatchers.IO) {
            val account = accountManager.getPlayerAccount(player)
            if (account == null) {
                println("玩家 ${player.name} 未登录Bilibili账户")
                return@withContext null
            }
            
            try {
                // 配置认证信息
                httpClient.setCredentials(account.sessdata, account.buvid3)
                
                // 构建WBI签名参数
                val params = mapOf("mid" to upMid)
                val signedParams = if (account.imgKey.isNotEmpty() && account.subKey.isNotEmpty()) {
                    WbiSigner.encWbi(params, account.imgKey, account.subKey)
                } else {
                    // 如果没有WBI密钥，使用简单版本
                    return@withContext checkFollowStatusSimple(player, upMid)
                }
                
                val queryString = signedParams.entries.joinToString("&") { "${it.key}=${it.value}" }
                val url = "$RELATION_URL?$queryString"
                
                val response = httpClient.getJson<RelationResponse>(url)
                
                if (response?.code == 0 && response.data != null) {
                    val followStatus = FollowStatus.fromValue(response.data.beRelation.attribute)
                    
                    // 获取基本用户信息
                    val userInfo = getUserBasicInfo(upMid)
                    
                    UpFollowInfo(
                        mid = upMid,
                        name = userInfo?.name ?: "Unknown",
                        face = userInfo?.face ?: "",
                        followStatus = followStatus,
                        fans = userInfo?.fans ?: 0L
                    )
                } else {
                    println("获取关注关系失败: ${response?.message}")
                    null
                }
            } catch (e: Exception) {
                println("检查详细关注状态异常: ${e.message}")
                // 降级到简单版本
                checkFollowStatusSimple(player, upMid)
            }
        }
    }
    
    /**
     * 批量检查多个UP主的关注状态
     */
    suspend fun checkMultipleFollowStatus(
        player: Player,
        upMids: List<Long>,
        useDetailed: Boolean = false
    ): List<UpFollowInfo> {
        return withContext(Dispatchers.IO) {
            if (!accountManager.isPlayerLoggedIn(player)) {
                println("玩家 ${player.name} 未登录Bilibili账户")
                return@withContext emptyList()
            }
            
            upMids.chunked(5).flatMap { chunk ->
                chunk.map { upMid ->
                    async {
                        if (useDetailed) {
                            checkFollowStatusDetailed(player, upMid)
                        } else {
                            checkFollowStatusSimple(player, upMid)
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }
    
    /**
     * 获取用户基本信息（不需要WBI签名）
     */
    private suspend fun getUserBasicInfo(mid: Long): UserCardData? {
        return try {
            val url = "$USER_CARD_URL?mid=$mid"
            val response = httpClient.getJson<UserCardResponse>(url)
            response?.data
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 搜索UP主并返回关注状态
     */
    suspend fun searchUpWithFollowStatus(
        player: Player,
        keyword: String,
        limit: Int = 10
    ): List<UpFollowInfo> {
        // 这里可以集成Bilibili搜索API
        // 为了简化，先返回空列表
        return emptyList()
    }
}
```

### TabooLib 命令实现

```kotlin
import taboolib.common.platform.command.*
import taboolib.module.lang.*
import kotlinx.coroutines.launch
import org.bukkit.entity.Player

@CommandBody
object BilibiliUpCommand {
    
    private val accountManager = PlayerBilibiliAccountManager()
    private val followChecker = UpFollowStatusChecker(accountManager = accountManager)
    
    @CommandHeader(name = "bup", permission = "bilibili.up")
    fun main(sender: ProxyCommandSender) {
        sender.sendInfo("upCommandHelp")
    }
    
    @SubCommand("login")
    fun login(
        sender: ProxyCommandSender,
        @CommandArgument("sessdata") sessdata: String,
        @CommandArgument("buvid3", optional = true) buvid3: String = ""
    ) {
        if (sender !is Player) {
            sender.sendError("playerOnly")
            return
        }
        
        launch {
            try {
                accountManager.setPlayerAccount(sender, sessdata, buvid3)
                sender.sendInfo("loginSuccess")
            } catch (e: Exception) {
                sender.sendError("loginFailed", "error" to e.message.orEmpty())
            }
        }
    }
    
    @SubCommand("logout") 
    fun logout(sender: ProxyCommandSender) {
        if (sender !is Player) {
            sender.sendError("playerOnly")
            return
        }
        
        accountManager.removePlayerAccount(sender)
        sender.sendInfo("logoutSuccess")
    }
    
    @SubCommand("follow")
    fun checkFollow(
        sender: ProxyCommandSender,
        @CommandArgument("upId") upId: Long
    ) {
        if (sender !is Player) {
            sender.sendError("playerOnly")
            return
        }
        
        if (!accountManager.isPlayerLoggedIn(sender)) {
            sender.sendError("notLoggedIn")
            return
        }
        
        launch {
            try {
                sender.sendInfo("checkingFollowStatus", "upId" to upId)
                
                val followInfo = followChecker.checkFollowStatusSimple(sender, upId)
                if (followInfo != null) {
                    val statusText = when (followInfo.followStatus) {
                        FollowStatus.MUTUAL_FOLLOWING -> "互相关注"
                        FollowStatus.FOLLOWING -> "已关注"
                        FollowStatus.NOT_FOLLOWING -> "未关注"  
                        FollowStatus.BLOCKED -> "已拉黑"
                    }
                    
                    sender.sendInfo("followStatusResult",
                        "upName" to followInfo.name,
                        "upId" to followInfo.mid,
                        "status" to statusText,
                        "fans" to formatNumber(followInfo.fans)
                    )
                    
                    if (followInfo.isMutualFollow) {
                        sender.sendInfo("mutualFollowNotice")
                    }
                } else {
                    sender.sendError("upNotFound", "upId" to upId)
                }
            } catch (e: Exception) {
                sender.sendError("checkFollowError", "error" to e.message.orEmpty())
            }
        }
    }
    
    @SubCommand("batch")
    fun batchCheckFollow(
        sender: ProxyCommandSender,
        @CommandArgument("upIds") upIds: String
    ) {
        if (sender !is Player) {
            sender.sendError("playerOnly")
            return
        }
        
        if (!accountManager.isPlayerLoggedIn(sender)) {
            sender.sendError("notLoggedIn")
            return
        }
        
        launch {
            try {
                val ids = upIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                sender.sendInfo("batchCheckingFollow", "count" to ids.size)
                
                val results = followChecker.checkMultipleFollowStatus(sender, ids)
                
                sender.sendInfo("batchFollowResults", "total" to results.size)
                
                val followedCount = results.count { it.isFollowed }
                val mutualCount = results.count { it.isMutualFollow }
                
                sender.sendInfo("followSummary",
                    "followed" to followedCount,
                    "mutual" to mutualCount,
                    "total" to results.size
                )
                
                // 显示详细结果
                results.forEach { info ->
                    val statusIcon = when (info.followStatus) {
                        FollowStatus.MUTUAL_FOLLOWING -> "💙"
                        FollowStatus.FOLLOWING -> "❤️"
                        FollowStatus.NOT_FOLLOWING -> "⚪"
                        FollowStatus.BLOCKED -> "🚫"
                    }
                    
                    sender.sendMessage("$statusIcon ${info.name} - ${formatNumber(info.fans)}粉")
                }
                
            } catch (e: Exception) {
                sender.sendError("batchFollowError", "error" to e.message.orEmpty())
            }
        }
    }
    
    @SubCommand("status")
    fun loginStatus(sender: ProxyCommandSender) {
        if (sender !is Player) {
            sender.sendError("playerOnly")
            return
        }
        
        val account = accountManager.getPlayerAccount(sender)
        if (account != null) {
            sender.sendInfo("loginStatusInfo",
                "playerName" to account.playerName,
                "loginTime" to formatTime(account.loginTime)
            )
        } else {
            sender.sendInfo("notLoggedIn")
        }
    }
    
    private fun formatNumber(num: Long): String {
        return when {
            num >= 10000 -> "${num / 10000}万"
            num >= 1000 -> "${String.format("%.1f", num / 1000.0)}k"
            else -> num.toString()
        }
    }
    
    private fun formatTime(timestamp: Long): String {
        val minutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
        return when {
            minutes < 60 -> "${minutes}分钟前"
            minutes < 1440 -> "${minutes / 60}小时前"
            else -> "${minutes / 1440}天前"
        }
    }
}
```

### 语言文件示例

```yaml
# zh_CN.yml
upCommandHelp: "&e=== Bilibili UP主关注查询 ==="
loginSuccess: "&a成功登录Bilibili账户！"
loginFailed: "&c登录失败: {error}"
logoutSuccess: "&a已退出Bilibili账户"
notLoggedIn: "&c请先使用 /bup login <sessdata> 登录账户"
checkingFollowStatus: "&e正在查询UP主 {upId} 的关注状态..."
followStatusResult: "&f{upName} (UID:{upId}) - &b{status} &7- 粉丝: {fans}"
mutualFollowNotice: "&d💙 你们互相关注！"
upNotFound: "&cUP主 {upId} 不存在或查询失败"
checkFollowError: "&c查询关注状态失败: {error}"
batchCheckingFollow: "&e正在批量查询 {count} 个UP主的关注状态..."
batchFollowResults: "&a批量查询完成，共查询到 {total} 个UP主"
followSummary: "&f已关注: &a{followed} &f互关: &d{mutual} &f总计: &7{total}"
batchFollowError: "&c批量查询失败: {error}"
loginStatusInfo: "&a玩家: {playerName} &7登录时间: {loginTime}"
playerOnly: "&c此命令只能由玩家执行"
```

## 使用说明

1. **玩家登录**: `/bup login <SESSDATA> [buvid3]`
2. **查询单个UP主**: `/bup follow <UP主UID>`  
3. **批量查询**: `/bup batch <UID1,UID2,UID3>`
4. **查看登录状态**: `/bup status`
5. **退出登录**: `/bup logout`

## 注意事项

1. **隐私保护**: SESSDATA包含敏感信息，需要安全存储
2. **WBI签名**: 详细接口需要WBI签名，简单接口可能足够使用
3. **请求限制**: 避免过于频繁的请求
4. **账户隔离**: 每个玩家使用自己的Bilibili账户
5. **错误处理**: 需要处理账户失效、网络错误等情况
6. **数据缓存**: 可以考虑缓存UP主信息减少请求