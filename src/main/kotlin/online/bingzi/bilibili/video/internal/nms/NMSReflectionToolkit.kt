package online.bingzi.bilibili.video.internal.nms

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * NMS 反射工具集。
 *
 * 提供语义化反射构造能力：参数名识别、Record 探测、泛型解析、字段校验、多名探测。
 *
 * 设计目标：把"按位置填值"升级为"按语义填值"，覆盖字段重命名 / 顺序变更 / Optional
 * 元素类型变化 / Record 化等真实演进路径。
 */
internal object NMSReflectionToolkit {

    /**
     * 参数语义角色。
     *
     * 用参数名（或 Record component name）映射到角色，避免按位置硬编码。
     */
    enum class ArgRole {
        WINDOW_ID, STATE_ID, SLOT,
        MAP_ID, SCALE,
        LOCKED, TRACKING_POSITION,
        DECORATIONS, MAP_PATCH,
        ITEM_STACK, BYTE_ARRAY,
        START_X, START_Y, WIDTH, HEIGHT,
        UNKNOWN
    }

    /**
     * 参数名 → 角色字典。
     *
     * key 已 lower-case 并去 underscore，匹配时同样规范化。
     * 来源：1.16 - 1.21 Mojang 映射 + Spigot 部分常见命名。
     */
    private val NAME_DICT: Map<String, ArgRole> = mapOf(
        "windowid" to ArgRole.WINDOW_ID,
        "containerid" to ArgRole.WINDOW_ID,
        "syncid" to ArgRole.WINDOW_ID,
        "stateid" to ArgRole.STATE_ID,
        "revision" to ArgRole.STATE_ID,
        "slot" to ArgRole.SLOT,
        "slotid" to ArgRole.SLOT,
        "slotindex" to ArgRole.SLOT,
        "mapid" to ArgRole.MAP_ID,
        "id" to ArgRole.MAP_ID,
        "scale" to ArgRole.SCALE,
        "locked" to ArgRole.LOCKED,
        "trackingposition" to ArgRole.TRACKING_POSITION,
        "showdecorations" to ArgRole.TRACKING_POSITION,
        "decorations" to ArgRole.DECORATIONS,
        "icons" to ArgRole.DECORATIONS,
        "banners" to ArgRole.DECORATIONS,
        "colorpatch" to ArgRole.MAP_PATCH,
        "patch" to ArgRole.MAP_PATCH,
        "mapdecorations" to ArgRole.MAP_PATCH,
        "itemstack" to ArgRole.ITEM_STACK,
        "item" to ArgRole.ITEM_STACK,
        "carrieditem" to ArgRole.ITEM_STACK,
        "startx" to ArgRole.START_X,
        "x" to ArgRole.START_X,
        "starty" to ArgRole.START_Y,
        "y" to ArgRole.START_Y,
        "width" to ArgRole.WIDTH,
        "height" to ArgRole.HEIGHT,
        "colors" to ArgRole.BYTE_ARRAY,
        "mapcolors" to ArgRole.BYTE_ARRAY,
        "data" to ArgRole.BYTE_ARRAY
    )

    /**
     * 规范化名字（lower-case + 去 underscore）以便匹配字典。
     */
    private fun normalize(name: String): String = name.lowercase().replace("_", "")

    /**
     * 通过参数名识别角色。
     *
     * 仅在 [Parameter.isNamePresent] 为 true 时有效（需要 jar 编译时带 -parameters 或保留 record component 名）。
     */
    fun roleOf(name: String?): ArgRole {
        if (name.isNullOrBlank()) return ArgRole.UNKNOWN
        return NAME_DICT[normalize(name)] ?: ArgRole.UNKNOWN
    }

    /**
     * Record 类探测（JDK 16+，反射调用避免编译期依赖）。
     */
    fun isRecord(cls: Class<*>): Boolean {
        return try {
            val m = Class::class.java.getMethod("isRecord")
            m.invoke(cls) as? Boolean == true
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Record 组件描述。
     */
    data class RecordComp(val name: String, val type: Class<*>)

    /**
     * 读取 Record components。null 表示非 Record 或反射失败。
     */
    fun recordComponents(cls: Class<*>): List<RecordComp>? {
        return try {
            val m = Class::class.java.getMethod("getRecordComponents")
            val arr = m.invoke(cls) as? Array<*> ?: return null
            arr.mapNotNull { rc ->
                rc ?: return@mapNotNull null
                val rcCls = rc.javaClass
                val nm = rcCls.getMethod("getName").invoke(rc) as? String ?: return@mapNotNull null
                val ty = rcCls.getMethod("getType").invoke(rc) as? Class<*> ?: return@mapNotNull null
                RecordComp(nm, ty)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * 解析 [Optional] 参数的元素类型。返回 null 表示未知或非 Optional。
     */
    fun optionalElementType(generic: Type): Class<*>? {
        if (generic !is ParameterizedType) return null
        val raw = generic.rawType as? Class<*> ?: return null
        if (raw != Optional::class.java) return null
        val arg = generic.actualTypeArguments.firstOrNull() ?: return null
        return when (arg) {
            is Class<*> -> arg
            is ParameterizedType -> arg.rawType as? Class<*>
            else -> null
        }
    }

    /**
     * 探测目标对象的 int 方法。按 [names] 顺序尝试，第一个成功即返回。
     */
    fun probeIntMethod(target: Any, names: List<String>): Int? {
        val cache = methodCache.getOrPut(target.javaClass) { ConcurrentHashMap() }
        for (n in names) {
            val cached = cache[n]
            if (cached != null) {
                return try {
                    (cached.invoke(target) as? Number)?.toInt()
                } catch (_: Throwable) {
                    null
                }
            }
            try {
                val m = target.javaClass.getMethod(n)
                val v = m.invoke(target)
                if (v is Number) {
                    cache[n] = m
                    return v.toInt()
                }
            } catch (_: Throwable) {
                // try next
            }
        }
        return null
    }

    /**
     * 探测目标对象的 int 字段（含父类）。按 [names] 顺序尝试。
     */
    fun probeIntField(target: Any, names: List<String>): Int? {
        val normalized = names.map { normalize(it) }.toSet()
        val cls = target.javaClass
        val cache = fieldCache.getOrPut(cls) { ConcurrentHashMap() }
        cache.values.firstOrNull()?.let { f ->
            return try {
                f.isAccessible = true
                (f.get(target) as? Number)?.toInt()
            } catch (_: Throwable) {
                null
            }
        }
        var c: Class<*>? = cls
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.type != Int::class.javaPrimitiveType && f.type != Int::class.java) continue
                if (normalize(f.name) !in normalized) continue
                return try {
                    f.isAccessible = true
                    cache[f.name] = f
                    (f.get(target) as? Number)?.toInt()
                } catch (_: Throwable) {
                    null
                }
            }
            c = c.superclass
        }
        return null
    }

    /**
     * 判断类是否像 int 包装值类（如 ContainerId / SlotId / MapId）。
     *
     * 启发：
     * - 包前缀在 Minecraft 命名空间
     * - 简单名以 Id / Index / Slot 结尾，或为已知值类
     */
    fun isLikelyIntWrapper(cls: Class<*>): Boolean {
        val pkg = cls.`package`?.name ?: return false
        val isMcPkg = pkg.startsWith("net.minecraft.") || pkg.startsWith("org.bukkit.")
        if (!isMcPkg) return false
        val sn = cls.simpleName
        if (sn.matches(Regex("(?i).*(Id|Index|Slot)$"))) return true
        // Record 单 int 组件兜底
        val comps = recordComponents(cls)
        if (comps != null && comps.size == 1) {
            val t = comps[0].type
            if (t == Int::class.javaPrimitiveType || t == Int::class.java) return true
        }
        return false
    }

    /**
     * MapPatch 构造描述。
     *
     * 形状：4×int + 1×byte[]，参数顺序由 [intRoles] 给出。
     * intRoles 长度等于 4，按 ctor 实际参数顺序排列：
     * 元素是从 [START_X, START_Y, WIDTH, HEIGHT] 中的某个排列。
     */
    data class MapPatchCtor(
        val ctor: Constructor<*>,
        val argOrder: List<MapPatchArg>
    )

    /**
     * MapPatch 构造参数槽位。
     */
    enum class MapPatchArg { START_X, START_Y, WIDTH, HEIGHT, COLORS }

    /**
     * 扫描 MapPatch 类的构造函数，找形状匹配的 (4×int + byte[]) 签名。
     *
     * 参数名可读时按字典语义对齐；不可读则返回默认顺序 (startX, startY, width, height, colors)。
     */
    fun findMapPatchCtor(cls: Class<*>): MapPatchCtor? {
        val intType = Int::class.javaPrimitiveType
        val candidates = cls.declaredConstructors.filter { ctor ->
            val pts = ctor.parameterTypes
            if (pts.size != 5) return@filter false
            val intCount = pts.count { it == intType || it == Int::class.java }
            val byteArrCount = pts.count { it == ByteArray::class.java }
            intCount == 4 && byteArrCount == 1
        }
        if (candidates.isEmpty()) return null

        for (ctor in candidates) {
            val pts = ctor.parameterTypes
            val params = ctor.parameters
            val order = mutableListOf<MapPatchArg>()
            var allNamed = true
            var startXSeen = false
            var startYSeen = false
            var widthSeen = false
            var heightSeen = false

            for (i in pts.indices) {
                if (pts[i] == ByteArray::class.java) {
                    order.add(MapPatchArg.COLORS)
                    continue
                }
                val role = if (params[i].isNamePresent) roleOf(params[i].name) else ArgRole.UNKNOWN
                when (role) {
                    ArgRole.START_X -> { order.add(MapPatchArg.START_X); startXSeen = true }
                    ArgRole.START_Y -> { order.add(MapPatchArg.START_Y); startYSeen = true }
                    ArgRole.WIDTH -> { order.add(MapPatchArg.WIDTH); widthSeen = true }
                    ArgRole.HEIGHT -> { order.add(MapPatchArg.HEIGHT); heightSeen = true }
                    else -> { allNamed = false; order.add(MapPatchArg.START_X) }
                }
            }
            if (allNamed && startXSeen && startYSeen && widthSeen && heightSeen) {
                ctor.isAccessible = true
                return MapPatchCtor(ctor, order)
            }
        }

        // 名字不可读 → 默认顺序 (startX, startY, width, height, colors)
        val first = candidates.first()
        first.isAccessible = true
        val pts = first.parameterTypes
        val defaults = mutableListOf<MapPatchArg>()
        val intQueue = ArrayDeque(listOf(MapPatchArg.START_X, MapPatchArg.START_Y, MapPatchArg.WIDTH, MapPatchArg.HEIGHT))
        for (pt in pts) {
            defaults.add(if (pt == ByteArray::class.java) MapPatchArg.COLORS else intQueue.removeFirst())
        }
        return MapPatchCtor(first, defaults)
    }

    /**
     * 用 4 个 int 值 + colors 构造 MapPatch，按 [MapPatchCtor.argOrder] 排列。
     */
    fun buildMapPatchArgs(
        descriptor: MapPatchCtor,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        colors: ByteArray
    ): Array<Any> {
        return descriptor.argOrder.map { arg ->
            when (arg) {
                MapPatchArg.START_X -> startX
                MapPatchArg.START_Y -> startY
                MapPatchArg.WIDTH -> width
                MapPatchArg.HEIGHT -> height
                MapPatchArg.COLORS -> colors
            }
        }.toTypedArray()
    }

    /**
     * 校验已构造 packet 的关键 int 字段。
     *
     * @param packet 反射构造出的对象
     * @param expected 期望键名（规范化后）→ 期望值
     * @return 不匹配的字段诊断列表（空列表 = 全部通过）
     */
    fun verifyPacketFields(packet: Any, expected: Map<String, Int>): List<String> {
        if (expected.isEmpty()) return emptyList()
        val mismatches = mutableListOf<String>()
        val matched = mutableSetOf<String>()
        var c: Class<*>? = packet.javaClass
        while (c != null && c != Any::class.java) {
            for (f in c.declaredFields) {
                if (f.type != Int::class.javaPrimitiveType && f.type != Int::class.java) continue
                val key = expected.keys.firstOrNull { normalize(f.name) == normalize(it) } ?: continue
                if (key in matched) continue
                try {
                    f.isAccessible = true
                    val actual = (f.get(packet) as? Number)?.toInt()
                    val want = expected[key] ?: continue
                    if (actual != want) {
                        mismatches.add("field=${f.name} expected=$want actual=$actual")
                    }
                    matched.add(key)
                } catch (_: Throwable) {
                    // 忽略不可访问字段
                }
            }
            c = c.superclass
        }
        return mismatches
    }

    /**
     * 已解析 ctor 的缓存条目。
     */
    data class ResolvedCtor(
        val ctor: Constructor<*>,
        val argBuilder: (Map<ArgRole, Any?>) -> Array<Any>
    )

    private val methodCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, Method>>()
    private val fieldCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, Field>>()
}
