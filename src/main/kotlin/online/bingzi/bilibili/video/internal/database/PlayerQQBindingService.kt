package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.database.dao.QQBindingDaoService
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 玩家QQ绑定服务
 * 提供玩家QQ绑定相关的高级功能
 */
object PlayerQQBindingService {

    /**
     * 获取玩家绑定的QQ号
     */
    fun getPlayerQQNumber(player: ProxyPlayer): CompletableFuture<Long?> {
        return getPlayerQQNumber(player.uniqueId)
    }

    /**
     * 根据玩家UUID获取绑定的QQ号
     */
    fun getPlayerQQNumber(playerUuid: UUID): CompletableFuture<Long?> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid).thenApply { binding ->
            if (binding?.isValidBinding() == true) {
                try {
                    binding.qqNumber.toLong()
                } catch (e: NumberFormatException) {
                    console().sendWarn("qqBindingInvalidFormat", binding.qqNumber, playerUuid.toString())
                    null
                }
            } else {
                null
            }
        }
    }

    /**
     * 获取玩家的完整QQ绑定信息
     */
    fun getPlayerQQBinding(player: ProxyPlayer): CompletableFuture<QQBinding?> {
        return getPlayerQQBinding(player.uniqueId)
    }

    /**
     * 根据玩家UUID获取完整QQ绑定信息
     */
    fun getPlayerQQBinding(playerUuid: UUID): CompletableFuture<QQBinding?> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid).thenApply { binding ->
            if (binding?.isValidBinding() == true) binding else null
        }
    }

    /**
     * 绑定玩家QQ号
     */
    fun bindPlayerQQ(player: ProxyPlayer, qqNumber: Long, qqNickname: String? = null): CompletableFuture<Boolean> {
        return bindPlayerQQ(player.uniqueId, qqNumber, qqNickname)
            .thenApply { success ->
                if (success) {
                    player.sendInfo("qqBindSuccess", qqNumber.toString())
                    console().sendInfo("qqBindingPlayerBound", player.name, qqNumber.toString())
                } else {
                    player.sendError("qqBindingFailed")
                }
                success
            }
    }

    /**
     * 根据UUID绑定玩家QQ号
     */
    fun bindPlayerQQ(playerUuid: UUID, qqNumber: Long, qqNickname: String? = null): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync {
            // 验证QQ号格式
            if (!isValidQQNumber(qqNumber)) {
                console().sendWarn("qqBindingInvalidNumber", qqNumber.toString())
                return@supplyAsync false
            }
            
            // 检查QQ号是否已被其他玩家绑定
            val existingBinding = QQBindingDaoService.getQQBindingByNumber(qqNumber.toString()).get()
            if (existingBinding != null && existingBinding.playerUuid != playerUuid.toString()) {
                console().sendWarn("qqBindingAlreadyBound", qqNumber.toString(), existingBinding.playerUuid)
                return@supplyAsync false
            }
            
            // 检查玩家是否已有绑定
            val currentBinding = QQBindingDaoService.getQQBindingByPlayer(playerUuid).get()
            
            val binding = if (currentBinding != null) {
                // 更新现有绑定
                currentBinding.apply {
                    this.qqNumber = qqNumber.toString()
                    this.qqNickname = qqNickname
                    this.bindTime = System.currentTimeMillis()
                    this.setBindStatus(QQBinding.Status.ACTIVE)
                    this.updateActiveStatus(true)
                    this.updateLastVerifyTime()
                }
            } else {
                // 创建新绑定
                QQBinding(
                    playerUuid = playerUuid.toString(),
                    qqNumber = qqNumber.toString(),
                    qqNickname = qqNickname,
                    bindTime = System.currentTimeMillis(),
                    bindStatus = QQBinding.Status.ACTIVE.value,
                    isActive = true
                ).apply {
                    updateLastVerifyTime()
                }
            }
            
            QQBindingDaoService.saveQQBinding(binding).get()
        }
    }

    /**
     * 解绑玩家QQ号
     */
    fun unbindPlayerQQ(player: ProxyPlayer): CompletableFuture<Boolean> {
        return unbindPlayerQQ(player.uniqueId)
            .thenApply { success ->
                if (success) {
                    player.sendInfo("qqUnbindSuccess")
                    console().sendInfo("qqBindingPlayerUnbound", player.name)
                } else {
                    player.sendError("qqUnbindingFailed")
                }
                success
            }
    }

    /**
     * 根据UUID解绑玩家QQ号
     */
    fun unbindPlayerQQ(playerUuid: UUID): CompletableFuture<Boolean> {
        return QQBindingDaoService.deleteQQBinding(playerUuid)
    }

    /**
     * 检查玩家是否有有效的QQ绑定
     */
    fun hasValidQQBinding(player: ProxyPlayer): CompletableFuture<Boolean> {
        return hasValidQQBinding(player.uniqueId)
    }

    /**
     * 根据UUID检查玩家是否有有效的QQ绑定
     */
    fun hasValidQQBinding(playerUuid: UUID): CompletableFuture<Boolean> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid)
            .thenApply { binding -> binding?.isValidBinding() == true }
    }

    /**
     * 验证QQ号格式是否有效
     * QQ号应该是5-10位数字
     */
    private fun isValidQQNumber(qqNumber: Long): Boolean {
        val qqStr = qqNumber.toString()
        return qqStr.length in 5..10 && qqStr.all { it.isDigit() } && qqNumber > 0
    }

    /**
     * 刷新QQ绑定的验证时间
     */
    fun refreshVerifyTime(player: ProxyPlayer): CompletableFuture<Boolean> {
        return refreshVerifyTime(player.uniqueId)
    }

    /**
     * 根据UUID刷新QQ绑定的验证时间
     */
    fun refreshVerifyTime(playerUuid: UUID): CompletableFuture<Boolean> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid)
            .thenCompose { binding ->
                if (binding?.isValidBinding() == true) {
                    binding.updateLastVerifyTime()
                    QQBindingDaoService.saveQQBinding(binding)
                } else {
                    CompletableFuture.completedFuture(false)
                }
            }
    }

    /**
     * 根据QQ号获取绑定的玩家UUID
     */
    fun getPlayerByQQNumber(qqNumber: Long): CompletableFuture<String?> {
        return QQBindingDaoService.getQQBindingByNumber(qqNumber.toString())
            .thenApply { binding ->
                if (binding?.isValidBinding() == true) {
                    binding.playerUuid
                } else {
                    null
                }
            }
    }

    /**
     * 更新QQ绑定状态
     */
    fun updateQQBindingStatus(playerUuid: UUID, status: QQBinding.Status): CompletableFuture<Boolean> {
        return QQBindingDaoService.getQQBindingByPlayer(playerUuid)
            .thenCompose { binding ->
                if (binding != null) {
                    binding.setBindStatus(status)
                    binding.updateLastVerifyTime()
                    QQBindingDaoService.saveQQBinding(binding)
                } else {
                    CompletableFuture.completedFuture(false)
                }
            }
    }
}