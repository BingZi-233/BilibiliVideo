package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.config.RewardConfigManager
import online.bingzi.bilibili.video.internal.config.RewardTemplate
import online.bingzi.bilibili.video.internal.repository.RewardRecordRepository
import org.bukkit.entity.Player

/**
 * 奖励发放服务层。
 *
 * 当前实现：
 * - 基于「玩家 + bvid」检测是否完成三连
 * - 若完成且未领取过，则记录一条奖励记录
 * - 根据 rewardKey 从配置中载入 Kether 奖励模板，供命令层执行
 */
object RewardService {

    data class RewardResult(
        val success: Boolean,
        val message: String,
        val template: RewardTemplate? = null,
        val bvid: String? = null,
        val targetKey: String? = null
    )

    /**
     * 使用玩家绑定的 B 站账号，基于指定 bvid 发放奖励。
     *
     * @param player     Minecraft 玩家
     * @param bvid       视频 BVID
     * @param targetKey  奖励目标 key（默认使用 bvid）
     * @param rewardKey  奖励模板 key（默认 "default"）
     */
    fun rewardByPlayerAndBvid(
        player: Player,
        bvid: String,
        targetKey: String = bvid,
        rewardKey: String = "default"
    ): RewardResult {
        if (!RewardConfigManager.isBvidConfigured(bvid)) {
            return RewardResult(
                success = false,
                message = "该稿件未在 config.yml 的 reward.videos 中登记，无法领取奖励。"
            )
        }

        val tripleResult = CredentialService.checkTripleByPlayer(player, bvid)
        if (!tripleResult.success || tripleResult.tripleStatus == null) {
            return RewardResult(
                success = false,
                message = tripleResult.message
            )
        }

        val tripleStatus = tripleResult.tripleStatus
        if (!tripleStatus.isTriple) {
            return RewardResult(
                success = false,
                message = "尚未完成该视频的点赞、投币、收藏，无法领取奖励。"
            )
        }

        val playerUuid = player.uniqueId.toString()
        val existing = RewardRecordRepository.findAllByPlayerAndTarget(playerUuid, targetKey)
        // 先解析本次应当使用的奖励模板 key，便于做幂等判断
        val resolve = RewardConfigManager.resolveForBvid(bvid, defaultKey = rewardKey)
        val resolvedRewardKey = resolve.rewardKey

        if (existing.any { it.rewardKey == resolvedRewardKey && it.status == 1 }) {
            return RewardResult(
                success = false,
                message = "你已经领取过该任务的奖励，无法重复领取。"
            )
        }

        val inserted = RewardRecordRepository.insert(
            playerUuid = playerUuid,
            playerName = player.name,
            targetKey = targetKey,
            rewardKey = resolvedRewardKey,
            status = 1,
            context = "bvid=$bvid; triple=true",
            failReason = null
        )

        if (inserted <= 0) {
            return RewardResult(
                success = false,
                message = "记录奖励失败，请稍后重试。"
            )
        }

        val template = resolve.template
        val msg = if (template != null) {
            "已为你发放该任务的奖励（模板: $resolvedRewardKey）。"
        } else {
            "已记录你对该视频的三连行为，但尚未配置奖励模板。"
        }

        return RewardResult(
            success = true,
            message = msg,
            template = template,
            bvid = bvid,
            targetKey = targetKey
        )
    }
}
