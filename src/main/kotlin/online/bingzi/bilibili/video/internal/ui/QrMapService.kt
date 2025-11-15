package online.bingzi.bilibili.video.internal.ui

import online.bingzi.bilibili.video.internal.util.QrCodeGenerator
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

/**
 * 二维码地图物品创建服务。
 *
 * 负责根据二维码 URL 创建带有自定义渲染器的地图物品，由调用方决定如何发给玩家。
 */
object QrMapService {

    fun createQrMapItem(player: Player, qrUrl: String): ItemStack {
        val image = QrCodeGenerator.generateQrImage(qrUrl, 128)

        val mapView = Bukkit.createMap(player.world).apply {
            renderers.forEach { removeRenderer(it) }
            addRenderer(QrMapRenderer(image))
            try {
                // 兼容旧版本：部分版本无 setTrackingPosition 方法
                this::class.java.getMethod("setTrackingPosition", Boolean::class.javaPrimitiveType)
                    .invoke(this, false)
            } catch (_: Throwable) {
                // ignore
            }
            try {
                // 某些旧版本不存在该属性，使用 try-catch 兼容
                this::class.java.getMethod("setUnlimitedTracking", Boolean::class.javaPrimitiveType)
                    .invoke(this, false)
            } catch (_: Throwable) {
                // ignore
            }
        }

        val item = ItemStack(MapMaterialCompat.mapMaterial)
        val meta = item.itemMeta
        if (meta != null) {
            MapViewCompat.setMapOnMeta(meta, mapView)
            meta.setDisplayName("§b扫描地图上的二维码绑定 B 站")
            item.itemMeta = meta
        }

        // 兼容 1.12 及以下：使用耐久度作为地图 ID 绑定
        if (item.type.name == "MAP") {
            try {
                val idMethod = mapView.javaClass.getMethod("getId")
                val idResult = idMethod.invoke(mapView)
                val id = (idResult as? Number)?.toInt() ?: 0

                val durabilityMethod =
                    item.javaClass.getMethod("setDurability", java.lang.Short.TYPE)
                durabilityMethod.invoke(item, id.toShort())
            } catch (_: Throwable) {
                // ignore，交给服务器默认逻辑
            }
        }

        return item
    }
}
