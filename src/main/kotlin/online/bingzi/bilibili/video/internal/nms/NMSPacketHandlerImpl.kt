package online.bingzi.bilibili.video.internal.nms

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * NMS 发包实现层。
 *
 * 通过启发式反射跨版本兼容（1.12 - 当前 + 前瞻 26.X.X）。
 *
 * 核心策略：
 * - 类查找走多候选注册表，覆盖 Mojang / Spigot / 历史命名 + 26.X 重定位猜测。
 * - 构造函数走启发式参数填充，自动适配新增 / 变更字段。
 * - 版本判定独立于 [MinecraftVersion.isUniversal]，按运行时类存在性探测。
 * - 关键失败点 warn-once，便于用户上报真实签名。
 */
@Suppress("unused")
class NMSPacketHandlerImpl : NMSPacketHandler() {

    init {
        logStartupProbe()
    }

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
     * 创建 PacketPlayOutSetSlot 包（启发式构造发现）。
     *
     * 历史签名：
     * - 1.12 - 1.16.5：(int windowId, int slot, ItemStack)
     * - 1.17 - 1.21.X：(int windowId, int stateId, int slot, ItemStack)
     * - 26.X.X：未知，可能在前述基础上插入 ContainerId 值类等。
     */
    private fun createSetSlotPacket(windowId: Int, slot: Int, nmsItem: Any): Any {
        val packetClass = nmsClass("PacketPlayOutSetSlot")
        val itemStackClass = nmsClassOrNull("ItemStack")

        val constructors = packetClass.constructors.sortedByDescending { it.parameterCount }
        val errors = mutableListOf<String>()

        for (constructor in constructors) {
            try {
                val args = buildArgsForSetSlot(
                    constructor.parameterTypes,
                    windowId = windowId,
                    stateId = 0,
                    slot = slot,
                    nmsItem = nmsItem,
                    itemStackClass = itemStackClass
                )
                if (args != null) {
                    return constructor.newInstance(*args)
                } else {
                    errors.add("${constructor.parameterTypes.map { it.simpleName }}: unsupported param type")
                }
            } catch (e: Exception) {
                errors.add("${constructor.parameterTypes.map { it.simpleName }}: ${e.message}")
            }
        }

        val signatures = constructors.map { it.parameterTypes.map { p -> p.simpleName } }
        warnOnce(
            "setSlot:${packetClass.name}",
            "[BilibiliVideo NMS] PacketPlayOutSetSlot 构造发现失败 mc=${MinecraftVersion.minecraftVersion} " +
                "signatures=$signatures errors=$errors"
        )
        throw IllegalStateException(
            "No suitable PacketPlayOutSetSlot constructor for ${MinecraftVersion.minecraftVersion}.\n" +
                "Available: $signatures\nErrors: $errors"
        )
    }

    /**
     * 启发式构建 SetSlot 构造参数。
     *
     * int 队列：[windowId, stateId, slot]，溢出补 0。
     * 单 int 构造的非原生非 ItemStack 类视为 ContainerId-style 值类，传 windowId。
     */
    private fun buildArgsForSetSlot(
        paramTypes: Array<Class<*>>,
        windowId: Int,
        stateId: Int,
        slot: Int,
        nmsItem: Any,
        itemStackClass: Class<*>?
    ): Array<Any>? {
        val intQueue = ArrayDeque(listOf(windowId, stateId, slot))
        val args = mutableListOf<Any>()

        for (paramType in paramTypes) {
            val arg: Any = when {
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> {
                    if (intQueue.isNotEmpty()) intQueue.removeFirst() else 0
                }
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> false
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> 0.toByte()
                Optional::class.java == paramType -> Optional.empty<Any>()
                Collection::class.java.isAssignableFrom(paramType) -> emptyList<Any>()
                itemStackClass != null && paramType.isAssignableFrom(itemStackClass) -> nmsItem
                paramType.simpleName == "ItemStack" -> nmsItem
                else -> {
                    // 尝试将非原生类视为 int 值类（ContainerId 等）
                    val viaInt = tryConstructFromInt(paramType, windowId)
                    viaInt ?: return null
                }
            }
            args.add(arg)
        }

        return args.toTypedArray()
    }

    private fun tryConstructFromInt(type: Class<*>, value: Int): Any? {
        return try {
            val ctor = type.getDeclaredConstructor(Int::class.javaPrimitiveType)
            ctor.isAccessible = true
            ctor.newInstance(value)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 解析 NMS 类（多候选 + 缓存）。
     *
     * 1.17+ 走候选注册表，自动覆盖 Mojang / Spigot 命名。
     * 1.16.5 - 走 net.minecraft.server.$version.$name。
     */
    private fun nmsClass(name: String): Class<*> {
        nmsClassCache[name]?.let { return it }

        if (isModernNms) {
            val candidates = MODERN_NMS_CANDIDATES[name]
                ?: throw IllegalArgumentException("Unknown NMS class: $name")
            for (candidate in candidates) {
                val cls = loadClassOrNull(candidate)
                if (cls != null) {
                    nmsClassCache[name] = cls
                    return cls
                }
            }
            warnOnce(
                "nmsClass:$name",
                "[BilibiliVideo NMS] $name 全候选未命中 mc=${MinecraftVersion.minecraftVersion} " +
                    "candidates=$candidates loader=${Bukkit.getServer().javaClass.classLoader}"
            )
            throw ClassNotFoundException("None of the classes found for '$name': ${candidates.joinToString()}")
        } else {
            val version = MinecraftVersion.minecraftVersion
            val cls = loadClass("net.minecraft.server.$version.$name")
            nmsClassCache[name] = cls
            return cls
        }
    }

    private fun nmsClassOrNull(name: String): Class<*>? {
        return try {
            nmsClass(name)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 获取 OBC (org.bukkit.craftbukkit) 类。
     *
     * 1.20.5+ / Folia / 26.X：org.bukkit.craftbukkit.xxx（无版本字符串）
     * 1.20.4-：org.bukkit.craftbukkit.v1_XX_RX.xxx
     *
     * 即使 minecraftVersion 报非 UNKNOWN，也会兜底尝试无版本路径，防 26.X 报 "26.X.X" 但实际无版本化包。
     */
    private fun obcClass(name: String): Class<*> {
        val version = MinecraftVersion.minecraftVersion
        if (version == "UNKNOWN") {
            return loadClass("org.bukkit.craftbukkit.$name")
        }
        return try {
            loadClass("org.bukkit.craftbukkit.$version.$name")
        } catch (_: ClassNotFoundException) {
            loadClass("org.bukkit.craftbukkit.$name")
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
     * 1.17+：PacketPlayOutMap(MapId, scale, locked, icons, MapPatch)
     * 1.12-1.16.5：PacketPlayOutMap(mapId, scale, trackingPosition, locked, icons, startX, startY, width, height, data)
     */
    private fun createMapPacket(mapId: Int, colors: ByteArray): Any {
        return if (isModernNms) {
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
        val mapIdClass = nmsClassOrNull("MapId")
        val mapIdObj = mapIdClass?.let {
            try {
                val mapIdConstructor = it.getConstructor(Int::class.javaPrimitiveType)
                mapIdConstructor.newInstance(mapId)
            } catch (_: Throwable) {
                null
            }
        }

        val mapPatch = createMapPatchOrNull(colors)

        val constructors = packetClass.constructors.sortedByDescending { it.parameterCount }
        val errors = mutableListOf<String>()

        for (constructor in constructors) {
            try {
                val args = buildArgsForMapPacket117Plus(
                    constructor.parameterTypes,
                    mapId,
                    mapIdClass,
                    mapIdObj,
                    mapPatch
                )
                if (args != null) {
                    return constructor.newInstance(*args)
                } else {
                    errors.add("${constructor.parameterTypes.map { it.simpleName }}: unsupported param type")
                }
            } catch (e: Exception) {
                errors.add("${constructor.parameterTypes.map { it.simpleName }}: ${e.message}")
            }
        }

        val signatures = constructors.map { it.parameterTypes.map { p -> p.simpleName } }
        warnOnce(
            "mapPacket117:${packetClass.name}",
            "[BilibiliVideo NMS] PacketPlayOutMap(modern) 构造发现失败 mc=${MinecraftVersion.minecraftVersion} " +
                "signatures=$signatures errors=$errors"
        )
        throw IllegalStateException(
            "No suitable PacketPlayOutMap constructor found for ${MinecraftVersion.minecraftVersion}.\n" +
                "Available: $signatures\nErrors: $errors"
        )
    }

    private fun loadClass(name: String): Class<*> {
        val serverLoader = Bukkit.getServer().javaClass.classLoader
        return Class.forName(name, false, serverLoader)
    }

    private fun loadClassOrNull(name: String): Class<*>? {
        return try {
            loadClass(name)
        } catch (_: ClassNotFoundException) {
            null
        }
    }

    private fun createMapPatchOrNull(colors: ByteArray): Any? {
        val mapPatchClass = nmsClassOrNull("MapPatch") ?: return null
        return try {
            val mapPatchConstructor = mapPatchClass.getConstructor(
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                ByteArray::class.java
            )
            mapPatchConstructor.newInstance(0, 0, 128, 128, colors)
        } catch (_: Throwable) {
            null
        }
    }

    private fun buildArgsForMapPacket117Plus(
        paramTypes: Array<Class<*>>,
        mapId: Int,
        mapIdClass: Class<*>?,
        mapIdObj: Any?,
        mapPatch: Any?
    ): Array<Any>? {
        val args = mutableListOf<Any>()
        var optionalIndex = 0

        for (paramType in paramTypes) {
            val arg = when {
                mapIdClass != null && paramType == mapIdClass -> mapIdObj ?: return null
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> mapId
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> 0.toByte()
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> false
                Optional::class.java == paramType -> {
                    val value = if (optionalIndex == 0) {
                        Optional.empty<Any>()
                    } else {
                        if (mapPatch != null) Optional.of(mapPatch) else Optional.empty<Any>()
                    }
                    optionalIndex++
                    value
                }
                Collection::class.java.isAssignableFrom(paramType) -> emptyList<Any>()
                paramType.name.endsWith("MapPatch") || paramType.name.endsWith("WorldMap\$b") || paramType.name.endsWith("Patch") -> mapPatch ?: return null
                else -> return null
            }
            args.add(arg)
        }

        return args.toTypedArray()
    }

    /**
     * 1.12-1.16.5 地图数据包构建（启发式自动兼容）。
     *
     * - int 队列：[mapId, startX(0), startY(0), width(128), height(128)]
     * - byte: scale = 0
     * - boolean: trackingPosition / locked = false
     * - Collection: icons = emptyList
     * - byte[]: colors
     */
    private fun createMapPacketLegacy(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")

        val constructors = packetClass.constructors
            .filter { it.parameterCount >= 8 }
            .sortedByDescending { it.parameterCount }

        if (constructors.isEmpty()) {
            val allConstructors = packetClass.constructors.sortedByDescending { it.parameterCount }
            val signatures = allConstructors.map { c -> c.parameterTypes.map { it.simpleName } }
            warnOnce(
                "mapPacketLegacy:${packetClass.name}",
                "[BilibiliVideo NMS] PacketPlayOutMap(legacy) 无可用构造 mc=${MinecraftVersion.minecraftVersion} " +
                    "signatures=$signatures"
            )
            throw IllegalStateException(
                "No PacketPlayOutMap constructor with >= 8 params for ${MinecraftVersion.minecraftVersion}.\n" +
                    "All constructors: $signatures"
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

        val signatures = constructors.map { it.parameterTypes.map { p -> p.simpleName } }
        warnOnce(
            "mapPacketLegacy:${packetClass.name}",
            "[BilibiliVideo NMS] PacketPlayOutMap(legacy) 构造发现失败 mc=${MinecraftVersion.minecraftVersion} " +
                "signatures=$signatures errors=$errors"
        )
        throw IllegalStateException(
            "No suitable PacketPlayOutMap constructor found for ${MinecraftVersion.minecraftVersion}.\n" +
                "Available: $signatures\nErrors: $errors"
        )
    }

    /**
     * 启发式构建参数数组。
     *
     * 根据参数类型自动推断填充值，无需硬编码版本签名。
     */
    private fun buildArgsHeuristically(paramTypes: Array<Class<*>>, mapId: Int, colors: ByteArray): Array<Any>? {
        val args = mutableListOf<Any>()
        val intValues = mutableListOf(mapId, 0, 0, 128, 128)

        for (paramType in paramTypes) {
            val arg = when {
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> {
                    if (intValues.isNotEmpty()) intValues.removeAt(0) else 0
                }
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> 0.toByte()
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> false
                Collection::class.java.isAssignableFrom(paramType) -> emptyList<Any>()
                paramType == ByteArray::class.java -> colors
                else -> return null
            }
            args.add(arg)
        }

        return args.toTypedArray()
    }

    /**
     * 启动一次性诊断探针，便于 Issue 上报复现环境。
     */
    private fun logStartupProbe() {
        if (!startupProbeLogged.compareAndSet(false, true)) return
        val mc = try { MinecraftVersion.minecraftVersion } catch (_: Throwable) { "?" }
        val universal = try { MinecraftVersion.isUniversal } catch (_: Throwable) { false }
        Bukkit.getLogger().info(
            "[BilibiliVideo NMS] modern=$isModernNms mcVersion=$mc isUniversal=$universal obcUnknown=${mc == "UNKNOWN"}"
        )
    }

    private fun warnOnce(key: String, msg: String) {
        if (warnedKeys.add(key)) {
            Bukkit.getLogger().warning(msg)
        }
    }

    /**
     * 运行时探测是否处于 1.17+ NMS 包结构（含 26.X.X）。
     *
     * 不依赖 [MinecraftVersion.isUniversal]，避免 26.X.X 命名跳跃导致误判。
     */
    private val isModernNms: Boolean by lazy {
        loadClassOrNull("net.minecraft.network.protocol.game.PacketPlayOutSetSlot") != null ||
            loadClassOrNull("net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket") != null
    }

    private val nmsClassCache = ConcurrentHashMap<String, Class<*>>()
    private val warnedKeys = ConcurrentHashMap.newKeySet<String>()
    private val startupProbeLogged = java.util.concurrent.atomic.AtomicBoolean(false)

    companion object {
        /**
         * 1.17+ NMS 类的多候选查找表。
         *
         * 顺序：Mojang 映射 → Spigot 映射 → 26.X 重定位猜测。
         * 第一个解析成功的候选会被缓存。
         */
        private val MODERN_NMS_CANDIDATES: Map<String, List<String>> = mapOf(
            "PacketPlayOutSetSlot" to listOf(
                "net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket",
                "net.minecraft.network.protocol.game.PacketPlayOutSetSlot"
            ),
            "PacketPlayOutMap" to listOf(
                "net.minecraft.network.protocol.game.ClientboundMapItemDataPacket",
                "net.minecraft.network.protocol.game.PacketPlayOutMap"
            ),
            "ItemStack" to listOf(
                "net.minecraft.world.item.ItemStack",
                "net.minecraft.world.ItemStack"
            ),
            "MapId" to listOf(
                "net.minecraft.world.level.saveddata.maps.MapId",
                "net.minecraft.world.saveddata.maps.MapId"
            ),
            "MapPatch" to listOf(
                "net.minecraft.world.level.saveddata.maps.MapItemSavedData\$MapPatch",
                "net.minecraft.world.level.saveddata.maps.WorldMap\$b",
                "net.minecraft.world.level.saveddata.maps.MapItemSavedData\$Patch"
            )
        )
    }
}
