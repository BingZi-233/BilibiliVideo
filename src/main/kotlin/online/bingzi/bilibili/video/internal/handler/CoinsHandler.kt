package online.bingzi.bilibili.video.internal.handler

import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.debug
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer

/**
 * 硬币处理器类
 * 该类负责处理与硬币相关的逻辑，包括检查玩家是否有足够的硬币进行操作。
 * 继承自ApiHandler，作为处理链中的一部分。
 */
class CoinsHandler : ApiHandler() {

    /**
     * 处理硬币相关的请求
     * @param player 进行操作的玩家，类型为 ProxyPlayer，表示当前请求的玩家对象
     * @param bvid 视频的唯一标识符，类型为 String，表示需要投币的视频ID
     * @param sessData 会话数据，类型为 String，表示用户的会话信息
     * @return 返回 Boolean 类型，表示处理是否成功，true 表示处理成功，false 表示处理失败
     */
    override fun handle(player: ProxyPlayer, bvid: String, sessData: String): Boolean {
        // 记录调试信息，显示当前处理的玩家和视频信息
        debug("硬币处理器 > 玩家: ${player.name} | 视频: $bvid | 接受处理")

        // 调用网络引擎检查玩家是否拥有足够的硬币
        NetworkEngine.bilibiliAPI.hasCoins(bvid, sessData).execute().let {
            // 如果请求成功
            if (it.isSuccessful) {
                // 获取返回的数据中的硬币数量
                it.body()?.data?.multiply?.let { count ->
                    // 检查硬币数量是否小于1
                    if (count < 1) {
                        // 如果硬币不足，向玩家发送失败信息
                        player.infoAsLang("GetTripleStatusFailureNotCoins")
                        // 记录调试信息，显示硬币不足的情况
                        debug("硬币处理器 > 玩家: ${player.name} | 视频: $bvid | 硬币不足(已投币: $count, 需要投币: 2)")
                        return false // 返回处理失败
                    }
                }
            } else {
                // 如果请求失败，向玩家发送网络请求失败的信息
                player.infoAsLang("NetworkRequestFailureCode", it.code())
            }
        }
        // 记录调试信息，表示将请求移交给下一个处理器
        debug("硬币处理器 > 玩家: ${player.name} | 视频: $bvid | 移交处理")
        // 移交处理给下一个处理器
        return callNextHandler(player, bvid, sessData)
    }
}