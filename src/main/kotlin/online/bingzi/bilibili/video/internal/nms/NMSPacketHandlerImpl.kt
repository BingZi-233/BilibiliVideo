package online.bingzi.bilibili.video.internal.nms

import online.bingzi.bilibili.video.internal.nms.NMSReflectionToolkit.ArgRole
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.map.MapView
import taboolib.module.nms.MinecraftVersion
import taboolib.module.nms.sendPacket
import java.lang.reflect.Constructor
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * NMS 发包实现层。
 *
 * 通过启发式反射跨版本兼容（1.12 - 当前 + 前瞻 26.X.X）。
 *
 * 分层执行链：
 * - L0：ResolvedCtor 缓存（按 packetClass:role 命中即跳过反射）
 * - L2：语义启发式（Record component name → 参数名字典 → 泛型类型）
 * - L3：纯类型启发式（兼容老版本 + 名字不可读时的 safety net）
 * - L4：构造后字段校验（advisory，warn-once；strict 模式抛出回退下一 ctor）
 * - L5：全部失败 → 完整诊断信息
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

    private fun asNMSCopy(item: ItemStack): Any {
        val craftItemStackClass = obcClass("inventory.CraftItemStack")
        val method = craftItemStackClass.getMethod("asNMSCopy", ItemStack::class.java)
        return method.invoke(null, item)
    }

    /**
     * 创建 PacketPlayOutSetSlot 包。
     *
     * 历史签名：
     * - 1.12 - 1.16.5：(int windowId, int slot, ItemStack)
     * - 1.17 - 1.21.X：(int windowId, int stateId, int slot, ItemStack)
     * - 26.X.X：未知，可能引入 ContainerId 值类等。
     */
    private fun createSetSlotPacket(windowId: Int, slot: Int, nmsItem: Any): Any {
        val packetClass = nmsClass("PacketPlayOutSetSlot")
        val itemStackClass = nmsClassOrNull("ItemStack")
        val cacheKey = "${packetClass.name}:setSlot"

        resolvedCtorCache[cacheKey]?.let { resolved ->
            val args = resolved.argBuilder(
                mapOf(
                    ArgRole.WINDOW_ID to windowId,
                    ArgRole.STATE_ID to 0,
                    ArgRole.SLOT to slot,
                    ArgRole.ITEM_STACK to nmsItem
                )
            )
            return resolved.ctor.newInstance(*args)
        }

        val constructors = packetClass.constructors.sortedByDescending { it.parameterCount }
        val errors = mutableListOf<String>()

        for (constructor in constructors) {
            val attempt = tryConstructSetSlot(constructor, windowId, slot, nmsItem, itemStackClass, errors)
            if (attempt != null) {
                cacheSetSlotCtor(cacheKey, constructor, itemStackClass)
                return attempt
            }
        }

        val signatures = constructors.map { it.parameterTypes.map { p -> p.simpleName } }
        warnOnce(
            "setSlot:${packetClass.name}",
            "[BilibiliVideo NMS] PacketPlayOutSetSlot 构造发现失败 mc=${MinecraftVersion.minecraftVersion} " +
                    "isRecord=${NMSReflectionToolkit.isRecord(packetClass)} " +
                    "signatures=$signatures errors=$errors"
        )
        throw IllegalStateException(
            "No suitable PacketPlayOutSetSlot constructor for ${MinecraftVersion.minecraftVersion}.\n" +
                    "Available: $signatures\nErrors: $errors"
        )
    }

    private fun tryConstructSetSlot(
        constructor: Constructor<*>,
        windowId: Int,
        slot: Int,
        nmsItem: Any,
        itemStackClass: Class<*>?,
        errors: MutableList<String>
    ): Any? {
        return try {
            val args = buildArgsForSetSlot(
                constructor,
                windowId = windowId,
                stateId = 0,
                slot = slot,
                nmsItem = nmsItem,
                itemStackClass = itemStackClass
            ) ?: run {
                errors.add("${constructor.parameterTypes.map { it.simpleName }}: unsupported param type")
                return null
            }
            val packet = constructor.newInstance(*args)
            verifySetSlot(packet, windowId, slot, constructor, errors)?.let { return it }
            packet
        } catch (e: Exception) {
            errors.add("${constructor.parameterTypes.map { it.simpleName }}: ${e.message}")
            null
        }
    }

    private fun verifySetSlot(packet: Any, windowId: Int, slot: Int, ctor: Constructor<*>, errors: MutableList<String>): Any? {
        val expected = mapOf(
            "slot" to slot,
            "slotId" to slot,
            "i" to slot,
            "windowId" to windowId,
            "containerId" to windowId
        )
        val mismatches = NMSReflectionToolkit.verifyPacketFields(packet, expected)
        if (mismatches.isEmpty()) return null
        val key = "verifySetSlot:${ctor.parameterTypes.map { it.simpleName }}"
        warnOnce(
            key,
            "[BilibiliVideo NMS] SetSlot 构造校验告警 ctor=${ctor.parameterTypes.map { it.simpleName }} " +
                    "mismatches=$mismatches (advisory; 设置 -Dbilibilivideo.nms.strict=true 切换到严格模式)"
        )
        if (strictMode) {
            errors.add("${ctor.parameterTypes.map { it.simpleName }}: verify failed $mismatches")
            return null
        }
        return packet
    }

    private fun cacheSetSlotCtor(key: String, ctor: Constructor<*>, itemStackClass: Class<*>?) {
        resolvedCtorCache[key] = NMSReflectionToolkit.ResolvedCtor(
            ctor = ctor,
            argBuilder = { inputs ->
                val windowId = inputs[ArgRole.WINDOW_ID] as Int
                val stateId = inputs[ArgRole.STATE_ID] as Int
                val slot = inputs[ArgRole.SLOT] as Int
                val nmsItem = inputs[ArgRole.ITEM_STACK]!!
                buildArgsForSetSlot(ctor, windowId, stateId, slot, nmsItem, itemStackClass)!!
            }
        )
    }

    /**
     * 启发式构建 SetSlot 构造参数。
     *
     * 顺序：
     * 1. 参数名 / Record component name → ArgRole 字典
     * 2. 类型 + Optional 泛型识别
     * 3. int 队列 [windowId, stateId, slot] 兜底
     */
    private fun buildArgsForSetSlot(
        ctor: Constructor<*>,
        windowId: Int,
        stateId: Int,
        slot: Int,
        nmsItem: Any,
        itemStackClass: Class<*>?
    ): Array<Any>? {
        val paramTypes = ctor.parameterTypes
        val parameters = ctor.parameters
        val recordRoles = recordRolesFor(ctor.declaringClass, paramTypes.size)
        val intQueue = ArrayDeque(listOf(windowId, stateId, slot))
        val args = mutableListOf<Any>()

        for (i in paramTypes.indices) {
            val paramType = paramTypes[i]
            val role = resolveRole(parameters.getOrNull(i)?.let { if (it.isNamePresent) it.name else null }, recordRoles, i)
            val arg: Any? = when (role) {
                ArgRole.WINDOW_ID -> windowId
                ArgRole.STATE_ID -> stateId
                ArgRole.SLOT -> slot
                ArgRole.ITEM_STACK -> nmsItem
                else -> resolveSetSlotByType(paramType, intQueue, nmsItem, itemStackClass, windowId)
            }
            if (arg == null) return null
            args.add(arg)
        }
        return args.toTypedArray()
    }

    private fun resolveSetSlotByType(
        paramType: Class<*>,
        intQueue: ArrayDeque<Int>,
        nmsItem: Any,
        itemStackClass: Class<*>?,
        windowId: Int
    ): Any? {
        return when {
            paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> {
                if (intQueue.isNotEmpty()) intQueue.removeFirst() else 0
            }
            paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> false
            paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> 0.toByte()
            Optional::class.java == paramType -> Optional.empty<Any>()
            Collection::class.java.isAssignableFrom(paramType) -> emptyList<Any>()
            itemStackClass != null && paramType.isAssignableFrom(itemStackClass) -> nmsItem
            paramType.simpleName == "ItemStack" -> nmsItem
            NMSReflectionToolkit.isLikelyIntWrapper(paramType) -> tryConstructFromInt(paramType, windowId)
            else -> null
        }
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
     *
     * 多名探测：getId / getMapId / id 方法 → int 字段名 id / mapId。
     */
    private fun resolveMapId(mapView: MapView): Int {
        NMSReflectionToolkit.probeIntMethod(mapView, listOf("getId", "getMapId", "id"))?.let { return it }
        NMSReflectionToolkit.probeIntField(mapView, listOf("id", "mapId"))?.let { return it }
        warnOnce(
            "resolveMapId:${mapView.javaClass.name}",
            "[BilibiliVideo NMS] resolveMapId 未命中任何已知方法/字段 cls=${mapView.javaClass.name}"
        )
        return 0
    }

    private fun createMapPacket(mapId: Int, colors: ByteArray): Any {
        return if (isModernNms) {
            createMapPacket117Plus(mapId, colors)
        } else {
            createMapPacketLegacy(mapId, colors)
        }
    }

    /**
     * 1.17+ 地图数据包构建。
     *
     * 走泛型解析 Optional<MapPatch> 精准定位 mapPatch 槽位（不再依赖 optionalIndex 顺位假设）。
     * trackingPosition 等命名 boolean 修正为语义正确值（true）。
     */
    private fun createMapPacket117Plus(mapId: Int, colors: ByteArray): Any {
        val packetClass = nmsClass("PacketPlayOutMap")
        val mapIdClass = nmsClassOrNull("MapId")
        val mapPatchClass = nmsClassOrNull("MapPatch")
        val mapIdObj = mapIdClass?.let { tryConstructFromInt(it, mapId) }
        val mapPatch = createMapPatchOrNull(mapPatchClass, colors)

        val cacheKey = "${packetClass.name}:map117"
        resolvedCtorCache[cacheKey]?.let { resolved ->
            val args = resolved.argBuilder(
                mapOf(
                    ArgRole.MAP_ID to (mapIdObj ?: mapId),
                    ArgRole.MAP_PATCH to mapPatch
                )
            )
            return resolved.ctor.newInstance(*args)
        }

        val constructors = packetClass.constructors.sortedByDescending { it.parameterCount }
        val errors = mutableListOf<String>()

        for (constructor in constructors) {
            val attempt = tryConstructMap117(constructor, mapId, mapIdClass, mapIdObj, mapPatchClass, mapPatch, errors)
            if (attempt != null) {
                cacheMap117Ctor(cacheKey, constructor, mapId, mapIdClass, mapPatchClass)
                return attempt
            }
        }

        val signatures = constructors.map { it.parameterTypes.map { p -> p.simpleName } }
        warnOnce(
            "mapPacket117:${packetClass.name}",
            "[BilibiliVideo NMS] PacketPlayOutMap(modern) 构造发现失败 mc=${MinecraftVersion.minecraftVersion} " +
                    "isRecord=${NMSReflectionToolkit.isRecord(packetClass)} " +
                    "signatures=$signatures errors=$errors"
        )
        throw IllegalStateException(
            "No suitable PacketPlayOutMap constructor found for ${MinecraftVersion.minecraftVersion}.\n" +
                    "Available: $signatures\nErrors: $errors"
        )
    }

    private fun tryConstructMap117(
        constructor: Constructor<*>,
        mapId: Int,
        mapIdClass: Class<*>?,
        mapIdObj: Any?,
        mapPatchClass: Class<*>?,
        mapPatch: Any?,
        errors: MutableList<String>
    ): Any? {
        return try {
            val args = buildArgsForMapPacket117Plus(
                constructor,
                mapId = mapId,
                mapIdClass = mapIdClass,
                mapIdObj = mapIdObj,
                mapPatchClass = mapPatchClass,
                mapPatch = mapPatch
            ) ?: run {
                errors.add("${constructor.parameterTypes.map { it.simpleName }}: unsupported param type")
                return null
            }
            val packet = constructor.newInstance(*args)
            verifyMapPacket(packet, mapId, constructor, errors)?.let { return it }
            packet
        } catch (e: Exception) {
            errors.add("${constructor.parameterTypes.map { it.simpleName }}: ${e.message}")
            null
        }
    }

    private fun verifyMapPacket(packet: Any, mapId: Int, ctor: Constructor<*>, errors: MutableList<String>): Any? {
        val expected = mapOf("mapId" to mapId, "id" to mapId)
        val mismatches = NMSReflectionToolkit.verifyPacketFields(packet, expected)
        if (mismatches.isEmpty()) return null
        val key = "verifyMap:${ctor.parameterTypes.map { it.simpleName }}"
        warnOnce(
            key,
            "[BilibiliVideo NMS] Map 构造校验告警 ctor=${ctor.parameterTypes.map { it.simpleName }} " +
                    "mismatches=$mismatches (advisory; 设置 -Dbilibilivideo.nms.strict=true 切换到严格模式)"
        )
        if (strictMode) {
            errors.add("${ctor.parameterTypes.map { it.simpleName }}: verify failed $mismatches")
            return null
        }
        return packet
    }

    private fun cacheMap117Ctor(
        key: String,
        ctor: Constructor<*>,
        mapId: Int,
        mapIdClass: Class<*>?,
        mapPatchClass: Class<*>?
    ) {
        resolvedCtorCache[key] = NMSReflectionToolkit.ResolvedCtor(
            ctor = ctor,
            argBuilder = { inputs ->
                val mapIdValue = inputs[ArgRole.MAP_ID]
                val mapPatch = inputs[ArgRole.MAP_PATCH]
                buildArgsForMapPacket117Plus(
                    ctor,
                    mapId = (mapIdValue as? Int) ?: mapId,
                    mapIdClass = mapIdClass,
                    mapIdObj = if (mapIdValue != null && mapIdValue !is Int) mapIdValue else null,
                    mapPatchClass = mapPatchClass,
                    mapPatch = mapPatch
                )!!
            }
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

    /**
     * 通过形状扫描定位 MapPatch 构造函数（4×int + byte[]）。
     */
    private fun createMapPatchOrNull(mapPatchClass: Class<*>?, colors: ByteArray): Any? {
        if (mapPatchClass == null) return null
        val descriptor = NMSReflectionToolkit.findMapPatchCtor(mapPatchClass) ?: return null
        return try {
            val args = NMSReflectionToolkit.buildMapPatchArgs(descriptor, 0, 0, 128, 128, colors)
            descriptor.ctor.newInstance(*args)
        } catch (e: Throwable) {
            warnOnce(
                "mapPatchCtor:${mapPatchClass.name}",
                "[BilibiliVideo NMS] MapPatch 构造失败 cls=${mapPatchClass.name} " +
                        "ctor=${descriptor.ctor.parameterTypes.map { it.simpleName }} order=${descriptor.argOrder} err=${e.message}"
            )
            null
        }
    }

    /**
     * 1.17+ Map packet 参数构建。
     *
     * 关键改进：
     * - Optional<MapPatch>：用泛型签名解析；元素类型匹配 MapPatch → Optional.of(mapPatch)，否则 Optional.empty()
     * - 参数名识别 trackingPosition / locked → 语义正确填值（trackingPosition 默认 true）
     */
    private fun buildArgsForMapPacket117Plus(
        ctor: Constructor<*>,
        mapId: Int,
        mapIdClass: Class<*>?,
        mapIdObj: Any?,
        mapPatchClass: Class<*>?,
        mapPatch: Any?
    ): Array<Any>? {
        val paramTypes = ctor.parameterTypes
        val parameters = ctor.parameters
        val genericTypes = ctor.genericParameterTypes
        val recordRoles = recordRolesFor(ctor.declaringClass, paramTypes.size)
        val args = mutableListOf<Any>()

        for (i in paramTypes.indices) {
            val paramType = paramTypes[i]
            val role = resolveRole(parameters.getOrNull(i)?.let { if (it.isNamePresent) it.name else null }, recordRoles, i)
            val arg: Any? = when {
                mapIdClass != null && paramType == mapIdClass -> mapIdObj
                role == ArgRole.MAP_ID && (paramType == Int::class.javaPrimitiveType || paramType == Int::class.java) -> mapId
                role == ArgRole.LOCKED -> false
                role == ArgRole.TRACKING_POSITION -> true
                role == ArgRole.SCALE -> 0.toByte()
                role == ArgRole.DECORATIONS -> emptyList<Any>()
                role == ArgRole.MAP_PATCH -> mapPatch
                paramType == Optional::class.java -> resolveOptionalArg(genericTypes[i], mapPatchClass, mapPatch)
                paramType == Int::class.javaPrimitiveType || paramType == Int::class.java -> mapId
                paramType == Byte::class.javaPrimitiveType || paramType == Byte::class.java -> 0.toByte()
                paramType == Boolean::class.javaPrimitiveType || paramType == Boolean::class.java -> false
                Collection::class.java.isAssignableFrom(paramType) -> emptyList<Any>()
                paramType.name.endsWith("MapPatch") || paramType.name.endsWith("WorldMap\$b") || paramType.name.endsWith("Patch") -> mapPatch
                else -> null
            }
            if (arg == null) return null
            args.add(arg)
        }
        return args.toTypedArray()
    }

    private fun resolveOptionalArg(genericType: java.lang.reflect.Type, mapPatchClass: Class<*>?, mapPatch: Any?): Any {
        val element = NMSReflectionToolkit.optionalElementType(genericType)
        if (mapPatchClass != null && element != null && mapPatchClass.isAssignableFrom(element) && mapPatch != null) {
            return Optional.of(mapPatch)
        }
        return Optional.empty<Any>()
    }

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
     * 老版本（1.12 - 1.16.5）类型启发式参数构建。
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
     * 读取 [ctorOwner] 的 Record component 名映射到 ArgRole（如果是 Record）。
     *
     * 仅当 component 数量与构造参数数量一致时使用，避免合成 ctor 错位。
     */
    private fun recordRolesFor(ctorOwner: Class<*>, paramCount: Int): List<ArgRole>? {
        val comps = NMSReflectionToolkit.recordComponents(ctorOwner) ?: return null
        if (comps.size != paramCount) return null
        return comps.map { NMSReflectionToolkit.roleOf(it.name) }
    }

    private fun resolveRole(paramName: String?, recordRoles: List<ArgRole>?, index: Int): ArgRole {
        val byName = NMSReflectionToolkit.roleOf(paramName)
        if (byName != ArgRole.UNKNOWN) return byName
        return recordRoles?.getOrNull(index) ?: ArgRole.UNKNOWN
    }

    private fun logStartupProbe() {
        if (!startupProbeLogged.compareAndSet(false, true)) return
        val mc = try { MinecraftVersion.minecraftVersion } catch (_: Throwable) { "?" }
        val universal = try { MinecraftVersion.isUniversal } catch (_: Throwable) { false }
        Bukkit.getLogger().info(
            "[BilibiliVideo NMS] modern=$isModernNms mcVersion=$mc isUniversal=$universal " +
                    "obcUnknown=${mc == "UNKNOWN"} strict=$strictMode"
        )
    }

    private fun warnOnce(key: String, msg: String) {
        if (warnedKeys.add(key)) {
            Bukkit.getLogger().warning(msg)
        }
    }

    /**
     * 运行时探测是否处于 1.17+ NMS 包结构（含 26.X.X）。
     */
    private val isModernNms: Boolean by lazy {
        loadClassOrNull("net.minecraft.network.protocol.game.PacketPlayOutSetSlot") != null ||
                loadClassOrNull("net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket") != null
    }

    private val strictMode: Boolean by lazy {
        System.getProperty("bilibilivideo.nms.strict") == "true"
    }

    private val nmsClassCache = ConcurrentHashMap<String, Class<*>>()
    private val warnedKeys = ConcurrentHashMap.newKeySet<String>()
    private val resolvedCtorCache = ConcurrentHashMap<String, NMSReflectionToolkit.ResolvedCtor>()
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
