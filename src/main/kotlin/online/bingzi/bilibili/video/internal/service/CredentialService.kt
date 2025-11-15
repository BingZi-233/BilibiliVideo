package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.bilibili.dto.TripleStatusResult
import online.bingzi.bilibili.video.internal.entity.Credential
import online.bingzi.bilibili.video.internal.repository.BoundAccountRepository
import online.bingzi.bilibili.video.internal.repository.CredentialRepository
import org.bukkit.entity.Player
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 凭证服务层。
 *
 * 负责：
 * - 从数据库中按 label 或玩家绑定加载 B 站凭证
 * - 对外提供安全的 DTO 视图
 * - 基于 label 或 Player 调用三连检测服务
 */
object CredentialService {

    /**
     * 对外暴露的凭证信息视图。
     */
    data class CredentialInfo(
        val label: String,
        val sessData: String,
        val biliJct: String,
        val buvid3: String?,
        val accessKey: String?,
        val refreshToken: String?,
        val status: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val expiredAt: Long?,
        val lastUsedAt: Long?,
        val bilibiliMid: Long?
    )

    /**
     * 三连检测结果代码（可用于 label 或 Player）。
     */
    enum class TripleCheckCode {
        SUCCESS,
        PLAYER_NOT_BOUND,
        CREDENTIAL_NOT_FOUND,
        CREDENTIAL_DISABLED,
        CREDENTIAL_EXPIRED,
        TRIPLE_CHECK_FAILED
    }

    /**
     * 三连检测结果。
     */
    data class TripleCheckResult(
        val code: TripleCheckCode,
        val message: String,
        val tripleStatus: TripleStatusResult? = null
    ) {
        val success: Boolean get() = code == TripleCheckCode.SUCCESS
    }

    /**
     * 根据 label 获取凭证信息 DTO。
     */
    fun getCredentialInfo(label: String): CredentialInfo? {
        val entity = CredentialRepository.findByLabel(label) ?: return null
        return entity.toCredentialInfo()
    }

    /**
     * 根据玩家对象获取其绑定的凭证信息 DTO（如果存在绑定与凭证）。
     */
    fun getCredentialInfo(player: Player): CredentialInfo? {
        val bound = BoundAccountRepository.findByPlayerUuid(player.uniqueId.toString()) ?: return null
        val credential = bound.bilibiliMid.let { mid ->
            CredentialRepository.findByBilibiliMid(mid)
        } ?: return null
        return credential.toCredentialInfo()
    }

    /**
     * 使用指定 label 的凭证，对某个 bvid 进行三连状态检测。
     */
    fun checkTripleByLabel(label: String, bvid: String): TripleCheckResult {
        val entity = CredentialRepository.findByLabel(label)
            ?: return TripleCheckResult(
                code = TripleCheckCode.CREDENTIAL_NOT_FOUND,
                message = "未找到名为 $label 的凭证。"
            )

        return internalCheckTripleWithCredential(entity, bvid) {
            "[CredentialService] 三连状态检测成功（来源=命名凭证，bvid=$bvid）"
        }
    }

    /**
     * 使用玩家对象自动获取绑定的 B 站账号凭证，并对某个 bvid 进行三连状态检测。
     */
    fun checkTripleByPlayer(player: Player, bvid: String): TripleCheckResult {
        val bound = BoundAccountRepository.findByPlayerUuid(player.uniqueId.toString())
            ?: return TripleCheckResult(
                code = TripleCheckCode.PLAYER_NOT_BOUND,
                message = "你还没有绑定 B 站账号。"
            )

        val entity = CredentialRepository.findByBilibiliMid(bound.bilibiliMid)
            ?: return TripleCheckResult(
                code = TripleCheckCode.CREDENTIAL_NOT_FOUND,
                message = "未找到与你绑定的 B 站账号对应的凭证。"
            )

        return internalCheckTripleWithCredential(entity, bvid) {
            "[CredentialService] 三连状态检测成功（来源=玩家绑定，bvid=$bvid）"
        }
    }

    /**
     * 使用给定凭证实体执行三连检测的公共逻辑。
     */
    private fun internalCheckTripleWithCredential(
        entity: Credential,
        bvid: String,
        successLogBuilder: () -> String
    ): TripleCheckResult {
        if (entity.status == 0) {
            return TripleCheckResult(
                code = TripleCheckCode.CREDENTIAL_DISABLED,
                message = "B 站凭证已被禁用。"
            )
        }
        if (entity.status == 2 || (entity.expiredAt != null && entity.expiredAt!! > 0 && entity.expiredAt!! < System.currentTimeMillis())) {
            return TripleCheckResult(
                code = TripleCheckCode.CREDENTIAL_EXPIRED,
                message = "B 站凭证已过期。"
            )
        }

        return try {
            val triple = TripleCheckService.checkTripleByBvid(
                sessData = entity.sessData,
                biliJct = entity.biliJct,
                buvid3 = entity.buvid3,
                accessKey = entity.accessKey,
                bvid = bvid
            )

            runCatching {
                CredentialRepository.updateStatusAndUsage(
                    label = entity.label,
                    status = entity.status,
                    expiredAt = entity.expiredAt,
                    lastUsedAt = System.currentTimeMillis()
                )
            }

            info(successLogBuilder())
            TripleCheckResult(
                code = TripleCheckCode.SUCCESS,
                message = "三连状态检测成功。",
                tripleStatus = triple
            )
        } catch (t: Throwable) {
            warning("[CredentialService] 三连状态检测调用失败：${t.message}")
            TripleCheckResult(
                code = TripleCheckCode.TRIPLE_CHECK_FAILED,
                message = "调用 B 站接口失败，请稍后重试或检查凭证。"
            )
        }
    }

    private fun Credential.toCredentialInfo(): CredentialInfo {
        return CredentialInfo(
            label = label,
            sessData = sessData,
            biliJct = biliJct,
            buvid3 = buvid3,
            accessKey = accessKey,
            refreshToken = refreshToken,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiredAt = expiredAt,
            lastUsedAt = lastUsedAt,
            bilibiliMid = bilibiliMid
        )
    }
}
