package online.bingzi.bilibili.video.internal.command.actions

import online.bingzi.bilibili.video.internal.helper.sendMap
import online.bingzi.bilibili.video.internal.helper.toBufferedImage
import org.bukkit.Bukkit
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.submit
import taboolib.library.kether.ArgTypes
import taboolib.module.chat.colored

object VideoAction {
    fun execute(sender: ProxyPlayer, bv: String) {
        sender.sendMap("https://www.bilibili.com/video/$bv/".toBufferedImage(128)) {
            name = "&a&lBilibili传送门".colored()
            shiny()
            lore.clear()
            lore.addAll(
                ArgTypes.listOf(
                    "&7请使用Bilibili客户端扫描二维码"
                ).colored()
            )
        }
        submit(async = true, delay = 20 * 60 * 3) {
            Bukkit.getPlayer(sender.uniqueId)?.updateInventory()
        }
    }
} 