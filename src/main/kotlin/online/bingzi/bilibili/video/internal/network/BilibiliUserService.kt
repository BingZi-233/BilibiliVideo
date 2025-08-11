package online.bingzi.bilibili.video.internal.network

import com.google.gson.Gson
import com.google.gson.JsonParser
import online.bingzi.bilibili.video.internal.network.entity.UserDetailInfo
import online.bingzi.bilibili.video.internal.network.entity.UserInfo
import online.bingzi.bilibili.video.internal.network.entity.UserStats
import taboolib.common.platform.function.console
import taboolib.module.lang.sendWarn
import java.util.concurrent.CompletableFuture

/**
 * Bilibili 用户服务
 * 提供用户信息获取等功能
 */
object BilibiliUserService {

    private val gson = Gson()

    /**
     * 获取当前登录用户的基本信息
     * @return 用户信息或 null
     */
    fun getCurrentUserInfo(): CompletableFuture<UserInfo?> {
        if (!BilibiliCookieJar.isLoggedIn()) {
            return CompletableFuture.completedFuture(null)
        }

        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/web-interface/nav")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val mid = data.get("mid")?.asLong
                            val uname = data.get("uname")?.asString
                            val face = data.get("face")?.asString
                            val level = data.get("level_info")?.asJsonObject?.get("current_level")?.asInt ?: 0
                            val coins = data.get("money")?.asDouble ?: 0.0

                            if (mid != null && uname != null) {
                                return@thenApply UserInfo(
                                    uid = mid,
                                    username = uname,
                                    avatar = face,
                                    level = level,
                                    coins = coins
                                )
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userInfoGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userInfoParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 获取指定用户的详细信息
     * @param uid 用户 UID
     * @return 用户详细信息或 null
     */
    fun getUserInfo(uid: Long): CompletableFuture<UserDetailInfo?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/space/acc/info?mid=$uid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val mid = data.get("mid")?.asLong
                            val name = data.get("name")?.asString
                            val face = data.get("face")?.asString
                            val sign = data.get("sign")?.asString
                            val level = data.get("level")?.asInt ?: 0
                            val sex = data.get("sex")?.asString

                            if (mid != null && name != null) {
                                return@thenApply UserDetailInfo(
                                    uid = mid,
                                    name = name,
                                    avatar = face,
                                    signature = sign,
                                    level = level,
                                    gender = sex
                                )
                            }
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userDetailGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userDetailParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }

    /**
     * 获取用户的关注统计信息
     * @param uid 用户 UID
     * @return 关注统计信息或 null
     */
    fun getUserStats(uid: Long): CompletableFuture<UserStats?> {
        return BilibiliApiClient.getAsync("https://api.bilibili.com/x/relation/stat?vmid=$uid")
            .thenApply { response ->
                if (response.isSuccess()) {
                    try {
                        val json = JsonParser.parseString(response.data).asJsonObject
                        val code = json.get("code")?.asInt ?: -1

                        if (code == 0) {
                            val data = json.getAsJsonObject("data")

                            val following = data.get("following")?.asLong ?: 0L
                            val follower = data.get("follower")?.asLong ?: 0L

                            return@thenApply UserStats(
                                uid = uid,
                                following = following,
                                follower = follower
                            )
                        } else {
                            val message = json.get("message")?.asString ?: "未知错误"
                            console().sendWarn("userStatsGetFailed", message)
                        }
                    } catch (e: Exception) {
                        console().sendWarn("userStatsParseError", e.message ?: "")
                    }
                } else {
                    console().sendWarn("networkApiRequestFailed", response.getError() ?: "")
                }
                null
            }
    }
}