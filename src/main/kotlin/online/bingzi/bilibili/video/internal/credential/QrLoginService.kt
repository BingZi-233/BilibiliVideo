package online.bingzi.bilibili.video.internal.credential

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import online.bingzi.bilibili.video.internal.bilibili.dto.NavResponse
import online.bingzi.bilibili.video.internal.bilibili.dto.QrGenerateResponse
import online.bingzi.bilibili.video.internal.bilibili.dto.QrPollResponse
import online.bingzi.bilibili.video.internal.repository.CredentialRepository
import online.bingzi.bilibili.video.internal.service.BindingService
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import java.io.IOException
import java.util.*
import java.util.concurrent.*

/**
 * 二维码登录服务。
 *
 * 负责：
 * - 调用 B 站二维码登录接口生成登录 URL
 * - 启动后台轮询任务检查扫码状态
 * - 登录成功时提取 Cookie / refresh_token / 用户 mid 与昵称
 * - 将凭证与账号绑定写入数据库
 *
 * 注意：
 * - 内部只存储 UUID 与少量必要信息，不持有 Player 引用，避免内存泄漏。
 */
object QrLoginService {

    private const val QR_GENERATE_URL =
        "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    private const val QR_POLL_URL =
        "https://passport.bilibili.com/x/passport-login/web/qrcode/poll"
    private const val NAV_URL =
        "https://api.bilibili.com/x/web-interface/nav"

    private const val QR_EXPIRE_MILLIS = 180_000L

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val gson: Gson = Gson()

    private val scheduler: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "BilibiliVideo-QrLogin").apply { isDaemon = true }
        }

    private data class QrSession(
        val playerUuid: UUID,
        val playerName: String,
        val qrcodeKey: String,
        val createdAt: Long,
        val expireAt: Long
    )

    private val sessions = ConcurrentHashMap<UUID, QrSession>()
    private val pollTasks = ConcurrentHashMap<UUID, ScheduledFuture<*>>()

    enum class QrStartCode {
        SUCCESS,
        NETWORK_ERROR,
        API_ERROR
    }

    data class QrStartResult(
        val code: QrStartCode,
        val message: String,
        val qrUrl: String? = null,
        val expireAt: Long? = null
    ) {
        val success: Boolean get() = code == QrStartCode.SUCCESS
    }

    /**
     * 开始为指定玩家创建二维码登录会话。
     *
     * 成功时返回二维码 URL，由调用方负责通过地图等形式展示给玩家。
     */
    fun startLogin(player: Player): QrStartResult {
        val now = System.currentTimeMillis()
        val generate = try {
            requestQrGenerate()
        } catch (e: IOException) {
            warning("[QrLoginService] 申请二维码失败：${e.message}", e)
            return QrStartResult(
                code = QrStartCode.NETWORK_ERROR,
                message = "无法连接到 B 站登录服务器，请稍后重试。"
            )
        } catch (e: Throwable) {
            warning("[QrLoginService] 申请二维码时发生异常：${e.message}", e)
            return QrStartResult(
                code = QrStartCode.API_ERROR,
                message = "申请二维码失败，请联系管理员。"
            )
        }

        if (generate.code != 0 || generate.data == null) {
            return QrStartResult(
                code = QrStartCode.API_ERROR,
                message = "B 站返回错误：${generate.message}"
            )
        }

        val session = QrSession(
            playerUuid = player.uniqueId,
            playerName = player.name,
            qrcodeKey = generate.data.qrcodeKey,
            createdAt = now,
            expireAt = now + QR_EXPIRE_MILLIS
        )

        // 覆盖旧会话及轮询任务
        cancelSession(player.uniqueId)
        sessions[player.uniqueId] = session

        val future = scheduler.scheduleAtFixedRate(
            { pollOnce(player.uniqueId) },
            2,
            2,
            TimeUnit.SECONDS
        )
        pollTasks[player.uniqueId] = future

        info("[QrLoginService] 已为玩家 ${player.name} 创建二维码登录会话。")
        return QrStartResult(
            code = QrStartCode.SUCCESS,
            message = "二维码已生成。",
            qrUrl = generate.data.url,
            expireAt = session.expireAt
        )
    }

    fun cancelLogin(player: Player) {
        cancelSession(player.uniqueId)
    }

    fun shutdown() {
        sessions.clear()
        pollTasks.values.forEach { it.cancel(true) }
        pollTasks.clear()
        scheduler.shutdownNow()
    }

    private fun cancelSession(playerUuid: UUID) {
        sessions.remove(playerUuid)
        pollTasks.remove(playerUuid)?.cancel(true)
    }

    private fun pollOnce(playerUuid: UUID) {
        val session = sessions[playerUuid]
        if (session == null) {
            cancelSession(playerUuid)
            return
        }

        val now = System.currentTimeMillis()
        if (now > session.expireAt) {
            info("[QrLoginService] 玩家 ${session.playerName} 的二维码会话已过期，已清理。")
            cancelSession(playerUuid)
            return
        }

        val poll = try {
            requestQrPoll(session.qrcodeKey)
        } catch (e: IOException) {
            warning("[QrLoginService] 轮询扫码状态网络错误：${e.message}", e)
            return
        } catch (e: Throwable) {
            warning("[QrLoginService] 轮询扫码状态异常：${e.message}", e)
            return
        }

        if (poll.code != 0 || poll.data == null) {
            // B 站接口本身异常，稍后重试
            return
        }

        when (poll.data.code) {
            86101 -> {
                // 尚未扫码
            }

            86090 -> {
                // 已扫码未确认
            }

            86038 -> {
                // 二维码失效
                info("[QrLoginService] 玩家 ${session.playerName} 的二维码已失效，已提示重新生成。")
                notifyPlayer(
                    session,
                    "§e[BV] 二维码已失效，请重新执行 /bv qrcode。"
                )
                cancelSession(playerUuid)
            }

            0 -> {
                // 登录成功
                handleLoginSuccess(session, poll)
                cancelSession(playerUuid)
            }
        }
    }

    @Throws(IOException::class)
    private fun requestQrGenerate(): QrGenerateResponse {
        val request = Request.Builder()
            .url(QR_GENERATE_URL)
            .header("User-Agent", defaultUserAgent())
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            return gson.fromJson(body, QrGenerateResponse::class.java)
        }
    }

    @Throws(IOException::class)
    private fun requestQrPoll(qrcodeKey: String): QrPollResponse {
        val url = "$QR_POLL_URL?qrcode_key=$qrcodeKey"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", defaultUserAgent())
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            val headers = resp.headers("Set-Cookie")
            val response = gson.fromJson(body, QrPollResponse::class.java)
            if (response.data?.code == 0) {
                val sessData = extractCookie("SESSDATA", headers)
                val biliJct = extractCookie("bili_jct", headers)
                val buvid3 = extractCookie("buvid3", headers)
                // 无论 refreshToken 是否为空，都写入上下文，避免因缺少 refresh_token 导致后续无法获取 Cookie。
                LoginContextHolder.set(
                    LoginContext(
                        sessData = sessData,
                        biliJct = biliJct,
                        buvid3 = buvid3,
                        refreshToken = response.data.refreshToken
                    )
                )
            }
            return response
        }
    }

    private fun handleLoginSuccess(session: QrSession, pollResponse: QrPollResponse) {
        val context = LoginContextHolder.get() ?: run {
            warning("[QrLoginService] 登录成功但未能获取 Cookie 信息。")
            return
        }

        val sessData = context.sessData
        val biliJct = context.biliJct

        if (sessData.isNullOrBlank() || biliJct.isNullOrBlank()) {
            warning("[QrLoginService] 登录成功但缺少必要 Cookie（SESSDATA 或 bili_jct）。")
            return
        }

        val cookieHeader = buildCookieHeader(sessData, biliJct, context.buvid3)

        val nav = try {
            requestNav(cookieHeader)
        } catch (e: IOException) {
            warning("[QrLoginService] 获取当前登录用户信息失败：${e.message}", e)
            return
        } catch (e: Throwable) {
            warning("[QrLoginService] 获取当前登录用户信息异常：${e.message}", e)
            return
        }

        info("[QrLoginService] 玩家 ${session.playerName} 已获取当前登录用户信息，接口返回 code=${nav.code}")

        if (nav.code != 0 || nav.data == null || nav.data.mid <= 0L) {
            warning("[QrLoginService] 获取当前登录用户信息返回异常：code=${nav.code}, message=${nav.message}")
            return
        }

        val mid = nav.data.mid
        val uname = nav.data.uname
        val refreshToken = context.refreshToken

        val bindResult = saveCredentialAndBinding(
            playerUuid = session.playerUuid,
            playerName = session.playerName,
            bilibiliMid = mid,
            bilibiliName = uname,
            sessData = sessData,
            biliJct = biliJct,
            buvid3 = context.buvid3,
            refreshToken = refreshToken
        )

        if (!bindResult.success) {
            warning("[QrLoginService] 玩家 ${session.playerName} 绑定失败：${bindResult.message}")
            notifyPlayer(session, "§c[BV] ${bindResult.message}")
            return
        }

        info("[QrLoginService] 玩家 ${session.playerName} 已完成二维码登录绑定流程。")
        notifyPlayer(
            session,
            "§a[BV] 已成功绑定 B 站账号 $mid ($uname)"
        )
    }

    @Throws(IOException::class)
    private fun requestNav(cookieHeader: String): NavResponse {
        val request = Request.Builder()
            .url(NAV_URL)
            .header("User-Agent", defaultUserAgent())
            .header("Cookie", cookieHeader)
            .get()
            .build()
        client.newCall(request).execute().use { resp ->
            val body = resp.body?.string() ?: ""
            return gson.fromJson(body, NavResponse::class.java)
        }
    }

    private fun saveCredentialAndBinding(
        playerUuid: UUID,
        playerName: String,
        bilibiliMid: Long,
        bilibiliName: String,
        sessData: String,
        biliJct: String,
        buvid3: String?,
        refreshToken: String?
    ): BindingService.BindResult {
        val playerUuidStr = playerUuid.toString()
        val now = System.currentTimeMillis()

        val bindResult = BindingService.bind(
            playerUuid = playerUuidStr,
            playerName = playerName,
            bilibiliMid = bilibiliMid,
            bilibiliName = bilibiliName
        )

        if (!bindResult.success) {
            return bindResult
        }

        // 凭证表：按 mid 更新或插入
        val existingCredential = CredentialRepository.findByBilibiliMid(bilibiliMid)
        if (existingCredential == null) {
            val label = "mid-$bilibiliMid"
            CredentialRepository.insert(
                label = label,
                sessData = sessData,
                biliJct = biliJct,
                bilibiliMid = bilibiliMid,
                buvid3 = buvid3,
                accessKey = null,
                refreshToken = refreshToken,
                status = 1,
                createdAt = now,
                updatedAt = now,
                expiredAt = null,
                lastUsedAt = now
            )
        } else {
            CredentialRepository.updateTokensByMid(
                bilibiliMid = bilibiliMid,
                sessData = sessData,
                biliJct = biliJct,
                buvid3 = buvid3,
                accessKey = null,
                refreshToken = refreshToken
            )
        }

        return bindResult
    }

    private fun extractCookie(name: String, cookies: List<String>): String? {
        for (cookie in cookies) {
            val parts = cookie.split(';')
            val kv = parts.firstOrNull()?.split('=', limit = 2) ?: continue
            if (kv.size == 2 && kv[0].trim() == name) {
                return kv[1]
            }
        }
        return null
    }

    private fun buildCookieHeader(sessData: String, biliJct: String, buvid3: String?): String {
        val list = mutableListOf<String>()
        list += "SESSDATA=$sessData"
        list += "bili_jct=$biliJct"
        if (!buvid3.isNullOrBlank()) {
            list += "buvid3=$buvid3"
        }
        return list.joinToString("; ")
    }

    private fun defaultUserAgent(): String {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Safari/537.36"
    }

    /**
     * 在主线程上给玩家发送提示消息（如果仍在线）。
     */
    private fun notifyPlayer(session: QrSession, message: String) {
        submit(async = false) {
            val player = Bukkit.getPlayer(session.playerUuid)
            if (player != null && player.isOnline) {
                player.sendMessage(message)
            }
        }
    }

    /**
     * 登录过程中的 Cookie/refresh_token 暂存上下文。
     */
    private data class LoginContext(
        val sessData: String?,
        val biliJct: String?,
        val buvid3: String?,
        val refreshToken: String?
    )

    private object LoginContextHolder {
        private val holder = ThreadLocal<LoginContext?>()

        fun set(context: LoginContext) {
            holder.set(context)
        }

        fun get(): LoginContext? {
            val ctx = holder.get()
            holder.remove()
            return ctx
        }
    }
}
