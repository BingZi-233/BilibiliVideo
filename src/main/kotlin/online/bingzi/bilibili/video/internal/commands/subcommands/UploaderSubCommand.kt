package online.bingzi.bilibili.video.internal.commands.subcommands

import online.bingzi.bilibili.video.internal.cache.CommandSuggestionService
import online.bingzi.bilibili.video.internal.database.dao.UploaderVideoDaoService
import online.bingzi.bilibili.video.internal.scheduler.UploaderVideoScheduler
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.function.submit
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * UP主监控相关子命令
 * 处理 UP主视频监控功能
 */
object UploaderSubCommand {
    
    /**
     * UP主监控主命令 - /bilibilivideo uploader
     */
    val main = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendInfo("uploaderCommandHelp")
        }
    }
    
    /**
     * 添加UP主监控 - /bilibilivideo uploader add <uid> [interval]
     */
    val add = subCommand {
        dynamic("uid") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取热门UP主UID作为建议
                try {
                    CommandSuggestionService.getUploaderUidSuggestions(10).get()
                } catch (e: Exception) {
                    // 降级处理：提供知名UP主示例
                    listOf("703007996", "546195", "37663924", "请输入UP主UID")
                }
            }
            
            dynamic("interval", optional = true) {
                suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                    listOf("24", "12", "6", "48")
                }
                
                execute<ProxyCommandSender> { sender, context, argument ->
                    val uid = context["uid"].toLongOrNull()
                    val interval = argument.toIntOrNull() ?: 24
                    
                    if (uid == null || uid <= 0) {
                        sender.sendError("uploaderInvalidUid", context["uid"])
                        return@execute
                    }
                    
                    if (interval < 1 || interval > 168) {
                        sender.sendError("uploaderInvalidInterval", argument)
                        return@execute
                    }
                    
                    submit(async = true) {
                        if (UploaderVideoScheduler.addUploader(uid, interval)) {
                            sender.sendInfo("uploaderAddSuccess", uid.toString(), interval.toString())
                        } else {
                            sender.sendError("uploaderAddFailed", uid.toString())
                        }
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, context, _ ->
                val uid = context["uid"].toLongOrNull()
                
                if (uid == null || uid <= 0) {
                    sender.sendError("uploaderInvalidUid", context["uid"])
                    return@execute
                }
                
                submit(async = true) {
                    if (UploaderVideoScheduler.addUploader(uid)) {
                        sender.sendInfo("uploaderAddSuccess", uid.toString(), "24")
                    } else {
                        sender.sendError("uploaderAddFailed", uid.toString())
                    }
                }
            }
        }
    }
    
    /**
     * 移除UP主监控 - /bilibilivideo uploader remove <uid>
     */
    val remove = subCommand {
        dynamic("uid") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取所有已监控的UP主UID作为建议
                try {
                    CommandSuggestionService.getMonitoredUploaderSuggestions(10).get()
                } catch (e: Exception) {
                    // 降级处理：从数据库直接获取
                    try {
                        UploaderVideoDaoService.getAllConfigs().get()
                            .map { "${it.uploaderUid} (${it.uploaderName})" }
                    } catch (ex: Exception) {
                        listOf("请输入已监控的UP主UID")
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val uid = argument.toLongOrNull()
                
                if (uid == null || uid <= 0) {
                    sender.sendError("uploaderInvalidUid", argument)
                    return@execute
                }
                
                submit(async = true) {
                    if (UploaderVideoScheduler.removeUploader(uid)) {
                        sender.sendInfo("uploaderRemoveSuccess", uid.toString())
                    } else {
                        sender.sendError("uploaderRemoveFailed", uid.toString())
                    }
                }
            }
        }
    }
    
    /**
     * 列出所有监控的UP主 - /bilibilivideo uploader list
     */
    val list = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            submit(async = true) {
                val configs = UploaderVideoDaoService.getAllConfigs().get()
                
                if (configs.isEmpty()) {
                    sender.sendInfo("uploaderListEmpty")
                    return@submit
                }
                
                sender.sendInfo("uploaderListHeader", configs.size.toString())
                
                configs.forEach { config ->
                    val videoCount = UploaderVideoDaoService.getVideoCount(config.uploaderUid).get()
                    val status = if (config.isEnabled) {
                        if (UploaderVideoScheduler.isSyncing(config.uploaderUid)) {
                            "同步中"
                        } else if (config.needsSync()) {
                            "待同步"
                        } else {
                            "已同步"
                        }
                    } else {
                        "已禁用"
                    }
                    
                    sender.sendInfo(
                        "uploaderListItem",
                        config.uploaderUid.toString(),
                        config.uploaderName,
                        videoCount.toString(),
                        config.syncIntervalHours.toString(),
                        status
                    )
                }
            }
        }
    }
    
    /**
     * 手动同步UP主 - /bilibilivideo uploader sync <uid>
     */
    val sync = subCommand {
        dynamic("uid") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取所有已监控的UP主UID作为建议
                try {
                    CommandSuggestionService.getMonitoredUploaderSuggestions(10).get()
                } catch (e: Exception) {
                    // 降级处理：从数据库直接获取
                    try {
                        UploaderVideoDaoService.getAllConfigs().get()
                            .map { "${it.uploaderUid} (${it.uploaderName})" }
                    } catch (ex: Exception) {
                        listOf("请输入已监控的UP主UID")
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val uid = argument.toLongOrNull()
                
                if (uid == null || uid <= 0) {
                    sender.sendError("uploaderInvalidUid", argument)
                    return@execute
                }
                
                if (UploaderVideoScheduler.isSyncing(uid)) {
                    sender.sendWarn("uploaderAlreadySyncing", uid.toString())
                    return@execute
                }
                
                submit(async = true) {
                    sender.sendInfo("uploaderSyncStartManual", uid.toString())
                    
                    if (UploaderVideoScheduler.syncUploaderManual(uid)) {
                        sender.sendInfo("uploaderSyncQueued", uid.toString())
                    } else {
                        sender.sendError("uploaderSyncFailed", uid.toString())
                    }
                }
            }
        }
    }
    
    /**
     * 同步所有UP主 - /bilibilivideo uploader syncall
     */
    val syncAll = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            submit(async = true) {
                val configs = UploaderVideoDaoService.getEnabledConfigs().get()
                
                if (configs.isEmpty()) {
                    sender.sendInfo("uploaderNoEnabledConfigs")
                    return@submit
                }
                
                sender.sendInfo("uploaderSyncAllStart", configs.size.toString())
                
                configs.forEach { config ->
                    if (!UploaderVideoScheduler.isSyncing(config.uploaderUid)) {
                        UploaderVideoScheduler.syncUploader(config)
                        sender.sendInfo("uploaderSyncQueued", config.uploaderName)
                        Thread.sleep(1000) // 避免同时发起太多请求
                    }
                }
                
                sender.sendInfo("uploaderSyncAllQueued")
            }
        }
    }
    
    /**
     * 启用/禁用UP主监控 - /bilibilivideo uploader toggle <uid>
     */
    val toggle = subCommand {
        dynamic("uid") {
            suggestion<ProxyCommandSender>(uncheck = true) { _, _ ->
                // 获取所有已监控的UP主UID作为建议
                try {
                    CommandSuggestionService.getMonitoredUploaderSuggestions(10).get()
                } catch (e: Exception) {
                    // 降级处理：从数据库直接获取
                    try {
                        UploaderVideoDaoService.getAllConfigs().get()
                            .map { "${it.uploaderUid} (${it.uploaderName})" }
                    } catch (ex: Exception) {
                        listOf("请输入已监控的UP主UID")
                    }
                }
            }
            
            execute<ProxyCommandSender> { sender, _, argument ->
                val uid = argument.toLongOrNull()
                
                if (uid == null || uid <= 0) {
                    sender.sendError("uploaderInvalidUid", argument)
                    return@execute
                }
                
                submit(async = true) {
                    val configs = UploaderVideoDaoService.getAllConfigs().get()
                    val config = configs.find { it.uploaderUid == uid }
                    
                    if (config == null) {
                        sender.sendError("uploaderNotFound", uid.toString())
                        return@submit
                    }
                    
                    config.isEnabled = !config.isEnabled
                    
                    if (UploaderVideoDaoService.saveConfig(config).get()) {
                        if (config.isEnabled) {
                            sender.sendInfo("uploaderEnabled", config.uploaderName)
                        } else {
                            sender.sendInfo("uploaderDisabled", config.uploaderName)
                        }
                    } else {
                        sender.sendError("uploaderToggleFailed", config.uploaderName)
                    }
                }
            }
        }
    }
    
    /**
     * 查看同步状态 - /bilibilivideo uploader status
     */
    val status = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            val status = UploaderVideoScheduler.getSyncStatus()
            
            sender.sendInfo(
                "uploaderSchedulerStatus",
                if (status["isRunning"] as Boolean) "运行中" else "已停止",
                (status["syncingUploaders"] as List<*>).size.toString(),
                status["checkIntervalMinutes"].toString()
            )
            
            val syncingList = status["syncingUploaders"] as List<*>
            if (syncingList.isNotEmpty()) {
                sender.sendInfo("uploaderSyncingList", syncingList.joinToString(", "))
            }
        }
    }
    
    /**
     * 搜索视频 - /bilibilivideo uploader search <keyword>
     */
    val search = subCommand {
        dynamic("keyword") {
            execute<ProxyCommandSender> { sender, _, argument ->
                submit(async = true) {
                    val videos = UploaderVideoDaoService.searchVideos(argument).get()
                    
                    if (videos.isEmpty()) {
                        sender.sendInfo("uploaderSearchNoResult", argument)
                        return@submit
                    }
                    
                    sender.sendInfo("uploaderSearchResult", videos.size.toString(), argument)
                    
                    videos.take(10).forEach { video ->
                        sender.sendInfo(
                            "uploaderSearchItem",
                            video.bvId,
                            video.title,
                            video.uploaderName
                        )
                    }
                    
                    if (videos.size > 10) {
                        sender.sendInfo("uploaderSearchMore", (videos.size - 10).toString())
                    }
                }
            }
        }
    }
}