package online.bingzi.bilibili.video.commands

import online.bingzi.bilibili.video.internal.qrcode.QRCodeGenerator
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendMode
import online.bingzi.bilibili.video.internal.qrcode.QRCodeSendService
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.module.lang.sendError
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn

/**
 * 测试二维码发送功能的命令
 */
@CommandHeader(
    name = "testqr",
    description = "测试二维码发送功能",
    permission = "bilibilibideo.test"
)
object TestQRCodeCommand {

    @CommandBody
    val main = mainCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            sender.sendInfo("testQrUsage")
        }
    }

    @CommandBody
    val chat = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            testQRCodeSend(sender, QRCodeSendMode.CHAT, "聊天框")
        }
    }

    @CommandBody
    val map = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            testQRCodeSend(sender, QRCodeSendMode.MAP, "地图")
        }
    }

    @CommandBody
    val onebot = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            testQRCodeSend(sender, QRCodeSendMode.ONEBOT, "OneBot")
        }
    }

    @CommandBody
    val modes = subCommand {
        execute<ProxyPlayer> { sender, context, argument ->
            val availableModes = QRCodeSendService.getAvailableModes(sender)
            if (availableModes.isEmpty()) {
                sender.sendWarn("testQrNoAvailableModes")
            } else {
                val modeNames = availableModes.joinToString(", ") { it.displayName }
                sender.sendInfo("testQrAvailableModes", modeNames)
            }
        }
    }

    @CommandBody
    val custom = subCommand {
        dynamic(comment = "自定义文本") {
            execute<ProxyPlayer> { sender, context, argument ->
                val customText = context["custom"].toString()
                testCustomQRCode(sender, customText)
            }
        }
    }

    /**
     * 测试指定模式的二维码发送
     */
    private fun testQRCodeSend(player: ProxyPlayer, mode: QRCodeSendMode, modeName: String) {
        if (!QRCodeSendService.isModeAvailable(mode, player)) {
            player.sendWarn("testQrModeUnavailable", modeName)
            return
        }

        player.sendInfo("testQrGenerating", modeName)

        try {
            // 生成测试用二维码
            val testUrl = "https://www.bilibili.com"
            val qrSize = QRCodeGenerator.getRecommendedSize(testUrl.length)
            val qrImage = QRCodeGenerator.generateQRCode(testUrl, qrSize)

            if (qrImage == null) {
                player.sendError("testQrGenerateFailed")
                return
            }

            // 发送二维码
            QRCodeSendService.sendQRCode(
                player = player,
                qrCodeImage = qrImage,
                title = "测试二维码",
                description = "这是一个测试用的二维码，指向 bilibili.com",
                preferredMode = mode
            ).thenAccept { success ->
                if (success) {
                    player.sendInfo("testQrSendSuccess", modeName)
                } else {
                    player.sendError("testQrSendFailed", modeName)
                }
            }

        } catch (e: Exception) {
            player.sendError("testQrException", e.message ?: "未知错误")
        }
    }

    /**
     * 测试自定义文本的二维码
     */
    private fun testCustomQRCode(player: ProxyPlayer, text: String) {
        if (text.length > 500) {
            player.sendWarn("testQrTextTooLong", "500")
            return
        }

        player.sendInfo("testQrGeneratingCustom", text)

        try {
            val qrSize = QRCodeGenerator.getRecommendedSize(text.length)
            val qrImage = QRCodeGenerator.generateQRCode(text, qrSize)

            if (qrImage == null) {
                player.sendError("testQrGenerateFailed")
                return
            }

            // 使用最佳可用模式
            val availableModes = QRCodeSendService.getAvailableModes(player)
            if (availableModes.isEmpty()) {
                player.sendWarn("testQrNoAvailableModes")
                return
            }

            val preferredMode = availableModes.first()
            QRCodeSendService.sendQRCode(
                player = player,
                qrCodeImage = qrImage,
                title = "自定义二维码",
                description = "自定义内容: ${text.take(50)}${if (text.length > 50) "..." else ""}",
                preferredMode = preferredMode
            ).thenAccept { success ->
                if (success) {
                    player.sendInfo("testQrCustomSendSuccess", preferredMode.displayName)
                } else {
                    player.sendError("testQrCustomSendFailed")
                }
            }

        } catch (e: Exception) {
            player.sendError("testQrException", e.message ?: "未知错误")
        }
    }
}