package online.bingzi.bilibili.video.internal.service

import online.bingzi.bilibili.video.internal.entity.BoundAccount
import online.bingzi.bilibili.video.internal.repository.BoundAccountRepository
import taboolib.common.platform.function.info
import taboolib.common.platform.function.warning

/**
 * 账号绑定业务服务层。
 *
 * 只处理纯数据与业务规则，不持有任何 Bukkit 对象，避免内存泄漏。
 */
object BindingService {

    /**
     * 对外暴露的绑定信息视图。
     *
     * 避免直接泄露 internal 的实体类型。
     */
    data class BindingInfo(
        val playerUuid: String,
        val playerName: String,
        val bilibiliMid: Long,
        val bilibiliName: String,
        val status: Int,
        val createdAt: Long,
        val updatedAt: Long
    )

    /**
     * 绑定结果代码。
     */
    enum class BindResultCode {
        SUCCESS,
        ALREADY_BOUND_SAME,
        PLAYER_BOUND_OTHER,
        MID_BOUND_OTHER,
        DATABASE_ERROR
    }

    data class BindResult(
        val code: BindResultCode,
        val message: String
    ) {
        val success: Boolean get() = code == BindResultCode.SUCCESS || code == BindResultCode.ALREADY_BOUND_SAME
    }

    /**
     * 为玩家绑定 B 站账号。
     *
     * 业务规则：
     * - 一个玩家只能绑定一个 B 站账号；
     * - 一个 B 站账号只能绑定一个玩家；
     * - 同一玩家重复绑定同一 mid 直接返回成功，不重复写库。
     */
    fun bind(
        playerUuid: String,
        playerName: String,
        bilibiliMid: Long,
        bilibiliName: String
    ): BindResult {
        return try {
            val existingByPlayer = BoundAccountRepository.findByPlayerUuid(playerUuid, includeInactive = true)
            val activePlayerBinding = existingByPlayer?.takeIf { it.status == 1 }

            if (activePlayerBinding != null) {
                return if (activePlayerBinding.bilibiliMid == bilibiliMid) {
                    BoundAccountRepository.updateBinding(
                        playerUuid = playerUuid,
                        playerName = playerName,
                        bilibiliMid = bilibiliMid,
                        bilibiliName = bilibiliName,
                        status = 1
                    )
                    BindResult(
                        code = BindResultCode.ALREADY_BOUND_SAME,
                        message = "你已经绑定过该 B 站账号，无需重复操作。"
                    )
                } else {
                    BindResult(
                        code = BindResultCode.PLAYER_BOUND_OTHER,
                        message = "该玩家已绑定其他 B 站账号，如需更换请先解绑。"
                    )
                }
            }

            val existingByMid = BoundAccountRepository.findByBilibiliMid(bilibiliMid, includeInactive = true)
            val conflictByMid = existingByMid?.takeIf { it.status == 1 && it.playerUuid != playerUuid }
            if (conflictByMid != null) {
                return BindResult(
                    code = BindResultCode.MID_BOUND_OTHER,
                    message = "该 B 站账号已与其他玩家绑定。"
                )
            }

            val affected = if (existingByPlayer != null) {
                BoundAccountRepository.updateBinding(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    bilibiliMid = bilibiliMid,
                    bilibiliName = bilibiliName,
                    status = 1
                )
            } else {
                BoundAccountRepository.insert(
                    playerUuid = playerUuid,
                    playerName = playerName,
                    bilibiliMid = bilibiliMid,
                    bilibiliName = bilibiliName
                )
            }
            if (affected <= 0) {
                return BindResult(
                    code = BindResultCode.DATABASE_ERROR,
                    message = "数据库写入失败，请稍后重试。"
                )
            }

            val action = if (existingByPlayer == null) "完成账号绑定" else "重新激活账号绑定"
            info("[BindingService] 已为玩家 $playerName $action。")
            BindResult(
                code = BindResultCode.SUCCESS,
                message = "绑定成功。"
            )
        } catch (t: Throwable) {
            warning(
                "[BindingService] 绑定失败 | 玩家=$playerName($playerUuid) | B站=$bilibiliName($bilibiliMid) | 异常=${t.javaClass.simpleName}: ${t.message}",
                t
            )
            BindResult(
                code = BindResultCode.DATABASE_ERROR,
                message = "执行绑定时发生错误，请联系管理员。"
            )
        }
    }

    /**
     * 解绑结果代码。
     */
    enum class UnbindResultCode {
        SUCCESS,
        NOT_BOUND,
        DATABASE_ERROR
    }

    data class UnbindResult(
        val code: UnbindResultCode,
        val message: String
    ) {
        val success: Boolean get() = code == UnbindResultCode.SUCCESS
    }

    /**
     * 根据玩家 UUID 解绑。
     *
     * 当前实现为逻辑解绑：仅更新状态，不物理删除。
     */
    fun unbind(playerUuid: String): UnbindResult {
        return try {
            val existing = BoundAccountRepository.findByPlayerUuid(playerUuid)
            if (existing == null) {
                return UnbindResult(
                    code = UnbindResultCode.NOT_BOUND,
                    message = "未找到绑定记录。"
                )
            }

            val affected = BoundAccountRepository.updateStatusByPlayerUuid(playerUuid, status = 0)
            if (affected <= 0) {
                return UnbindResult(
                    code = UnbindResultCode.DATABASE_ERROR,
                    message = "数据库更新失败，请稍后重试。"
                )
            }

            info("[BindingService] 已为玩家 ${existing.playerName} 完成账号解绑。")
            UnbindResult(
                code = UnbindResultCode.SUCCESS,
                message = "解绑成功。"
            )
        } catch (t: Throwable) {
            warning(
                "[BindingService] 解绑失败 | 玩家UUID=$playerUuid | 异常=${t.javaClass.simpleName}: ${t.message}",
                t
            )
            UnbindResult(
                code = UnbindResultCode.DATABASE_ERROR,
                message = "执行解绑时发生错误，请联系管理员。"
            )
        }
    }

    /**
     * 查询玩家当前绑定信息。
     */
    fun getBoundAccount(playerUuid: String): BindingInfo? {
        val entity = BoundAccountRepository.findByPlayerUuid(playerUuid) ?: return null
        return entity.toBindingInfo()
    }

    private fun BoundAccount.toBindingInfo(): BindingInfo {
        return BindingInfo(
            playerUuid = playerUuid,
            playerName = playerName,
            bilibiliMid = bilibiliMid,
            bilibiliName = bilibiliName,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
