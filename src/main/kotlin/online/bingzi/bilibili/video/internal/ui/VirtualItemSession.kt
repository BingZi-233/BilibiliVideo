package online.bingzi.bilibili.video.internal.ui

import online.bingzi.bilibili.video.internal.nms.NMSPacketHandler
import online.bingzi.bilibili.video.internal.util.QrCodeGenerator
import org.bukkit.entity.Player
import org.bukkit.map.MapPalette
import org.bukkit.map.MapView
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 虚拟物品会话管理。
 *
 * 负责：
 * - 发送虚拟二维码地图物品到玩家主手槽位
 * - 保存玩家原主手物品和创建的 MapView
 * - 在扫码结束后刷新背包恢复原物品并清理 MapView
 */
object VirtualItemSession {

    private data class Session(
        val mapView: MapView
    )

    private val sessions = ConcurrentHashMap<UUID, Session>()

    /**
     * 发送虚拟二维码物品到玩家主手槽位。
     *
     * @param player 目标玩家
     * @param qrUrl 二维码 URL
     */
    fun sendVirtualItem(player: Player, qrUrl: String) {
        // 1. 生成二维码图像
        val qrImage = QrCodeGenerator.generateQrImage(qrUrl, 128)

        // 2. 创建 MapView 和地图物品（直接返回配对的结果，避免 ID 不匹配）
        val (mapItem, mapView) = QrMapService.createQrMapItem(player, qrUrl)

        // 3. 保存会话
        sessions[player.uniqueId] = Session(mapView)

        // 4. 发送地图物品到主手槽位
        val slot = 36 + player.inventory.heldItemSlot
        NMSPacketHandler.instance.sendSlotItem(player, slot, mapItem)

        // 5. 将二维码图像转换为 MapPalette 颜色数组并发送地图数据
        val colors = imageToMapColors(qrImage)
        NMSPacketHandler.instance.sendMapData(player, mapView, colors)
    }

    /**
     * 恢复玩家原物品（刷新背包使虚拟物品消失）。
     *
     * @param player 目标玩家
     */
    fun restoreItem(player: Player) {
        val session = sessions.remove(player.uniqueId) ?: return

        // 1. 清理 MapView 渲染器（避免内存泄漏）
        cleanupMapView(session.mapView)

        // 2. 刷新背包（恢复真实物品）
        NMSPacketHandler.instance.refreshInventory(player)
    }

    /**
     * 清理离线玩家数据。
     *
     * @param playerUuid 玩家 UUID
     */
    fun cleanup(playerUuid: UUID) {
        val session = sessions.remove(playerUuid) ?: return
        cleanupMapView(session.mapView)
    }

    /**
     * 检查玩家是否有活跃的虚拟物品会话。
     */
    fun hasSession(playerUuid: UUID): Boolean {
        return sessions.containsKey(playerUuid)
    }

    /**
     * 清理 MapView 的渲染器。
     */
    private fun cleanupMapView(mapView: MapView) {
        try {
            mapView.renderers.toList().forEach { renderer ->
                mapView.removeRenderer(renderer)
            }
        } catch (_: Throwable) {
            // 忽略清理失败
        }
    }

    /**
     * 将 BufferedImage 转换为 MapPalette 颜色字节数组。
     *
     * 地图数据格式：128x128 字节数组，每个字节是 MapPalette 颜色索引。
     * 重要：Minecraft 地图数据是**列优先**排列：colors[x * 128 + y]
     * （从左上角开始，一列一列地写入）
     *
     * 注意：不使用 MapPalette.matchColor，因为在某些版本上可能返回不正确的索引。
     * 二维码只需要黑白两色，使用简单的灰度判断更可靠。
     */
    @Suppress("DEPRECATION")
    private fun imageToMapColors(image: java.awt.image.BufferedImage): ByteArray {
        val colors = ByteArray(128 * 128)
        val width = minOf(128, image.width)
        val height = minOf(128, image.height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val rgb = Color(image.getRGB(x, y))
                val gray = (rgb.red + rgb.green + rgb.blue) / 3
                // 使用 MapPalette 预定义常量，比 matchColor 更可靠
                // 注意：Minecraft 地图数据是列优先排列 (column-major)
                colors[x * 128 + y] = if (gray < 128) MapPalette.DARK_GRAY else MapPalette.WHITE
            }
        }
        return colors
    }
}
