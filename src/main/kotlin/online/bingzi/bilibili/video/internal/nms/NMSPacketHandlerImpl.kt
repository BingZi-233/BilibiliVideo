package online.bingzi.bilibili.video.internal.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket

/**
 * NMS 发包实现层。
 *
 * 处理 1.12 - 最新版本的 PacketPlayOutSetSlot 差异：
 * - 1.12 - 1.16.5：PacketPlayOutSetSlot(windowId, slot, nmsItem)
 * - 1.17+：PacketPlayOutSetSlot(windowId, stateId, slot, nmsItem)
 */
@Suppress("unused")
class NMSPacketHandlerImpl : NMSPacketHandler() {

    override fun sendSlotItem(player: Player, slot: Int, item: ItemStack) {
        val nmsItem = asNMSCopy(item)
        val packet = createSetSlotPacket(windowId = 0, slot = slot, nmsItem = nmsItem)
        player.sendPacket(packet)
    }

    override fun refreshInventory(player: Player) {
        @Suppress("DEPRECATION")
        player.updateInventory()
    }

    /**
     * 将 Bukkit ItemStack 转换为 NMS ItemStack。
     */
    private fun asNMSCopy(item: ItemStack): Any {
        val craftItemStackClass = obcClass("inventory.CraftItemStack")
        val method = craftItemStackClass.getMethod("asNMSCopy", ItemStack::class.java)
        return method.invoke(null, item)
    }

    /**
     * 创建 PacketPlayOutSetSlot 包。
     *
     * windowId = 0 表示玩家背包窗口。
     */
    private fun createSetSlotPacket(windowId: Int, slot: Int, nmsItem: Any): Any {
        val packetClass = nmsClass("PacketPlayOutSetSlot")

        return if (MinecraftVersion.isUniversal) {
            // 1.17+：构造函数为 (int windowId, int stateId, int slot, ItemStack)
            // stateId 用于同步状态，传 0 即可
            val constructor = packetClass.getConstructor(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                nmsClass("ItemStack")
            )
            constructor.newInstance(windowId, 0, slot, nmsItem)
        } else {
            // 1.12 - 1.16.5：构造函数为 (int windowId, int slot, ItemStack)
            val constructor = packetClass.getConstructor(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                nmsClass("ItemStack")
            )
            constructor.newInstance(windowId, slot, nmsItem)
        }
    }

    /**
     * 获取 NMS 类。
     *
     * 1.17+ 使用 net.minecraft.network.protocol.game 等包名。
     * 1.16.5 及以下使用 net.minecraft.server.v1_XX_RX 包名。
     */
    private fun nmsClass(name: String): Class<*> {
        return if (MinecraftVersion.isUniversal) {
            // 1.17+ 类路径映射
            when (name) {
                "PacketPlayOutSetSlot" -> Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSetSlot")
                "ItemStack" -> Class.forName("net.minecraft.world.item.ItemStack")
                else -> throw IllegalArgumentException("Unknown NMS class: $name")
            }
        } else {
            // 1.12 - 1.16.5
            val version = MinecraftVersion.minecraftVersion
            Class.forName("net.minecraft.server.$version.$name")
        }
    }

    /**
     * 获取 OBC (org.bukkit.craftbukkit) 类。
     */
    private fun obcClass(name: String): Class<*> {
        val version = MinecraftVersion.minecraftVersion
        return Class.forName("org.bukkit.craftbukkit.$version.$name")
    }
}
