package online.bingzi.bilibili.video.internal.nms

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
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

    override fun sendMapData(player: Player, mapView: MapView, colors: ByteArray) {
        val mapId = resolveMapId(mapView)
        val packet = createMapPacket(mapId, colors)
        player.sendPacket(packet)
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
     *
     * Paper 1.17+ 使用 Mojang 映射，类名与 Spigot 不同：
     * - PacketPlayOutSetSlot → ClientboundContainerSetSlotPacket
     * - PacketPlayOutMap → ClientboundMapItemDataPacket
     */
    private fun nmsClass(name: String): Class<*> {
        return if (MinecraftVersion.isUniversal) {
            // 1.17+ 类路径映射（同时支持 Spigot 和 Paper/Mojang 映射）
            when (name) {
                "PacketPlayOutSetSlot" -> findClass(
                    "net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket",  // Paper (Mojang mappings)
                    "net.minecraft.network.protocol.game.PacketPlayOutSetSlot"                // Spigot
                )
                "PacketPlayOutMap" -> findClass(
                    "net.minecraft.network.protocol.game.ClientboundMapItemDataPacket",       // Paper (Mojang mappings)
                    "net.minecraft.network.protocol.game.PacketPlayOutMap"                    // Spigot
                )
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
     * 尝试按顺序查找类，返回第一个找到的。
     */
    private fun findClass(vararg classNames: String): Class<*> {
        for (className in classNames) {
            try {
                return Class.forName(className)
            } catch (_: ClassNotFoundException) {
                // 继续尝试下一个
            }
        }
        throw ClassNotFoundException("None of the classes found: ${classNames.joinToString()}")
    }

    /**
     * 获取 OBC (org.bukkit.craftbukkit) 类。
     *
     * 1.20.5+ / Folia：org.bukkit.craftbukkit.xxx（无版本字符串）
     * 1.20.4 及以下：org.bukkit.craftbukkit.v1_XX_RX.xxx
     */
    private fun obcClass(name: String): Class<*> {
        val version = MinecraftVersion.minecraftVersion
        // 1.20.5+ 和 Folia 服务器返回 "UNKNOWN"，此时 CraftBukkit 不再使用版本化包路径
        return if (version == "UNKNOWN") {
            Class.forName("org.bukkit.craftbukkit.$name")
        } else {
            Class.forName("org.bukkit.craftbukkit.$version.$name")
        }
    }

    /**
     * 从 MapView 获取地图 ID。
     */
    private fun resolveMapId(mapView: MapView): Int {
        return try {
            val method = mapView.javaClass.getMethod("getId")
            val result = method.invoke(mapView)
            when (result) {
                is Number -> result.toInt()
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * 创建 PacketPlayOutMap 包。
     *
     * 版本差异：
     * - 1.17+：PacketPlayOutMap(MapId, scale, locked, icons, MapPatch)
     * - 1.12-1.16.5：PacketPlayOutMap(mapId, scale, trackingPosition, locked, icons, startX, startY, width, height, data)
     */
    private fun createMapPacket(mapId: Int, colors: ByteArray): Any {
        return if (MinecraftVersion.isUniversal) {
            createMapPacket117Plus(mapId, colors)
        } else {
            createMapPacketLegacy(mapId, colors)
        }
    }

    /**
     * 1.17+ 地图数据包构建。
     *
     * Paper (Mojang mappings) 与 Spigot 类名对照：
     * - MapId: net.minecraft.world.level.saveddata.maps.MapId
     * - MapPatch: MapItemSavedData$MapPatch (Paper) / WorldMap$b (Spigot)
     */
    private fun createMapPacket117Plus(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")
        val mapIdClass = Class.forName("net.minecraft.world.level.saveddata.maps.MapId")

        // 创建 MapId 对象
        val mapIdConstructor = mapIdClass.getConstructor(Int::class.javaPrimitiveType)
        val mapIdObj = mapIdConstructor.newInstance(mapId)

        // 创建 MapPatch（更新区域）
        // Paper (Mojang): MapItemSavedData$MapPatch
        // Spigot: WorldMap$b
        val mapPatchClass = findClass(
            "net.minecraft.world.level.saveddata.maps.MapItemSavedData\$MapPatch",  // Paper (Mojang mappings)
            "net.minecraft.world.level.saveddata.maps.WorldMap\$b"                   // Spigot
        )
        val mapPatchConstructor = mapPatchClass.getConstructor(
            Int::class.javaPrimitiveType,  // startX
            Int::class.javaPrimitiveType,  // startY
            Int::class.javaPrimitiveType,  // width
            Int::class.javaPrimitiveType,  // height
            ByteArray::class.java          // colors
        )
        val mapPatch = mapPatchConstructor.newInstance(0, 0, 128, 128, colors)

        // 构建数据包
        // PacketPlayOutMap(MapId, byte scale, boolean locked, Optional<List<MapDecoration>> icons, Optional<MapPatch> updateData)
        val constructor = packetClass.getConstructor(
            mapIdClass,
            Byte::class.javaPrimitiveType,
            Boolean::class.javaPrimitiveType,
            java.util.Optional::class.java,
            java.util.Optional::class.java
        )
        return constructor.newInstance(
            mapIdObj,
            0.toByte(),  // scale
            false,       // locked
            java.util.Optional.empty<Any>(),  // no icons
            java.util.Optional.of(mapPatch)   // map data
        )
    }

    /**
     * 1.12-1.16.5 地图数据包构建（启发式自动兼容）。
     *
     * 使用纯启发式方法自动适配不同服务端版本的构造函数签名差异，
     * 无需针对特定版本硬编码。
     *
     * 启发式规则：
     * - int: 按顺序分配 [mapId, startX(0), startY(0), width(128), height(128)]
     * - byte: scale = 0
     * - boolean: trackingPosition/locked = false
     * - Collection: icons = emptyList
     * - byte[]: colors 数据
     */
    private fun createMapPacketLegacy(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")

        // 获取所有公共构造函数，优先尝试参数数量多的（通常是功能更完整的版本）
        val constructors = packetClass.constructors
            .filter { it.parameterCount >= 8 }  // 地图包至少需要 8 个参数
            .sortedByDescending { it.parameterCount }

        if (constructors.isEmpty()) {
            val allConstructors = packetClass.constructors.sortedByDescending { it.parameterCount }
            throw IllegalStateException(
                "No PacketPlayOutMap constructor with >= 8 params for ${MinecraftVersion.minecraftVersion}.\n" +
                "All constructors: ${allConstructors.map { c -> c.parameterTypes.map { it.simpleName } }}"
            )
        }

        val errors = mutableListOf<String>()

        for (constructor in constructors) {
            try {
                val args = buildArgsHeuristically(constructor.parameterTypes, mapId, colors)
                if (args != null) {
                    return constructor.newInstance(*args)
                } else {
                    errors.add("${constructor.parameterTypes.map { it.simpleName }}: unsupported param type")
                }
            } catch (e: Exception) {
                errors.add("${constructor.parameterTypes.map { it.simpleName }}: ${e.message}")
            }
        }

        throw IllegalStateException(
            "No suitable PacketPlayOutMap constructor found for ${MinecraftVersion.minecraftVersion}.\n" +
            "Available: ${constructors.map { it.parameterTypes.map { p -> p.simpleName } }}\n" +
            "Errors: $errors"
        )
    }

    /**
     * 启发式构建参数数组。
     *
     * 根据参数类型自动推断应该填充的值，无需硬编码特定版本的签名。
     * 这种方法可以自动兼容各种服务端 fork 的签名差异。
     */
    private fun buildArgsHeuristically(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        val args = mutableListOf<Any>()

        // int 类型参数的值队列：mapId 必须是第一个，后面是坐标和尺寸
        val intValues = mutableListOf(mapId, 0, 0, 128, 128)

        for (paramType in paramTypes) {
            val arg = when {
                // int/Integer: 按顺序分配 mapId → startX → startY → width → height
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> {
                    if (intValues.isNotEmpty()) intValues.removeAt(0) else 0
                }

                // byte/Byte: scale（地图缩放级别，0 = 1:1）
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> {
                    0.toByte()
                }

                // boolean/Boolean: trackingPosition 或 locked，都设为 false
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> {
                    false
                }

                // Collection: 地图图标列表，传空
                Collection::class.java.isAssignableFrom(paramType) -> {
                    emptyList<Any>()
                }

                // byte[]: 地图颜色数据
                paramType == ByteArray::class.java -> {
                    colors
                }

                // 未知类型：无法处理此构造函数
                else -> return null
            }
            args.add(arg)
        }

        return args.toTypedArray()
    }
}
