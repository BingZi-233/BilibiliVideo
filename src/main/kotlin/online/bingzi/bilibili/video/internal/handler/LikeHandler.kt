package online.bingzi.bilibili.video.internal.handler

import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.debug
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer

/**
 * 点赞处理器类
 * 该类负责处理用户对视频的点赞请求，检查用户是否已经点赞，并决定是否将请求转交给下一个处理器。
 *
 * @constructor 创建一个空的点赞处理器实例
 */
class LikeHandler : ApiHandler() {

    /**
     * 处理点赞请求
     *
     * @param player 进行点赞的玩家，类型为 ProxyPlayer，表示当前进行操作的玩家实例。
     * @param bvid 视频的唯一标识符，类型为 String，表示需要点赞的视频ID。
     * @param sessData 会话数据，类型为 String，表示用户的会话信息，用于API请求验证。
     * @return Boolean 返回操作是否成功，true表示请求已成功转交，false表示点赞失败或未点赞。
     */
    override fun handle(player: ProxyPlayer, bvid: String, sessData: String): Boolean {
        // 记录调试信息，输出当前玩家、视频以及处理状态
        debug("点赞处理器 > 玩家: ${player.name} | 视频: $bvid | 接受处理")

        // 发起网络请求，检查玩家是否已对视频点赞
        NetworkEngine.bilibiliAPI.hasLike(bvid, sessData).execute().let {
            // 如果请求成功
            if (it.isSuccessful) {
                // 获取响应体中的数据
                it.body()?.data?.let { count ->
                    // 检查点赞计数，如果小于1则表示未点赞
                    if (count < 1) {
                        // 提示玩家未点赞的消息
                        player.infoAsLang("GetTripleStatusFailureNotLike")
                        // 记录调试信息，输出玩家和视频状态
                        debug("点赞处理器 > 玩家: ${player.name} | 视频: $bvid | 未点赞")
                        return false // 返回false，表示点赞失败
                    }
                }
            } else {
                // 如果请求失败，提示玩家网络请求失败的信息
                player.infoAsLang("NetworkRequestFailureCode", it.code())
            }
        }

        // 记录调试信息，输出将请求移交给下一个处理器的状态
        debug("点赞处理器 > 玩家: ${player.name} | 视频: $bvid | 移交处理")
        return callNextHandler(player, bvid, sessData) // 将请求转交给下一个处理器
    }
}