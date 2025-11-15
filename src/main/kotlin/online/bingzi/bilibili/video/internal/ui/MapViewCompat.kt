package online.bingzi.bilibili.video.internal.ui

import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapView

/**
 * MapView 与物品元数据之间的兼容封装。
 *
 * 1.13+ 存在 setMapView(MapView)，而 1.12 使用 setMapId(short)。
 * 这里通过反射在运行时适配，避免直接依赖特定版本 API。
 */
object MapViewCompat {

    fun setMapOnMeta(meta: ItemMeta, mapView: MapView) {
        val mapMeta = meta as? MapMeta
        if (mapMeta != null) {
            if (trySetMapView(mapMeta, mapView)) {
                return
            }
            if (trySetMapId(mapMeta, mapView)) {
                return
            }
        }

        // 最后兜底：极端情况下 ItemMeta 不是 MapMeta，使用旧的反射方案
        tryLegacyReflection(meta, mapView)
    }

    private fun trySetMapView(mapMeta: MapMeta, mapView: MapView): Boolean {
        return try {
            mapMeta.mapView = mapView
            true
        } catch (_: NoSuchMethodError) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    private fun trySetMapId(mapMeta: MapMeta, mapView: MapView): Boolean {
        val id = resolveMapId(mapView)
        return try {
            mapMeta.setMapId(id)
            true
        } catch (_: NoSuchMethodError) {
            // 旧版本 MapMeta#setMapId 可能使用 short 签名
            try {
                val methodShort = MapMeta::class.java.getMethod("setMapId", java.lang.Short.TYPE)
                methodShort.invoke(mapMeta, id.toShort())
                true
            } catch (_: Throwable) {
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun tryLegacyReflection(meta: ItemMeta, mapView: MapView) {
        val id = resolveMapId(mapView)
        try {
            val methodInt = meta.javaClass.getDeclaredMethod("setMapId", Integer.TYPE)
            methodInt.isAccessible = true
            methodInt.invoke(meta, id)
            return
        } catch (_: Throwable) {
            // ignore and try short
        }
        try {
            val methodShort = meta.javaClass.getDeclaredMethod("setMapId", java.lang.Short.TYPE)
            methodShort.isAccessible = true
            methodShort.invoke(meta, id.toShort())
        } catch (_: Throwable) {
            // 如果兜底仍失败，则交给服务器默认行为
        }
    }

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
}
