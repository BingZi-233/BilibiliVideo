package online.bingzi.bilibili.video.internal.ui

import online.bingzi.bilibili.video.internal.nms.NMSPacketHandler
import org.bukkit.entity.Player
import org.bukkit.map.MapView
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
        // 1. 创建 MapView 和地图物品（复用现有 QrMapService 逻辑）
        val mapItem = QrMapService.createQrMapItem(player, qrUrl)
        val mapView = extractMapView(mapItem, player)

        // 2. 保存会话（仅需保存 MapView 以便后续清理）
        sessions[player.uniqueId] = Session(mapView)

        // 3. 计算主手槽位编号并发包
        // Minecraft 背包槽位：36-44 为快捷栏，主手 = 36 + heldItemSlot
        val slot = 36 + player.inventory.heldItemSlot
        NMSPacketHandler.instance.sendSlotItem(player, slot, mapItem)
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
     * 从地图物品中提取 MapView。
     *
     * 由于 QrMapService.createQrMapItem 内部创建了 MapView，
     * 这里需要从 ItemMeta 中提取出来以便后续清理。
     */
    private fun extractMapView(mapItem: org.bukkit.inventory.ItemStack, player: Player): MapView {
        val meta = mapItem.itemMeta
        if (meta is org.bukkit.inventory.meta.MapMeta) {
            // 1.13+ 可以直接获取 MapView
            try {
                val mapView = meta.mapView
                if (mapView != null) {
                    return mapView
                }
            } catch (_: Throwable) {
                // 旧版本可能没有 getMapView 方法
            }

            // 尝试通过 mapId 获取
            try {
                val mapId = meta.mapId
                val mapView = org.bukkit.Bukkit.getMap(mapId)
                if (mapView != null) {
                    return mapView
                }
            } catch (_: Throwable) {
                // 忽略
            }
        }

        // 兜底：1.12 及以下通过耐久度获取地图 ID
        try {
            if (mapItem.type.name == "MAP") {
                @Suppress("DEPRECATION")
                val mapId = mapItem.durability.toInt()
                val mapView = org.bukkit.Bukkit.getMap(mapId)
                if (mapView != null) {
                    return mapView
                }
            }
        } catch (_: Throwable) {
            // 忽略
        }

        // 最后兜底：创建一个新的空 MapView（不应该走到这里）
        return org.bukkit.Bukkit.createMap(player.world)
    }
}
