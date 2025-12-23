package online.bingzi.bilibili.video.internal.config

import taboolib.common.platform.function.warning
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigFile

/**
 * 奖励判重策略枚举。
 */
enum class DedupStrategy {
    /**
     * 玩家和 B 站账户同时记录（最严格）。
     * 同一玩家 + 同一 B 站号只能领一次。
     */
    PLAYER_AND_BILIBILI,

    /**
     * 仅记录 B 站账户。
     * 同一 B 站号只能领一次，玩家换号可重新领。
     */
    BILIBILI_ONLY,

    /**
     * 仅记录玩家。
     * 同一玩家只能领一次，换号无法重新领。
     */
    PLAYER_ONLY
}

/**
 * 奖励模板配置。
 *
 * 从 config.yml 中读取 reward.templates 节点，并转换为 RewardTemplate。
 */
data class RewardTemplate(
    val key: String,
    val description: String?,
    val lines: List<String>
)

object RewardConfigManager {

    @Config("config.yml")
    lateinit var file: ConfigFile
        private set

    fun getTemplate(key: String): RewardTemplate? {
        val basePath = "reward.templates.$key"
        val section = file.getConfigurationSection(basePath) ?: return null
        val description = section.getString("description")
        val scripts = section.getStringList("kether")
        if (scripts.isEmpty()) {
            return null
        }
        return RewardTemplate(
            key = key,
            description = description,
            lines = scripts
        )
    }

    /**
     * 根据 bvid 解析应该使用的奖励模板：
     *
     * - 如果 reward.videos.<bvid>.rewardKey 存在且非空，则使用该 key；
     * - 如果 reward.videos.<bvid> 结点存在但未配置 rewardKey，则回退到 defaultKey；
     * - 如果 reward.videos.<bvid> 不存在，则同样回退到 defaultKey。
     *
     * 即：对任意 bvid 至少尝试使用 defaultKey，对应模板不存在时返回的 template 为 null。
     */
    data class ResolveResult(
        val rewardKey: String,
        val template: RewardTemplate?
    )

    fun resolveForBvid(bvid: String, defaultKey: String = "default"): ResolveResult {
        val videoPath = "reward.videos.$bvid"
        val videoSection = file.getConfigurationSection(videoPath)
        val configuredKey = videoSection
            ?.getString("rewardKey")
            ?.takeIf { it.isNotBlank() }

        val keyToUse = configuredKey ?: defaultKey
        val template = getTemplate(keyToUse)
        return ResolveResult(
            rewardKey = keyToUse,
            template = template
        )
    }

    fun reload(): Boolean {
        return try {
            file.reload()
            true
        } catch (ex: Throwable) {
            warning("重载 config.yml 失败: ${ex.message}", ex)
            false
        }
    }

    /**
     * 返回 config.yml 中 reward.videos 节点下配置的全部 bvid（不含子键）。
     */
    fun getConfiguredBvids(): List<String> {
        val videosSection = file.getConfigurationSection("reward.videos") ?: return emptyList()
        return videosSection.getKeys(false).toList()
    }

    /**
     * 判断某个 bvid 是否在 config.yml 的 reward.videos 节点下存在。
     */
    fun isBvidConfigured(bvid: String): Boolean {
        if (bvid.isBlank()) {
            return false
        }
        val videosSection = file.getConfigurationSection("reward.videos") ?: return false
        return videosSection.getKeys(false).any { it.equals(bvid, ignoreCase = false) }
    }

    /**
     * 获取奖励判重策略。
     *
     * 从 config.yml 中读取 reward.dedup-strategy 节点，默认为 PLAYER_ONLY（兼容旧行为）。
     */
    fun getDedupStrategy(): DedupStrategy {
        val strategyName = file.getString("reward.dedup-strategy")?.uppercase() ?: return DedupStrategy.PLAYER_ONLY
        return try {
            DedupStrategy.valueOf(strategyName.replace("-", "_"))
        } catch (e: IllegalArgumentException) {
            warning("无效的 reward.dedup-strategy 配置值: $strategyName，使用默认值 PLAYER_ONLY")
            DedupStrategy.PLAYER_ONLY
        }
    }
}
