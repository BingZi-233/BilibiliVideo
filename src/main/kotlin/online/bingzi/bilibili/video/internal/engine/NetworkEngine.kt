package online.bingzi.bilibili.video.internal.engine

import online.bingzi.bilibili.video.api.event.TripleSendRewardsEvent
import online.bingzi.bilibili.video.internal.cache.*
import online.bingzi.bilibili.video.internal.config.SettingConfig.chainOperations
import online.bingzi.bilibili.video.internal.database.Database
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer
import online.bingzi.bilibili.video.internal.engine.drive.BilibiliApiDrive
import online.bingzi.bilibili.video.internal.engine.drive.BilibiliPassportDrive
import online.bingzi.bilibili.video.internal.entity.*
import online.bingzi.bilibili.video.internal.handler.ApiType
import online.bingzi.bilibili.video.internal.helper.*
import online.bingzi.bilibili.video.internal.onebot.QRCodeService
import online.bingzi.bilibili.video.internal.onebot.QQBindManager
import org.bukkit.Bukkit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import taboolib.common.platform.ProxyPlayer
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

    /**
     * 生成哔哩哔哩二维码 URL
     * @param player 触发生成二维码的玩家
     * @param target 可选参数，指定二维码的目标玩家，默认为 null
     */
    fun generateBilibiliQRCodeUrl(player: ProxyPlayer, target: ProxyPlayer? = null) {
        // 调用通行证 API 生成二维码
        bilibiliPassportAPI.applyQRCodeGenerate().enqueue(object : Callback<BilibiliResult<QRCodeGenerateData>> {
            override fun onResponse(
                call: Call<BilibiliResult<QRCodeGenerateData>>, response: Response<BilibiliResult<QRCodeGenerateData>>
            ) {
                // 确定操作的玩家
                val p = target ?: player
                if (response.isSuccessful) {
                    // 处理成功的响应
                    val body = response.body()
                    if (body != null && body.code == 0) {
                        // 使用QRCodeService发送二维码
                        val success = QRCodeService.sendQRCode(
                            player,
                            body.data.url,
                            "Bilibili扫码登陆",
                            "请使用Bilibili客户端扫描二维码"
                        )
                        
                        if (!success) {
                            player.infoAsLang("GenerateUseCookieFailure", "发送二维码失败")
                            return
                        }
                        
                        // 每隔1s检查一次玩家是否扫码
                        // 出现以下情况后会自动取消任务：
                        // 1. 玩家扫码登陆成功
                        // 2. 二维码已超时
                        submit(async = true, delay = 20L * 9, period = 20L * 3) {
                            val qrCodeKey = body.data.qrCodeKey
                            val execute = bilibiliPassportAPI.scanningQRCode(qrCodeKey).execute()
                            if (execute.isSuccessful) {
                                execute.body()?.let { result ->
                                    when (result.data.code) {
                                        0 -> {
                                            qrCodeKeyCache[qrCodeKey]?.let { list ->
                                                // 提取Cookie中有效部分
                                                // 这里不知道为什么会传递一些容易产生干扰的信息进来
                                                val cookieList = list.map { it.split(";")[0] }
                                                // 将Cookie转化为JSON
                                                val replace = cookieList.joinToString(
                                                    ",", prefix = "{", postfix = "}"
                                                ) {
                                                    "\"${
                                                        it.replace("=", "\":\"").replace("""\u003d""", "\":\"")
                                                    }\""
                                                }
                                                // GSON反序列化成CookieData
                                                val cookieData = gson.fromJson(replace, CookieData::class.java)
                                                // 检查MID重复
                                                val userInfoData = checkRepeatabilityMid(p, cookieData)
                                                val cacheMid = midCache[p.uniqueId]
                                                when {
                                                    // 检查重复的MID
                                                    userInfoData == null -> {
                                                        player.infoAsLang("GenerateUseCookieRepeatabilityMid")
                                                    }
                                                    // 登录的MID和绑定的MID不一致
                                                    cacheMid.isNullOrBlank().not() && cacheMid != userInfoData.mid -> {
                                                        player.infoAsLang("PlayerIsBindMid")
                                                    }
                                                    // Cookie刷新
                                                    else -> {
                                                        cookieCache.put(p.uniqueId, cookieData)
                                                        midCache.put(p.uniqueId, userInfoData.mid)
                                                        unameCache.put(p.uniqueId, userInfoData.uname)
                                                        p.setDataContainer("mid", userInfoData.mid)
                                                        p.setDataContainer("uname", userInfoData.uname)
                                                        p.setDataContainer("refresh_token", result.data.refreshToken)
                                                        p.setDataContainer("timestamp", result.data.timestamp.toString())
                                                        p.infoAsLang("GenerateUseCookieSuccess")
                                                        
                                                        // 如果绑定了QQ，更新Bilibili MID
                                                        if (QQBindManager.hasBinding(p.uniqueId)) {
                                                            QQBindManager.updateBilibiliMid(p.uniqueId, userInfoData.mid)
                                                        }
                                                        
                                                        // 发送登录成功通知
                                                        QRCodeService.sendLoginSuccessNotification(p, userInfoData.uname)
                                                    }
                                                }
                                                this.cancel() // 取消任务
                                            }
                                        }

                                        86038 -> {
                                            // 处理二维码超时
                                            player.infoAsLang("GenerateUseCookieQRCodeTimeout")
                                            // 发送二维码过期通知
                                            QRCodeService.sendQRCodeExpiredNotification(p)
                                            this.cancel()
                                        }
                                        
                                        86090 -> {
                                            // 二维码已扫描，等待确认
                                            QRCodeService.sendScanSuccessNotification(p)
                                        }

                                        else -> return@submit
                                    }
                                }
                                // 更新玩家的库存
                                Bukkit.getPlayer(player.uniqueId)?.updateInventory()
                            } else {
                                // 网络请求失败处理
                                warningMessageAsLang("NetworkRequestFailureCode", response.code())
                            }
                        }
                    } else {
                        // 处理生成二维码失败的情况
                        player.infoAsLang("GenerateUseCookieFailure", response.body()?.message ?: ERROR_MESSAGE_NOT_PROVIDED)
                    }
                } else {
                    // 处理网络请求被拒绝的情况
                    player.infoAsLang("NetworkRequestRefuse", "HTTP受限，错误码：${response.code()}")
                }
            }

            override fun onFailure(call: Call<BilibiliResult<QRCodeGenerateData>>, t: Throwable) {
                // 处理请求失败的情况
                player.infoAsLang("NetworkRequestFailure", t.message ?: "未提供错误描述。")
            }
        })
    }

    /**
     * 获取三连状态
     * @param player 玩家
     * @param bvid 视频 BV 号
     */
    fun getTripleStatus(player: ProxyPlayer, bvid: String) {
        // 检查玩家是否已经获取过该视频的三连状态
        bvCache[player.uniqueId to bvid]?.let {
            if (it) {
                player.infoAsLang("GetTripleStatusRepeat")
                return
            }
        }
        // 获取玩家的 CSRF 令牌
        val csrf = cookieCache[player.uniqueId]?.biliJct ?: let {
            player.warningAsLang("CookieNotFound")
            return
        }
        // 获取玩家的 sessData
        val sessData = cookieCache[player.uniqueId]?.let { list -> list.sessData?.let { "SESSDATA=$it" } } ?: let {
            player.warningAsLang("CookieNotFound")
            return
        }
        // 调用 API 获取三连状态
        bilibiliAPI.actionLikeTriple(bvid, csrf, sessData).enqueue(object : Callback<BilibiliResult<TripleData>> {
            override fun onResponse(
                call: Call<BilibiliResult<TripleData>>, response: Response<BilibiliResult<TripleData>>
            ) {
                if (response.isSuccessful) {
                    response.body()?.let {
                        when (it.code) {
                            0 -> {
                                val tripleData = it.data
                                // 检查三连状态
                                if (tripleData.coin && tripleData.fav && tripleData.like) {
                                    player.setDataContainer(bvid, true.toString())
                                    bvCache.put(player.uniqueId to bvid, true)
                                    // 触发三连奖励事件
                                    TripleSendRewardsEvent(player, bvid).call()
                                } else {
                                    player.infoAsLang(
                                        "GetTripleStatusFailure", tripleData.like, tripleData.coin, tripleData.multiply, tripleData.fav
                                    )
                                }
                            }

                            -101 -> {
                                // 处理 cookie 无效的情况
                                player.infoAsLang("GetTripleStatusCookieInvalid")
                            }

                            10003 -> {
                                // 处理目标失败的情况
                                player.infoAsLang("GetTripleStatusTargetFailed")
                            }

                            else -> {
                                // 处理其他错误
                                player.infoAsLang(
                                    "GetTripleStatusError", response.body()?.message ?: ERROR_MESSAGE_NOT_PROVIDED
                                )
                            }
                        }
                    } ?: player.infoAsLang(
                        "GetTripleStatusRefuse", response.body()?.message ?: ERROR_MESSAGE_NOT_PROVIDED
                    )
                } else {
                    // 处理请求失败的情况
                    warning("请求失败")
                    warning("失败原因：${response.code()}")
                }
            }

            override fun onFailure(call: Call<BilibiliResult<TripleData>>, t: Throwable) {
                // 处理请求失败的情况
                player.infoAsLang("NetworkRequestFailure", t.message ?: "Bilibili未提供任何错误信息。")
            }
        })
    }

    /**
     * 获取三连状态（查看模式）
     * @param player 玩家
     * @param bvid 视频 BV 号
     */
    fun getTripleStatusShow(player: ProxyPlayer, bvid: String) {
        // 检查玩家是否已经获取过该视频的三连状态
        bvCache[player.uniqueId to bvid]?.let {
            if (it) {
                player.infoAsLang("GetTripleStatusRepeat")
                return
            }
        }
        // 获取玩家的 sessData
        val sessData = cookieCache[player.uniqueId]?.let { list ->
            list.sessData?.let { "SESSDATA=$it" }
        } ?: let {
            player.warningAsLang("CookieNotFound")
            return
        }
        // 处理 Show 模式下的三连状态
        if (showAction.handle(player, bvid, sessData)) {
            TripleSendRewardsEvent(player, bvid).call() // 触发三连奖励事件
        }
    }

    /**
     * 获取玩家绑定的用户信息
     * @param player 玩家
     * @return 返回用户信息数据，如果不存在则返回 null
     */
    fun getPlayerBindUserInfo(player: ProxyPlayer): UserInfoData? {
        return cookieCache[player.uniqueId]?.let {
            val userInfoData = getUserInfo(it) ?: return null
            userInfoData
        }
    }

    /**
     * 检查重复的 MID
     * @param player 玩家
     * @param cookie cookie 数据
     * @return 返回用户信息数据，如果数据库中存在该 MID 则返回 null
     */
    private fun checkRepeatabilityMid(player: ProxyPlayer, cookie: CookieData): UserInfoData? {
        val userInfo = getUserInfo(cookie) ?: return null
        // 如果数据库中存在该 MID 则返回 null，否则返回 MID
        return if (Database.searchPlayerByMid(player, userInfo.mid)) null else userInfo
    }

    /**
     * 获取用户信息
     * @param cookie cookie 数据
     * @return 返回用户信息数据，如果请求失败则返回 null
     */
    fun getUserInfo(cookie: CookieData): UserInfoData? {
        // 获取 SESSDATA
        val sessData = cookie.sessData?.let { "SESSDATA=$it" } ?: let {
            return null
        }
        // 获取用户信息
        val response = bilibiliAPI.getUserInfo(sessData).execute()
        // 判断请求是否成功并且返回的数据 code 是否为 0
        return when {
            response.isSuccessful -> {
                // 获取 MID
                response.body()?.data ?: return null
            }

            else -> null
        }
    }
}