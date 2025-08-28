# Bilibili 二维码登录 - Minecraft 插件版本

## 概述
本文档提供了在 Minecraft 插件中使用 Kotlin 实现 Bilibili 二维码登录的完整指南，支持每个玩家登录自己的 Bilibili 账户，并将登录信息与 Minecraft 玩家关联。

## API 端点

### Web 客户端二维码登录

#### 1. 生成二维码
- **端点**: `https://passport.bilibili.com/x/passport-login/web/qrcode/generate`
- **方法**: `GET`
- **参数**: 无
- **响应格式**: JSON

#### 2. 检查二维码状态（轮询）
- **端点**: `https://passport.bilibili.com/x/passport-login/web/qrcode/poll`
- **方法**: `GET`
- **参数**: `qrcode_key` - 从生成二维码步骤获得的密钥

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 0 | 登录成功 |
| 86038 | 二维码已失效 |
| 86090 | 二维码已扫码但未确认 |
| 86101 | 未扫码 |

## Kotlin 实现

### 数据类定义

```kotlin
import com.google.gson.annotations.SerializedName

// 二维码生成响应
data class QrCodeGenerateResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodeData
)

data class QrCodeData(
    val url: String,
    @SerializedName("qrcode_key")
    val qrcodeKey: String
)

// 二维码状态响应
data class QrCodePollResponse(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: QrCodePollData
)

data class QrCodePollData(
    val url: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    val timestamp: Long,
    val code: Int,
    val message: String
)

// 登录状态枚举
enum class LoginStatus {
    SUCCESS,           // 0 - 登录成功
    NOT_SCANNED,      // 86101 - 未扫码
    SCANNED_WAITING,  // 86090 - 已扫码但未确认
    EXPIRED           // 86038 - 二维码已失效
}
```

### HTTP 客户端工具（使用 OkHttp3）

```kotlin
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import com.google.gson.Gson
import java.io.IOException
import java.util.concurrent.TimeUnit

class BilibiliHttpClient {
    
    private val gson = Gson()
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .cookieJar(object : CookieJar {
            private val cookieStore = mutableMapOf<String, List<Cookie>>()
            
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                cookieStore[url.host] = cookies
            }
            
            override fun loadForRequest(url: HttpUrl): List<Cookie> {
                return cookieStore[url.host] ?: emptyList()
            }
        })
        .build()
    
    fun get(url: String): Response {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .build()
        return client.newCall(request).execute()
    }
    
    inline fun <reified T> getJson(url: String): T? {
        return try {
            val response = get(url)
            if (response.isSuccessful) {
                gson.fromJson(response.body?.string(), T::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getCookies(): List<Cookie> {
        return client.cookieJar.loadForRequest("https://bilibili.com".toHttpUrl())
    }
}
```

### Minecraft 玩家账户管理

```kotlin
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * 管理每个 Minecraft 玩家的 Bilibili 账户信息
 */
class PlayerBilibiliAccountManager {
    
    // 存储每个玩家的Bilibili认证信息
    private val playerAccounts = ConcurrentHashMap<UUID, PlayerBilibiliAccount>()
    
    data class PlayerBilibiliAccount(
        val playerUuid: UUID,
        val playerName: String,
        val sessdata: String,
        val buvid3: String = "",
        val dedeUserId: String = "",
        val biliJct: String = "",
        val refreshToken: String = "",
        val loginTime: Long = System.currentTimeMillis(),
        var lastActiveTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 从登录Cookie中提取关键信息并保存
     */
    fun savePlayerLoginInfo(player: Player, cookies: List<Cookie>, refreshToken: String) {
        val cookieMap = cookies.associateBy { it.name }
        
        val account = PlayerBilibiliAccount(
            playerUuid = player.uniqueId,
            playerName = player.name,
            sessdata = cookieMap["SESSDATA"]?.value ?: "",
            buvid3 = cookieMap["buvid3"]?.value ?: "",
            dedeUserId = cookieMap["DedeUserID"]?.value ?: "",
            biliJct = cookieMap["bili_jct"]?.value ?: "",
            refreshToken = refreshToken
        )
        
        playerAccounts[player.uniqueId] = account
    }
    
    /**
     * 获取玩家的Bilibili账户信息
     */
    fun getPlayerAccount(player: Player): PlayerBilibiliAccount? {
        val account = playerAccounts[player.uniqueId]
        // 更新最后活跃时间
        account?.lastActiveTime = System.currentTimeMillis()
        return account
    }
    
    /**
     * 检查玩家是否已登录Bilibili
     */
    fun isPlayerLoggedIn(player: Player): Boolean {
        return playerAccounts.containsKey(player.uniqueId)
    }
    
    /**
     * 移除玩家的登录信息
     */
    fun removePlayerAccount(player: Player) {
        playerAccounts.remove(player.uniqueId)
    }
    
    /**
     * 清理过期的登录信息（超过7天未使用）
     */
    fun cleanupExpiredAccounts() {
        val now = System.currentTimeMillis()
        val expireTime = 7 * 24 * 60 * 60 * 1000L // 7天
        
        playerAccounts.entries.removeAll { (_, account) ->
            now - account.lastActiveTime > expireTime
        }
    }
    
    /**
     * 获取当前登录的玩家数量
     */
    fun getLoggedInPlayerCount(): Int = playerAccounts.size
}
```

### 二维码登录实现

```kotlin
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class BilibiliQrCodeLogin(
    private val httpClient: BilibiliHttpClient = BilibiliHttpClient()
) {
    
    companion object {
        private const val QR_GENERATE_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
        private const val QR_POLL_URL = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
        private const val POLL_INTERVAL = 3000L // 3秒轮询间隔
        private const val MAX_POLL_TIME = 180000L // 3分钟超时
    }
    
    /**
     * 生成二维码
     * @return 包含二维码URL和密钥的数据，失败返回null
     */
    suspend fun generateQrCode(): QrCodeData? {
        return withContext(Dispatchers.IO) {
            try {
                val response = httpClient.getJson<QrCodeGenerateResponse>(QR_GENERATE_URL)
                if (response?.code == 0) {
                    response.data
                } else {
                    println("生成二维码失败: ${response?.message}")
                    null
                }
            } catch (e: Exception) {
                println("生成二维码异常: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 检查二维码状态
     * @param qrcodeKey 二维码密钥
     * @return 登录状态和相关数据
     */
    suspend fun checkQrCodeStatus(qrcodeKey: String): Pair<LoginStatus, QrCodePollData?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$QR_POLL_URL?qrcode_key=$qrcodeKey"
                val response = httpClient.getJson<QrCodePollResponse>(url)
                
                if (response?.code == 0) {
                    val status = when (response.data.code) {
                        0 -> LoginStatus.SUCCESS
                        86101 -> LoginStatus.NOT_SCANNED
                        86090 -> LoginStatus.SCANNED_WAITING
                        86038 -> LoginStatus.EXPIRED
                        else -> LoginStatus.EXPIRED
                    }
                    Pair(status, response.data)
                } else {
                    println("检查二维码状态失败: ${response?.message}")
                    Pair(LoginStatus.EXPIRED, null)
                }
            } catch (e: Exception) {
                println("检查二维码状态异常: ${e.message}")
                Pair(LoginStatus.EXPIRED, null)
            }
        }
    }
    
    /**
     * 执行完整的二维码登录流程
     * @param onQrCodeGenerated 二维码生成成功回调 (qrcodeUrl)
     * @param onStatusChanged 状态改变回调 (status, message)
     * @param onLoginSuccess 登录成功回调 (cookies, refreshToken)
     * @return 登录是否成功
     */
    suspend fun performQrCodeLogin(
        onQrCodeGenerated: (String) -> Unit,
        onStatusChanged: (LoginStatus, String) -> Unit,
        onLoginSuccess: (List<Cookie>, String) -> Unit
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 生成二维码
                val qrCodeData = generateQrCode()
                if (qrCodeData == null) {
                    onStatusChanged(LoginStatus.EXPIRED, "生成二维码失败")
                    return@withContext false
                }
                
                onQrCodeGenerated(qrCodeData.url)
                onStatusChanged(LoginStatus.NOT_SCANNED, "请使用 Bilibili APP 扫描二维码")
                
                // 2. 轮询二维码状态
                val startTime = System.currentTimeMillis()
                val isRunning = AtomicBoolean(true)
                
                while (isRunning.get() && System.currentTimeMillis() - startTime < MAX_POLL_TIME) {
                    val (status, data) = checkQrCodeStatus(qrCodeData.qrcodeKey)
                    
                    when (status) {
                        LoginStatus.SUCCESS -> {
                            onStatusChanged(status, "登录成功！")
                            val cookies = httpClient.getCookies()
                            onLoginSuccess(cookies, data?.refreshToken ?: "")
                            return@withContext true
                        }
                        
                        LoginStatus.SCANNED_WAITING -> {
                            onStatusChanged(status, "已扫码，请在手机上确认登录")
                        }
                        
                        LoginStatus.NOT_SCANNED -> {
                            onStatusChanged(status, "等待扫码...")
                        }
                        
                        LoginStatus.EXPIRED -> {
                            onStatusChanged(status, "二维码已失效")
                            return@withContext false
                        }
                    }
                    
                    delay(POLL_INTERVAL)
                }
                
                // 超时
                onStatusChanged(LoginStatus.EXPIRED, "登录超时")
                false
                
            } catch (e: Exception) {
                println("二维码登录过程异常: ${e.message}")
                onStatusChanged(LoginStatus.EXPIRED, "登录过程发生错误: ${e.message}")
                false
            }
        }
    }
    
    /**
     * 获取登录后的关键Cookie信息
     * @return Cookie键值对映射
     */
    fun getLoginCookies(): Map<String, String> {
        val cookies = httpClient.getCookies()
        val loginCookies = mutableMapOf<String, String>()
        
        cookies.forEach { cookie ->
            when (cookie.name) {
                "SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5" -> {
                    loginCookies[cookie.name] = cookie.value
                }
            }
        }
        
        return loginCookies
    }
}
```

### 使用示例

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val qrLogin = BilibiliQrCodeLogin()
    
    val success = qrLogin.performQrCodeLogin(
        onQrCodeGenerated = { qrCodeUrl ->
            println("请扫描二维码: $qrCodeUrl")
            // 这里可以生成二维码图片并显示
            generateQrCodeImage(qrCodeUrl)
        },
        
        onStatusChanged = { status, message ->
            println("[${status}] $message")
        },
        
        onLoginSuccess = { cookies, refreshToken ->
            println("登录成功！")
            cookies.forEach { cookie ->
                println("Cookie: ${cookie.name} = ${cookie.value}")
            }
            
            // 保存 refresh_token 用于后续刷新Cookie
            saveRefreshToken(refreshToken)
            
            // 获取关键Cookie
            val loginCookies = qrLogin.getLoginCookies()
            println("关键Cookie: $loginCookies")
        }
    )
    
    if (success) {
        println("二维码登录流程完成")
    } else {
        println("二维码登录失败")
    }
}

// 工具函数示例
fun generateQrCodeImage(url: String) {
    // 使用 ZXing 库生成二维码图片
    // 这里只是示例，需要添加相应的依赖和实现
    println("生成二维码: $url")
}

fun saveRefreshToken(refreshToken: String) {
    // 保存到文件或数据库
    println("保存 refresh_token: $refreshToken")
}
```

## 注意事项

1. **轮询频率**: 建议每3秒轮询一次，避免过于频繁请求
2. **超时处理**: 二维码通常有效期为3分钟，需要设置合理的超时时间
3. **Cookie管理**: 成功登录后需要保存关键Cookie用于后续API调用
4. **刷新Token**: 保存`refresh_token`用于Cookie刷新
5. **错误处理**: 需要处理网络异常、API错误等各种异常情况
6. **二维码显示**: 需要集成二维码生成库（如ZXing）来显示二维码

## 依赖项

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // 可选：二维码生成
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
```