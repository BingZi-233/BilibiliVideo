package online.bingzi.bilibili.video.internal.handler

import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.debug
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer

/**
 * 收藏处理器类
 * 该类负责处理用户对视频的收藏相关操作，继承自 ApiHandler。
 *
 * @constructor 创建一个空的收藏处理器
 */
class FavouredHandler : ApiHandler() {

    /**
     * 处理玩家的收藏请求
     *
     * @param player 请求收藏的玩家对象，类型为 ProxyPlayer
     * @param bvid 要收藏的视频的 bvid 字符串，类型为 String
     * @param sessData 玩家会话数据，类型为 String
     * @return 返回一个布尔值，如果收藏成功则返回 true，否则返回 false
     */
    override fun handle(player: ProxyPlayer, bvid: String, sessData: String): Boolean {
        // 记录调试信息，显示玩家名称、视频 bvid 和处理状态
        debug("收藏处理器 > 玩家: ${player.name} | 视频: $bvid | 接受处理")

        // 发起网络请求，检查视频是否已被收藏
        NetworkEngine.bilibiliAPI.hasFavoured(bvid, sessData).execute().let { resultResponse ->
            // 判断网络请求是否成功
            if (resultResponse.isSuccessful) {
                // 处理请求返回的数据
                resultResponse.body()?.data?.let {
                    // 检查视频是否已经被收藏
                    if (it.favoured.not()) {
                        // 如果未收藏，向玩家发送信息并记录调试信息
                        player.infoAsLang("GetTripleStatusFailureNotFavoured")
                        debug("收藏处理器 > 玩家: ${player.name} | 视频: $bvid | 未收藏")
                        return false // 返回 false，表示未收藏
                    }
                }
            } else {
                // 如果网络请求失败，向玩家发送错误代码信息
                player.infoAsLang("NetworkRequestFailureCode", resultResponse.code())
            }
        }

        // 记录调试信息，表示处理成功，并将请求移交给下一个处理器
        debug("收藏处理器 > 玩家: ${player.name} | 视频: $bvid | 移交处理")
        return callNextHandler(player, bvid, sessData) // 调用下一个处理器并返回其结果
    }
}