package online.bingzi.bilibili.bilibilivideo.internal.command.handler

import online.bingzi.bilibili.bilibilivideo.api.qrcode.registry.QRCodeSenderRegistry
import online.bingzi.bilibili.bilibilivideo.internal.config.SettingConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.DatabaseManager
import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import online.bingzi.bilibili.bilibilivideo.internal.manager.BvManager
import online.bingzi.bilibili.bilibilivideo.internal.session.SessionManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.module.lang.sendInfo

/**
 * 状态检查命令处理器
 */
object StatusCommandHandler {

    fun handleStatus(sender: ProxyCommandSender) {
        val dbInfo = DatabaseManager.getDatabaseInfo()
        val tableNames = TableFactory.getTableNames().joinToString(", ").ifEmpty { "无" }

        val configuredVideos = SettingConfig.getAllConfiguredBvids().size
        val rewardEnabledVideos = BvManager.getRewardEnabledBvidCount()

        val defaultReward = SettingConfig.getDefaultRewardConfig()
        val rewardSettings = SettingConfig.getRewardSettings()

        val senders = QRCodeSenderRegistry.getAvailableSenders()
        val activeSender = QRCodeSenderRegistry.getActiveSender()?.id ?: "无"

        val sessionCount = SessionManager.getActiveSessionCount()

        sender.sendInfo("statusHeader")
        sender.sendInfo("statusDatabase", dbInfo)
        sender.sendInfo("statusTables", tableNames)
        sender.sendInfo(
            "statusRewardSettings",
            rewardSettings.preventDuplicateRewards.toString(),
            rewardSettings.rewardDelay.toString(),
            rewardSettings.playSound.toString()
        )
        sender.sendInfo(
            "statusDefaultReward",
            defaultReward.enabled.toString(),
            defaultReward.requireCompleteTriple.toString(),
            defaultReward.rewards.size.toString()
        )
        sender.sendInfo("statusConfiguredVideos", configuredVideos.toString())
        sender.sendInfo("statusRewardEnabledVideos", rewardEnabledVideos.toString())
        sender.sendInfo("statusQRCodeSenders", senders.size.toString())
        sender.sendInfo("statusQRCodeActive", activeSender)
        sender.sendInfo("statusSessions", sessionCount.toString())
        sender.sendInfo("statusCommands", "login, logout, triple, follow, status")
        sender.sendInfo("statusDone")
    }
}
