package online.bingzi.bilibili.video.internal.helper

import online.bingzi.bilibili.video.internal.config.SettingConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import taboolib.common.platform.ProxyPlayer
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.MinecraftVersion
import taboolib.platform.util.ItemBuilder
import taboolib.platform.util.buildItem
import taboolib.platform.util.isNotAir
import taboolib.type.BukkitEquipment
import java.awt.image.BufferedImage

// 扩展 ProxyPlayer 类，提供发送地图功能
fun ProxyPlayer.sendMap(image: BufferedImage, builder: ItemBuilder.() -> Unit = {}) {
    // 检查是否启用虚拟化配置
    if (SettingConfig.virtualization) {
        // 调用兼容版本的发送地图方法
        this.sendMapVersionCompatible(image, builder = builder)
    } else {
        // 将 ProxyPlayer 转换为 Player，并构建地图物品
        this.castSafely<Player>()?.buildMapItem(image, builder = builder)
    }
}

// 私有扩展函数，使用兼容版本发送地图
private fun ProxyPlayer.sendMapVersionCompatible(
    image: BufferedImage, // 要发送的地图图像
    hand: NMSMap.Hand = NMSMap.Hand.MAIN, // 发送时使用的手（主手或副手），默认主手
    width: Int = 128, // 地图宽度，默认128
    height: Int = 128, // 地图高度，默认128
    builder: ItemBuilder.() -> Unit = {} // 可选的构建器，用于自定义物品属性
) {
    // 获取当前玩家
    Bukkit.getPlayer(this.uniqueId)?.let {
        // 根据 Minecraft 版本选择发送地图的方式
        when (MinecraftVersion.major) {
            in MinecraftVersion.V1_18..MinecraftVersion.V1_20 -> {
                // 发送适用于1.18到1.20的地图
                buildMap(image, hand, width, height, builder).sendTo(it)
            }
            in MinecraftVersion.V1_8..MinecraftVersion.V1_17 -> {
                // 发送适用于1.8到1.17的地图
                it.sendMap(image, hand, width, height, builder)
            }
            else -> {
                // 发送其他版本的地图
                it.sendMap(image, hand, width, height, builder)
            }
        }
    }
}

// 私有扩展函数，为玩家构建地图物品
private fun Player.buildMapItem(image: BufferedImage, builder: ItemBuilder.() -> Unit = {}) {
    // 获取玩家主手中的物品
    val handItem = BukkitEquipment.HAND.getItem(this)
    // 检查主手是否为空，如果不为空则提示并返回
    if (handItem.isNotAir()) {
        this.sendMessage("请清空主手物品")
        return
    }
    // 创建地图渲染器
    val mapRenderer = object : MapRenderer() {
        var rendered = false // 标记是否已经渲染过

        // 渲染地图
        override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player) {
            // 如果已经渲染过，直接返回
            if (rendered) {
                return
            }
            // 在画布上绘制图像
            mapCanvas.drawImage(0, 0, image)
            rendered = true // 更新渲染标记
        }
    }
    // 懒加载地图视图
    val mapView by unsafeLazy {
        val mapView = Bukkit.createMap(Bukkit.getWorlds()[0]) // 创建地图视图
        mapView.addRenderer(mapRenderer) // 添加渲染器
        mapView // 返回地图视图
    }
    // 使用构建器创建地图物品
    val mapItem = buildItem(XMaterial.FILLED_MAP) {
        damage = mapView.invokeMethod<Short>("getId")!!.toInt() // 设置地图物品的损坏值为地图视图的ID
        builder(this) // 应用可选的构建器
    }
    // 将构建的地图物品设置到玩家的主手中
    BukkitEquipment.HAND.setItem(player, mapItem)
}