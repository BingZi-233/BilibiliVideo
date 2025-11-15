package online.bingzi.bilibili.video.internal.ui

import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapPalette
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.min

/**
 * 用于在地图上渲染二维码图片的渲染器。
 *
 * 只持有二维码的 BufferedImage，不持有 Player 等 Bukkit 对象，避免内存泄漏。
 */
class QrMapRenderer(
    private val image: BufferedImage
) : MapRenderer() {

    private var rendered = false

    override fun render(map: MapView, canvas: MapCanvas, player: Player) {
        if (rendered) {
            return
        }
        rendered = true

        val width = min(128, image.width)
        val height = min(128, image.height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val rgb = Color(image.getRGB(x, y))
                val gray = (rgb.red + rgb.green + rgb.blue) / 3
                val mapColor = if (gray < 128) MapPalette.DARK_GRAY else MapPalette.WHITE
                canvas.setPixel(x, y, mapColor)
            }
        }
    }
}
