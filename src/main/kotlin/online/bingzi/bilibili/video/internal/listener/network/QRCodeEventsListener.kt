package online.bingzi.bilibili.video.internal.listener.network

import online.bingzi.bilibili.video.api.event.BilibiliQRCodeGeneratedResultEvent
import online.bingzi.bilibili.video.api.event.BilibiliQRCodeScanUpdateEvent
import online.bingzi.bilibili.video.internal.cache.*
import online.bingzi.bilibili.video.internal.database.Database
import online.bingzi.bilibili.video.internal.engine.NetworkEngine // 需要访问 NetworkEngine.getUserInfo
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import online.bingzi.bilibili.video.internal.helper.sendMap
import online.bingzi.bilibili.video.internal.helper.toBufferedImage
import org.bukkit.Bukkit
import taboolib.common.platform.event.SubscribeEvent
import taboolib.common.platform.function.submit
import taboolib.module.chat.colored
import online.bingzi.bilibili.video.internal.database.Database.Companion.setDataContainer // 确保引入

object QRCodeEventsListener {

    @SubscribeEvent
    fun onQRCodeGenerated(event: BilibiliQRCodeGeneratedResultEvent) {
        val player = event.player
        val target = event.target ?: player // 如果 target 为 null，则默认为 player

        if (event.isSuccess && event.qrCodeGenerateData != null) {
            player.sendMap(event.qrCodeGenerateData.url.toBufferedImage(128)) {
                name = "&a&lBilibili扫码登陆".colored()
                shiny()
                lore.clear()
                lore.addAll(
                    listOf(
                        "&7请使用Bilibili客户端扫描二维码",
                        "&7二维码有效期为3分钟",
                    ).colored()
                )
            }
            // 提示信息由 NetworkEngine 的轮询逻辑中触发扫码状态事件时再发出
        } else {
            player.infoAsLang("GenerateUseCookieFailure", event.errorMessage ?: "未知错误")
        }
    }

    @SubscribeEvent
    fun onQRCodeScanned(event: BilibiliQRCodeScanUpdateEvent) {
        val player = event.player // player 是指最初发起请求的玩家
        val targetUser = event.target ?: player // targetUser 是实际扫码绑定的玩家

        when (event.scanStatusCode) {
            0 -> { // 成功
                if (event.cookieData != null && event.refreshToken != null && event.timestamp != null) {
                    val userInfoData = NetworkEngine.getUserInfo(event.cookieData) // 调用NetworkEngine的公共方法

                    if (userInfoData == null) {
                        targetUser.infoAsLang("GenerateUseCookieFailure", "无法获取B站用户信息")
                        qrCodeKeyCache.remove(event.qrCodeKey)
                        return
                    }

                    // 检查MID重复 (原 checkRepeatabilityMid 逻辑)
                    if (Database.searchPlayerByMid(targetUser, userInfoData.mid)) {
                        targetUser.infoAsLang("GenerateUseCookieRepeatabilityMid")
                        qrCodeKeyCache.remove(event.qrCodeKey)
                        return
                    }

                    val cacheMid = midCache[targetUser.uniqueId]
                    if (!cacheMid.isNullOrBlank() && cacheMid != userInfoData.mid) {
                        targetUser.infoAsLang("PlayerIsBindMid")
                        qrCodeKeyCache.remove(event.qrCodeKey)
                        return
                    }

                    cookieCache[targetUser.uniqueId] = event.cookieData
                    midCache[targetUser.uniqueId] = userInfoData.mid
                    unameCache[targetUser.uniqueId] = userInfoData.uname
                    targetUser.setDataContainer("mid", userInfoData.mid)
                    targetUser.setDataContainer("uname", userInfoData.uname)
                    targetUser.setDataContainer("refresh_token", event.refreshToken)
                    targetUser.setDataContainer("timestamp", event.timestamp.toString())
                    targetUser.infoAsLang("GenerateUseCookieSuccess")

                    submit { // 确保在主线程更新玩家背包
                        Bukkit.getPlayer(targetUser.uniqueId)?.updateInventory()
                    }
                } else {
                    targetUser.infoAsLang("GenerateUseCookieFailure", "扫码成功但数据不完整: ${event.message}")
                }
                qrCodeKeyCache.remove(event.qrCodeKey)
            }
            86038 -> { // 二维码已过期
                (event.target ?: event.player).infoAsLang("GenerateUseCookieQRCodeTimeout")
                qrCodeKeyCache.remove(event.qrCodeKey)
            }
            86090, 86101 -> { // 86090: 等待扫码, 86101: 未确认 (客户端可能已扫码，但用户未在客户端上点击确认)
                // 这些是中间状态，可以不特别提示玩家，或只在控制台打印日志
                // taboolib.common.platform.function.info("二维码 ${event.qrCodeKey} 状态: ${event.scanStatusCode} - ${event.message}")
            }
            else -> { // 其他错误，例如 -1 (自定义：响应体为空), -2 (自定义：网络请求失败) 或B站其他错误码
                (event.target ?: event.player).infoAsLang("GenerateUseCookieFailure", "扫码失败 (${event.scanStatusCode}): ${event.message ?: "未知扫码错误"}")
                qrCodeKeyCache.remove(event.qrCodeKey) // 对于未知或确定失败的状态也清理缓存
            }
        }
    }
} 