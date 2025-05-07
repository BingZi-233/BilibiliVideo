package online.bingzi.bilibili.video.internal.engine

import online.bingzi.bilibili.video.api.event.*
import online.bingzi.bilibili.video.internal.cache.*
import online.bingzi.bilibili.video.internal.config.SettingConfig.chainOperations
import online.bingzi.bilibili.video.internal.database.Database
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.engine.drive.BilibiliApiDrive
import online.bingzi.bilibili.video.internal.engine.drive.BilibiliPassportDrive
import online.bingzi.bilibili.video.internal.entity.*
import online.bingzi.bilibili.video.internal.handler.ApiType
import online.bingzi.bilibili.video.internal.helper.*
import org.bukkit.Bukkit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.common.platform.function.warning
import taboolib.library.kether.ArgTypes.listOf
import taboolib.module.chat.colored

/**
 * 网络访问引擎
 * NetworkEngine 是一个单例对象，负责与哔哩哔哩的 API 进行交互，处理各种网络请求。
 * 它提供了生成二维码、获取三连状态等功能，并且管理与网络相关的操作。
 */
object NetworkEngine {
    /**
     * 哔哩哔哩 API 驱动
     * 通过 Retrofit 创建的 BilibiliApiDrive 实例，用于与哔哩哔哩的 API 进行交互。
     */
    val bilibiliAPI: BilibiliApiDrive by lazy {
        Retrofit.Builder().baseUrl("https://api.bilibili.com/x/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(BilibiliApiDrive::class.java)
    }

    /**
     * 哔哩哔哩通行证 API 驱动
     * 通过 Retrofit 创建的 BilibiliPassportDrive 实例，用于处理与哔哩哔哩通行证相关的 API 请求。
     */
    val bilibiliPassportAPI: BilibiliPassportDrive by lazy {
        Retrofit.Builder().baseUrl("https://passport.bilibili.com/x/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(BilibiliPassportDrive::class.java)
    }

    /**
     * 哔哩哔哩网站 API 驱动
     * 通过 Retrofit 创建的 BilibiliPassportDrive 实例，用于处理与哔哩哔哩网站相关的 API 请求。
     */
    val bilibiliWebsiteAPI: BilibiliPassportDrive by lazy {
        Retrofit.Builder().baseUrl("https://www.bilibili.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(BilibiliPassportDrive::class.java)
    }

    /**
     * Show模式动作处理链
     * 用于处理在 Show 模式下的 API 操作，通过 ApiType.buildHandler 构建一系列操作链。
     */
    private val showAction = ApiType.buildHandler(*chainOperations.toTypedArray())

    /**
     * 错误信息常量
     * 当哔哩哔哩未提供任何错误信息时使用的默认错误信息。
     */
    private const val ERROR_MESSAGE_NOT_PROVIDED: String = "Bilibili未提供任何错误信息"

    @SubscribeEvent
    fun onQRCodeGenerateRequest(event: BilibiliQRCodeGenerateRequestEvent) {
        val player = event.player
        val target = event.target

        bilibiliPassportAPI.applyQRCodeGenerate().enqueue(object : Callback<BilibiliResult<QRCodeGenerateData>> {
            override fun onResponse(
                call: Call<BilibiliResult<QRCodeGenerateData>>, response: Response<BilibiliResult<QRCodeGenerateData>>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 0) {
                        BilibiliQRCodeGeneratedResultEvent(player, target, body.data, true).call()
                        startQRCodePolling(player, target, body.data.qrCodeKey)
                    } else {
                        BilibiliQRCodeGeneratedResultEvent(
                            player, target, null, false,
                            body?.message ?: ERROR_MESSAGE_NOT_PROVIDED
                        ).call()
                    }
                } else {
                    BilibiliQRCodeGeneratedResultEvent(
                        player, target, null, false,
                        "HTTP受限，错误码：${response.code()}"
                    ).call()
                }
            }

            override fun onFailure(call: Call<BilibiliResult<QRCodeGenerateData>>, t: Throwable) {
                BilibiliQRCodeGeneratedResultEvent(
                    player, target, null, false,
                    t.message ?: "未提供错误描述。"
                ).call()
            }
        })
    }

    private fun startQRCodePolling(player: ProxyPlayer, target: ProxyPlayer?, qrCodeKey: String) {
        submit(async = true, delay = 20L * 3, period = 20L * 3) {
            val execute = bilibiliPassportAPI.scanningQRCode(qrCodeKey).execute()
            if (execute.isSuccessful) {
                execute.body()?.let { result ->
                    val scanStatusCode = result.data.code
                    var cookieData: CookieData? = null
                    var eventMessage = result.message

                    if (scanStatusCode == 0) {
                        qrCodeKeyCache[qrCodeKey]?.let { rawCookies ->
                            try {
                                val cookieList = rawCookies.map { it.split(";")[0] }
                                val cookieJsonString = cookieList.joinToString(
                                    ",", prefix = "{", postfix = "}"
                                ) { "\"${it.replace("=", "\":\"").replace("""\u003d""", "\":\"")}\"" }
                                cookieData = gson.fromJson(cookieJsonString, CookieData::class.java)
                            } catch (e: Exception) {
                                warning("解析CookieData失败 for qrCodeKey $qrCodeKey: ${e.message}")
                            }
                        }
                        if (cookieData == null) {
                            eventMessage = eventMessage ?: "扫码成功但无法获取或解析Cookie"
                        }
                    }

                    BilibiliQRCodeScanUpdateEvent(
                        player,
                        target,
                        qrCodeKey,
                        scanStatusCode,
                        cookieData,
                        result.data.refreshToken,
                        result.data.timestamp,
                        eventMessage
                    ).call()

                    if (scanStatusCode == 0 || scanStatusCode == 86038) {
                        this.cancel()
                    }
                } ?: BilibiliQRCodeScanUpdateEvent(player, target, qrCodeKey, -1, message = "扫码响应体为空").call().also { this.cancel() }
            } else {
                warningMessageAsLang("NetworkRequestFailureCode", execute.code())
                BilibiliQRCodeScanUpdateEvent(player, target, qrCodeKey, -2, message = "扫码网络请求失败: ${execute.code()}").call()
                this.cancel()
            }
        }
    }

    @SubscribeEvent
    fun onTripleStatusRequest(event: TripleStatusRequestEvent) {
        val player = event.player
        val bvid = event.bvid

        val csrf = cookieCache[player.uniqueId]?.biliJct
        val sessData = cookieCache[player.uniqueId]?.sessData?.let { "SESSDATA=$it" }

        if (csrf == null || sessData == null) {
            TripleStatusResultEvent(player, bvid, null, false, -101, "CookieNotFound (CSRF or SESSDATA missing)").call()
            return
        }

        bilibiliAPI.actionLikeTriple(bvid, csrf, sessData).enqueue(object : Callback<BilibiliResult<TripleData>> {
            override fun onResponse(
                call: Call<BilibiliResult<TripleData>>, response: Response<BilibiliResult<TripleData>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        TripleStatusResultEvent(player, bvid, body.data, body.code == 0, body.code, body.message ?: ERROR_MESSAGE_NOT_PROVIDED).call()
                    } ?: TripleStatusResultEvent(player, bvid, null, false, response.code(), "三连请求响应体为空但HTTP成功").call()
                } else {
                    TripleStatusResultEvent(player, bvid, null, false, response.code(), "三连请求失败: ${response.message()} (HTTP ${response.code()})").call()
                    warning("请求失败 - Code: ${response.code()}, Message: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<BilibiliResult<TripleData>>, t: Throwable) {
                TripleStatusResultEvent(player, bvid, null, false, null, t.message ?: ERROR_MESSAGE_NOT_PROVIDED).call()
            }
        })
    }

    @SubscribeEvent
    fun onTripleStatusShowRequest(event: TripleStatusShowRequestEvent) {
        val player = event.player
        val bvid = event.bvid

        val sessData = cookieCache[player.uniqueId]?.sessData?.let { "SESSDATA=$it" }
        if (sessData == null) {
            TripleStatusShowResultEvent(player, bvid, false, "CookieNotFound (SESSDATA missing)").call()
            return
        }

        val success = showAction.handle(player, bvid, sessData)
        TripleStatusShowResultEvent(player, bvid, success, if (!success) "ShowAction处理失败" else null).call()
    }

    fun getUserInfo(cookie: CookieData): UserInfoData? {
        val sessData = cookie.sessData?.let { "SESSDATA=$it" } ?: return null
        return try {
            val response = bilibiliAPI.getUserInfo(sessData).execute()
            if (response.isSuccessful) {
                response.body()?.data
            } else {
                null
            }
        } catch (e: Exception) {
            warning("获取用户信息时发生异常: ${e.message}")
            null
        }
    }
}