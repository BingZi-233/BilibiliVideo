package online.bingzi.bilibili.video.internal.ui

import org.bukkit.Material

/**
 * 地图物品在不同 Minecraft 版本中的兼容处理。
 *
 * - 1.13+ 使用 FILLED_MAP
 * - 1.12 及以下使用 MAP
 */
object MapMaterialCompat {

    val mapMaterial: Material by lazy {
        try {
            Material.valueOf("FILLED_MAP")
        } catch (_: IllegalArgumentException) {
            Material.valueOf("MAP")
        }
    }
}

