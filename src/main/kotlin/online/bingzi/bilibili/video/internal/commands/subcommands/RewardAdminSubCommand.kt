package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.rewards.RewardExecutor
import online.bingzi.bilibili.video.internal.rewards.TripleRewardService
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 奖励管理员子命令
 * 处理管理员的奖励配置管理操作
 */
@CommandHeader(
    name = "rewardadmin",
    description = "三连奖励系统管理命令",
    permission = "bilibilivideo.command.reward.admin"
)
object RewardAdminSubCommand {

    /**
     * 主管理命令 - 显示帮助信息
     */
    @CommandBody
    val main = subCommand {
        literal("admin")
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("rewardAdminCommandHelp")
        }
    }

    /**
     * 添加奖励配置
     * /bilibili reward admin add <UP主UID> <UP主名称> <奖励脚本>
     */
    @CommandBody
    val add = subCommand {
        literal("admin", "add")
        dynamic("uploader_uid") {
            suggestion<ProxyCommandSender> { _, _ ->
                listOf("123456789")  // 示例UID
            }
            dynamic("uploader_name") {
                suggestion<ProxyCommandSender> { _, _ ->
                    listOf("UP主名称")
                }
                dynamic("reward_script", optional = true) {
                    suggestion<ProxyCommandSender> { _, _ ->
                        listOf("tell player \"恭喜获得奖励！\"")
                    }
                    execute<ProxyCommandSender> { sender, context, argument ->
                        val uploaderUid = context.argument(-2).toLongOrNull()
                        val uploaderName = context.argument(-1)
                        val rewardScript = if (argument.isNotBlank()) argument else RewardExecutor.getDefaultRewardScript()

                        if (uploaderUid == null) {
                            sender.sendError("rewardAdminInvalidUid", context.argument(-2))
                            return@execute
                        }

                        if (uploaderName.isBlank()) {
                            sender.sendError("rewardAdminInvalidName")
                            return@execute
                        }

                        sender.sendInfo("rewardAdminAddProcessing", uploaderUid.toString())

                        TripleRewardService.addRewardConfig(uploaderUid, uploaderName, rewardScript).thenAccept { result ->
                            if (result.success) {
                                sender.sendInfo("rewardAdminConfigAdded", uploaderName, uploaderUid.toString())
                            } else {
                                sender.sendError("rewardAdminAddFailed", result.message)
                            }
                        }.exceptionally { throwable ->
                            sender.sendError("rewardAdminAddError", throwable.message ?: "未知错误")
                            null
                        }
                    }
                }
                execute<ProxyCommandSender> { sender, context, _ ->
                    val uploaderUid = context.argument(-1).toLongOrNull()
                    val uploaderName = context.argument(0)
                    val rewardScript = RewardExecutor.getDefaultRewardScript()

                    if (uploaderUid == null) {
                        sender.sendError("rewardAdminInvalidUid", context.argument(-1))
                        return@execute
                    }

                    if (uploaderName.isBlank()) {
                        sender.sendError("rewardAdminInvalidName")
                        return@execute
                    }

                    sender.sendInfo("rewardAdminAddProcessing", uploaderUid.toString())

                    TripleRewardService.addRewardConfig(uploaderUid, uploaderName, rewardScript).thenAccept { result ->
                        if (result.success) {
                            sender.sendInfo("rewardAdminConfigAdded", uploaderName, uploaderUid.toString())
                        } else {
                            sender.sendError("rewardAdminAddFailed", result.message)
                        }
                    }.exceptionally { throwable ->
                        sender.sendError("rewardAdminAddError", throwable.message ?: "未知错误")
                        null
                    }
                }
            }
        }
    }

    /**
     * 删除奖励配置
     * /bilibili reward admin remove <UP主UID>
     */
    @CommandBody
    val remove = subCommand {
        literal("admin", "remove")
        dynamic("uploader_uid") {
            suggestion<ProxyCommandSender> { _, _ ->
                // 这里可以从数据库获取已配置的UP主UID列表
                emptyList()
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                val uploaderUid = argument.toLongOrNull()

                if (uploaderUid == null) {
                    sender.sendError("rewardAdminInvalidUid", argument)
                    return@execute
                }

                sender.sendInfo("rewardAdminRemoveProcessing", uploaderUid.toString())

                TripleRewardService.removeRewardConfig(uploaderUid).thenAccept { result ->
                    if (result.success) {
                        sender.sendInfo("rewardAdminConfigRemoved", uploaderUid.toString())
                    } else {
                        sender.sendError("rewardAdminRemoveFailed", result.message)
                    }
                }.exceptionally { throwable ->
                    sender.sendError("rewardAdminRemoveError", throwable.message ?: "未知错误")
                    null
                }
            }
        }
    }

    /**
     * 查看所有奖励配置
     * /bilibili reward admin list
     */
    @CommandBody
    val list = subCommand {
        literal("admin", "list")
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("rewardAdminListLoading")

            TripleRewardService.getAllRewardConfigs().thenAccept { configs ->
                if (configs.isEmpty()) {
                    sender.sendInfo("rewardAdminListEmpty")
                } else {
                    sender.sendInfo("rewardAdminListHeader")
                    configs.forEachIndexed { index, config ->
                        val status = if (config.isEnabled) "&a启用" else "&c禁用"
                        sender.sendInfo("rewardAdminListItem", 
                            index + 1,
                            config.uploaderName,
                            config.uploaderUid,
                            status,
                            "${config.minVideoAgeDays}-${config.maxVideoAgeDays}天"
                        )
                    }
                    sender.sendInfo("rewardAdminListFooter", configs.size)
                }
            }.exceptionally { throwable ->
                sender.sendError("rewardAdminListError", throwable.message ?: "未知错误")
                null
            }
        }
    }

    /**
     * 启用/禁用奖励配置
     * /bilibili reward admin toggle <UP主UID> [true|false]
     */
    @CommandBody
    val toggle = subCommand {
        literal("admin", "toggle")
        dynamic("uploader_uid") {
            suggestion<ProxyCommandSender> { _, _ ->
                emptyList()
            }
            dynamic("enabled", optional = true) {
                suggestion<ProxyCommandSender> { _, _ ->
                    listOf("true", "false")
                }
                execute<ProxyCommandSender> { sender, context, argument ->
                    val uploaderUid = context.argument(-1).toLongOrNull()
                    val enabled = argument.toBooleanStrictOrNull()

                    if (uploaderUid == null) {
                        sender.sendError("rewardAdminInvalidUid", context.argument(-1))
                        return@execute
                    }

                    if (enabled == null) {
                        sender.sendError("rewardAdminInvalidBool", argument)
                        return@execute
                    }

                    val action = if (enabled) "启用" else "禁用"
                    sender.sendInfo("rewardAdminToggleProcessing", uploaderUid.toString(), action)

                    TripleRewardService.toggleRewardConfig(uploaderUid, enabled).thenAccept { result ->
                        if (result.success) {
                            sender.sendInfo("rewardAdminConfigToggled", uploaderUid.toString(), action)
                        } else {
                            sender.sendError("rewardAdminToggleFailed", result.message)
                        }
                    }.exceptionally { throwable ->
                        sender.sendError("rewardAdminToggleError", throwable.message ?: "未知错误")
                        null
                    }
                }
            }
            execute<ProxyCommandSender> { sender, _, argument ->
                val uploaderUid = argument.toLongOrNull()

                if (uploaderUid == null) {
                    sender.sendError("rewardAdminInvalidUid", argument)
                    return@execute
                }

                // 默认切换为启用
                sender.sendInfo("rewardAdminToggleProcessing", uploaderUid.toString(), "启用")

                TripleRewardService.toggleRewardConfig(uploaderUid, true).thenAccept { result ->
                    if (result.success) {
                        sender.sendInfo("rewardAdminConfigToggled", uploaderUid.toString(), "启用")
                    } else {
                        sender.sendError("rewardAdminToggleFailed", result.message)
                    }
                }.exceptionally { throwable ->
                    sender.sendError("rewardAdminToggleError", throwable.message ?: "未知错误")
                    null
                }
            }
        }
    }

    /**
     * 查看系统状态
     * /bilibili reward admin status
     */
    @CommandBody
    val status = subCommand {
        literal("admin", "status")
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("rewardAdminStatusLoading")

            TripleRewardService.getSystemStatus().thenAccept { status ->
                sender.sendInfo("rewardAdminStatusHeader")
                sender.sendInfo("rewardAdminStatusConfigs", 
                    status["enabledConfigs"] ?: 0,
                    status["totalConfigs"] ?: 0
                )
                sender.sendInfo("rewardAdminStatusRecentRewards", status["recentRewardCount"] ?: 0)
                sender.sendInfo("rewardAdminStatusSystemEnabled", 
                    if (status["systemEnabled"] as? Boolean == true) "&a启用" else "&c禁用"
                )
                
                if (status.containsKey("error")) {
                    sender.sendWarn("rewardAdminStatusError", status["error"] ?: "未知错误")
                }
            }.exceptionally { throwable ->
                sender.sendError("rewardAdminStatusError", throwable.message ?: "未知错误")
                null
            }
        }
    }

    /**
     * 获取脚本示例
     * /bilibili reward admin examples
     */
    @CommandBody
    val examples = subCommand {
        literal("admin", "examples")
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("rewardAdminExamplesHeader")
            
            val examples = RewardExecutor.getExampleRewardScripts()
            examples.forEach { (name, script) ->
                sender.sendInfo("rewardAdminExampleName", name)
                sender.sendInfo("rewardAdminExampleScript", script.replace("\n", "\\n"))
                sender.sendInfo("rewardAdminExampleSeparator")
            }
        }
    }
}