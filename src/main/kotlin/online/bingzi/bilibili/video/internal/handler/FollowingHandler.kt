package online.bingzi.bilibili.video.internal.handler

import online.bingzi.bilibili.video.internal.engine.NetworkEngine
import online.bingzi.bilibili.video.internal.helper.debug
import online.bingzi.bilibili.video.internal.helper.infoAsLang
import taboolib.common.platform.ProxyPlayer

/**
 * 关注处理器类
 * 该类负责处理用户关注相关的逻辑。
 * 主要功能是检查一个用户是否关注了某个视频，并根据结果进行相应的处理。
 */
class FollowingHandler : ApiHandler() {

    /**
     * 处理关注逻辑
     *
     * @param player 触发处理的玩家对象，类型为 ProxyPlayer，表示游戏中的玩家
     * @param bvid 视频的唯一标识符，类型为 String，表示要检查关注状态的视频ID
     * @param sessData 会话数据，类型为 String，包含用户的会话信息
     * @return Boolean 返回处理结果，成功返回 true，失败返回 false
     */
    override fun handle(player: ProxyPlayer, bvid: String, sessData: String): Boolean {
        // 输出调试信息，显示当前处理的玩家和视频
        debug("关注处理器 > 玩家: ${player.name} | 视频: $bvid | 接受处理")

        // 调用网络引擎检查玩家是否关注了指定视频
        NetworkEngine.bilibiliAPI.hasFollowing(bvid, sessData).execute().let { resultResponse ->
            // 如果网络请求成功
            if (resultResponse.isSuccessful) {
                // 获取响应体中的数据
                resultResponse.body()?.data?.let {
                    // 检查用户是否未关注该视频
                    if (it.card.following.not()) {
                        // 如果未关注，向玩家发送未关注的提示信息
                        player.infoAsLang("GetTripleStatusFailureNotFollowing")
                        // 输出调试信息，说明玩家未关注该视频
                        debug("关注处理器 > 玩家: ${player.name} | 视频: $bvid | 未关注")
                        // 返回处理失败
                        return false
                    }
                }
            } else {
                // 如果网络请求失败，向玩家发送错误码信息
                player.infoAsLang("NetworkRequestFailureCode", resultResponse.code())
            }
        }

        // 输出调试信息，说明已将处理移交给下一个处理器
        debug("关注处理器 > 玩家: ${player.name} | 视频: $bvid | 移交处理")
        // 调用下一个处理器进行处理，并返回结果
        return callNextHandler(player, bvid, sessData)
    }
}