package online.bingzi.bilibili.video.internal.commands

import online.bingzi.bilibili.video.internal.qrcode.QRCodeGeneratorManager
import taboolib.common.platform.ProxyCommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.expansion.createHelper

/**
 * 二维码测试命令
 * 用于验证优化后的二维码生成器功能
 * 
 * @author BingZi-233
 * @since 1.0.0
 */
@CommandHeader("qrcodetest", aliases = ["qrtest"], permission = "bilibilivideo.admin")
object QRCodeTestCommand {
    
    @CommandBody
    val main = mainCommand {
        createHelper()
    }
    
    @CommandBody
    val status = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("正在获取二维码生成器状态...")
            
            val statusReport = QRCodeGeneratorManager.getDetailedStatus()
            statusReport.forEach { line ->
                sender.sendMessage(line)
            }
        }
    }
    
    @CommandBody
    val validate = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("正在执行配置验证...")
            
            val validationResults = QRCodeGeneratorManager.validateConfiguration()
            validationResults.forEach { result ->
                sender.sendMessage(result)
            }
        }
    }
    
    @CommandBody
    val benchmark = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("正在执行性能基准测试...")
            
            val benchmarkResults = QRCodeGeneratorManager.getPerformanceBenchmark()
            benchmarkResults.forEach { result ->
                sender.sendMessage(result)
            }
        }
    }
    
    @CommandBody
    val cleanup = subCommand {
        execute<ProxyCommandSender> { sender, _, _ ->
            sender.sendMessage("正在执行缓存清理...")
            
            val cleanupResult = QRCodeGeneratorManager.performCacheCleanup()
            sender.sendMessage(cleanupResult)
        }
    }
}