package online.bingzi.bilibili.video.internal.placeholder

import me.clip.placeholderapi.expansion.Configurable
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import online.bingzi.bilibili.video.internal.service.BindingService
import online.bingzi.bilibili.video.internal.service.CredentialService
import org.bukkit.OfflinePlayer

/**
 * PlaceholderAPI 占位符扩展！
 * @author MaddyJace
 */
class PlaceholderExpansion : PlaceholderExpansion(), Configurable {

    /**
     * 默认配置
     */
    companion object {
        private val DEFAULTS = mapOf(
            "status.disabled" to "禁用",
            "status.normal" to "正常",
            "status.expired" to "过期",
            "status.unknown" to "未知(%status%)",
            "boolean.true" to "yes",
            "boolean.false" to "no",
            "options.null_value" to "null",
            "options.triple_error" to "default"
        )
    }

    override fun getDefaults(): Map<String, Any> = DEFAULTS
    override fun getIdentifier(): String = "bilibilivideo"
    override fun getAuthor(): String = "MaddyJace"
    override fun getVersion(): String = "1.0.0"

    override fun onRequest(player: OfflinePlayer?, identifier: String): String? {
        val nullFallback = getString("options.null_value", "null")
        val p = player?.player ?: return nullFallback

        val list = splitString(identifier)
        if (list.isEmpty()) return nullFallback

        val firstParam = list[0].lowercase()

        // 1. 绑定信息
        val binding = BindingService.getBoundAccount(player.uniqueId.toString())
        if (binding != null) {
            when (firstParam) {
                "name" -> return binding.playerName
                "uid" -> return binding.bilibiliMid.toString()
                "nickname" -> return binding.bilibiliName
            }
        }

        // 2. 凭证状态
        val credential = CredentialService.getCredentialInfo(p)
        if (credential != null) {
            val statusText = when (credential.status) {
                0 -> getString("status.disabled", "禁用")
                1 -> getString("status.normal", "正常")
                2 -> getString("status.expired", "过期")
                else -> getString("status.unknown", "未知(%status%)")
                    ?.replace("%status%", credential.status.toString())
            }

            when (firstParam) {
                "status" -> return statusText
                "label" -> return credential.label
                "binduid" -> {
                    val mid = credential.bilibiliMid?.toString()
                        ?: binding?.bilibiliMid?.toString()
                        ?: "nouid"
                    return mid
                }
            }
        }

        // 3. 三连检查
        if (firstParam == "triple" && list.size >= 3) {
            val type = list[1].lowercase()
            val bvid = list[2]
            val result = CredentialService.checkTripleByPlayer(p, bvid)

            val status = result.tripleStatus
            if (!result.success || status == null) {
                val customError = getString("options.triple_error", "default")
                return if (customError.equals("default", true)) result.message else result.message
            }

            val boolTrue = getString("boolean.true", "yes")
            val boolFalse = getString("boolean.false", "no")

            return when (type) {
                "liked" -> if (status.liked) boolTrue else boolFalse
                "coincount" -> status.coinCount.toString()
                "favoured" -> if (status.favoured) boolTrue else boolFalse
                "istriple" -> if (status.isTriple) boolTrue else boolFalse
                else -> nullFallback
            }
        }

        return nullFallback
    }

    private fun splitString(input: String): List<String> {
        val regex = "_(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()
        return input.split(regex).map { part ->
            if (part.startsWith("\"") && part.endsWith("\"") && part.length >= 2) {
                part.substring(1, part.length - 1)
            } else part
        }
    }

}