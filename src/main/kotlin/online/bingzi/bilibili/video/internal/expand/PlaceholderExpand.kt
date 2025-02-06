package online.bingzi.bilibili.video.internal.expand

import online.bingzi.bilibili.video.internal.cache.bvCache
import online.bingzi.bilibili.video.internal.cache.midCache
import online.bingzi.bilibili.video.internal.cache.unameCache
import org.bukkit.entity.Player
import taboolib.platform.compat.PlaceholderExpansion

/**
 * 变量拓展类
 * 该对象用于扩展占位符，提供与 Bilibili 视频相关的动态数据。
 * 主要功能是根据玩家的请求返回相应的占位符数据。
 */
object PlaceholderExpand : PlaceholderExpansion {
    /**
     * 占位符标识符
     * 返回该扩展的标识符，用于在占位符中进行识别。
     */
    override val identifier: String
        get() = "BilibiliVideo"

    /**
     * 占位符请求处理
     * 根据玩家和参数请求返回相应的占位符数据。
     *
     * @param player 触发请求的玩家，类型为 Player，可能为 null。
     *               如果为 null，表示没有玩家触发请求。
     * @param args 请求的参数，类型为 String，包含要获取的数据标识。
     *               可取值包括 "uid"、"uname"、"check_某个参数" 等。
     * @return 返回对应的占位符数据，类型为 String。
     *         如果参数无效或玩家为 null，将返回 "N/A" 或其他默认信息。
     */
    override fun onPlaceholderRequest(player: Player?, args: String): String {
        // 如果玩家为 null，返回不可用的标识
        if (player == null) {
            return "N/A"
        }

        // 将请求参数按下划线分割成列表
        val argsList: List<String> = args.split("_")

        // 根据分割后的参数数量处理不同的请求
        return when (argsList.size) {
            1 -> when (args) {
                // 请求用户 ID
                "uid" -> midCache[player.uniqueId] ?: "N/A-缓存"
                // 请求用户名
                "uname" -> unameCache[player.uniqueId] ?: "N/A-缓存"
                // 处理未知的参数
                else -> "N/A-未知参数"
            }

            2 -> when (argsList[0]) {
                // 检查特定的缓存状态
                "check" -> (bvCache.get(Pair(player.uniqueId, argsList[1]))?.toString() ?: "false").lowercase()
                // 处理未知的参数
                else -> "N/A"
            }

            // 处理参数数量不符合预期的情况
            else -> "N/A"
        }
    }
}