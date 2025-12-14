package online.bingzi.bilibili.video.internal.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
import taboolib.module.nms.nmsProxy

/**
 * NMS 发包抽象层。
 *
 * 提供跨版本（1.12 - 最新）的物品槽位发包能力，用于发送虚拟物品。
 */
abstract class NMSPacketHandler {

    /**
     * 发送虚拟物品到指定槽位。
     *
     * @param player 目标玩家
     * @param slot 槽位编号（玩家背包窗口 ID=0 的槽位，主手 = 36 + heldItemSlot）
     * @param item 要显示的物品
     */
    abstract fun sendSlotItem(player: Player, slot: Int, item: ItemStack)

    /**
     * 刷新玩家整个背包，使虚拟物品消失，恢复真实物品。
     */
    abstract fun refreshInventory(player: Player)

    /**
     * 发送地图数据到玩家客户端。
     *
     * @param player 目标玩家
     * @param mapView 地图视图
     * @param colors 128x128 地图颜色数据（MapPalette 颜色字节数组）
     */
    abstract fun sendMapData(player: Player, mapView: MapView, colors: ByteArray)

    companion object {
        val instance by lazy { nmsProxy<NMSPacketHandler>() }
    }
}
