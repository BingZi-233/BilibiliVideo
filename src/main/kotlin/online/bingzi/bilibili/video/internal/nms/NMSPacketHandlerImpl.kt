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
     */
    private fun nmsClass(name: String): Class<*> {
        return if (MinecraftVersion.isUniversal) {
            // 1.17+ 类路径映射
            when (name) {
                "PacketPlayOutSetSlot" -> Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSetSlot")
                "PacketPlayOutMap" -> Class.forName("net.minecraft.network.protocol.game.PacketPlayOutMap")
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
     */
    private fun createMapPacket117Plus(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")
        val mapIdClass = Class.forName("net.minecraft.world.level.saveddata.maps.MapId")

        // 创建 MapId 对象
        val mapIdConstructor = mapIdClass.getConstructor(Int::class.javaPrimitiveType)
        val mapIdObj = mapIdConstructor.newInstance(mapId)

        // 创建 MapPatch（更新区域）
        // MapPatch 是 PacketPlayOutMap 的内部类
        val mapPatchClass = Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap\$b")
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
     * 1.12-1.16.5 地图数据包构建。
     *
     * 不同版本的构造函数签名差异较大，使用反射动态检测可用的构造函数：
     * - 1.14-1.16.5: (int, byte, boolean, boolean, Collection, int, int, int, int, byte[]) - 10参数
     * - 1.12-1.13: (int, byte, boolean, Collection, int, int, int, int, byte[]) - 9参数
     */
    private fun createMapPacketLegacy(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")

        // 获取所有公共构造函数，按参数数量排序（优先尝试参数多的）
        val constructors = packetClass.constructors
            .filter { it.parameterCount >= 9 }
            .sortedByDescending { it.parameterCount }

        for (constructor in constructors) {
            try {
                val paramTypes = constructor.parameterTypes
                val args = buildLegacyMapPacketArgs(paramTypes, mapId, colors)
                if (args != null) {
                    return constructor.newInstance(*args)
                }
            } catch (_: Exception) {
                // 继续尝试下一个构造函数
            }
        }

        throw IllegalStateException(
            "No suitable PacketPlayOutMap constructor found for version ${MinecraftVersion.minecraftVersion}. " +
            "Available constructors: ${constructors.map { it.parameterTypes.map { p -> p.simpleName } }}"
        )
    }

    /**
     * 根据构造函数参数类型，构建地图数据包参数。
     *
     * @return 参数数组，如果无法匹配则返回 null
     */
    private fun buildLegacyMapPacketArgs(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        // 检测参数模式并构建对应的参数数组
        return when (paramTypes.size) {
            10 -> build10ParamArgs(paramTypes, mapId, colors)
            9 -> build9ParamArgs(paramTypes, mapId, colors)
            else -> null
        }
    }

    /**
     * 构建 10 参数地图包（1.14+）
     *
     * 已知签名变体：
     * - 标准 Spigot: (int, byte, boolean, boolean, Collection, int, int, int, int, byte[])
     * - Paper 1.16.5: (int, byte, boolean, boolean, Collection, byte[], int, int, int, int)
     */
    private fun build10ParamArgs(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        // 检查第 6 个参数（索引 5）是 byte[] 还是 int 来区分两种签名
        val sixthParam = paramTypes[5]

        return when {
            // Paper 1.16.5 签名：byte[] 在 Collection 之后
            sixthParam == ByteArray::class.java -> {
                arrayOf(mapId, 0.toByte(), false, false, emptyList<Any>(), colors, 0, 0, 128, 128)
            }
            // 标准 Spigot 签名：byte[] 在最后
            sixthParam == Int::class.javaPrimitiveType || sixthParam == Int::class.java -> {
                arrayOf(mapId, 0.toByte(), false, false, emptyList<Any>(), 0, 0, 128, 128, colors)
            }
            else -> {
                // Fallback：尝试根据参数类型动态构建
                buildArgsFromParamTypes(paramTypes, mapId, colors)
            }
        }
    }

    /**
     * 构建 9 参数地图包（1.12-1.13）
     * 标准签名: (int, byte, boolean, Collection, int, int, int, int, byte[])
     */
    private fun build9ParamArgs(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        if (matchTypes(paramTypes, Int::class, Byte::class, Boolean::class, Collection::class)) {
            return arrayOf(mapId, 0.toByte(), false, emptyList<Any>(), 0, 0, 128, 128, colors)
        }

        // Fallback
        if (paramTypes.last() == ByteArray::class.java &&
            paramTypes[0] == Int::class.javaPrimitiveType
        ) {
            return buildArgsFromParamTypes(paramTypes, mapId, colors)
        }
        return null
    }

    /**
     * 根据参数类型动态构建参数数组。
     * 这是一个 fallback 机制，用于处理非标准签名。
     */
    private fun buildArgsFromParamTypes(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        val args = mutableListOf<Any>()
        var intIndex = 0
        val intValues = listOf(mapId, 0, 0, 128, 128) // mapId, startX, startY, width, height

        for (paramType in paramTypes) {
            when {
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> {
                    args.add(if (intIndex < intValues.size) intValues[intIndex++] else 0)
                }
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> {
                    args.add(0.toByte()) // scale
                }
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> {
                    args.add(false) // trackingPosition or locked
                }
                Collection::class.java.isAssignableFrom(paramType) -> {
                    args.add(emptyList<Any>()) // icons
                }
                paramType == ByteArray::class.java -> {
                    args.add(colors) // map data
                }
                else -> {
                    // 未知类型，无法匹配
                    return null
                }
            }
        }

        return args.toTypedArray()
    }

    /**
     * 检查参数类型是否与预期模式的前几个类型匹配。
     */
    private fun matchTypes(paramTypes: Array<Class<*>>, vararg expectedPrefixes: kotlin.reflect.KClass<*>): Boolean {
        if (paramTypes.size < expectedPrefixes.size) return false
        return expectedPrefixes.indices.all { i ->
            val expected = expectedPrefixes[i]
            val actual = paramTypes[i]
            when (expected) {
                Int::class -> actual == Int::class.javaPrimitiveType || actual == Int::class.java
                Byte::class -> actual == Byte::class.javaPrimitiveType || actual == Byte::class.java
                Boolean::class -> actual == Boolean::class.javaPrimitiveType || actual == Boolean::class.java
                Collection::class -> Collection::class.java.isAssignableFrom(actual)
                else -> expected.java.isAssignableFrom(actual)
            }
        }
    }
}
