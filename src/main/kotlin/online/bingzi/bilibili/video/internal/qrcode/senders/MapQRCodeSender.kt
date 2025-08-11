package online.bingzi.bilibili.video.internal.qrcode.senders

import online.bingzi.bilibili.video.internal.qrcode.QRCodeSender
import taboolib.common.platform.ProxyPlayer
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import taboolib.platform.BukkitPlugin
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * 地图二维码发送器
 * 将二维码渲染成Minecraft地图物品发送给玩家
 */
class MapQRCodeSender : QRCodeSender {
    
    companion object {
        private const val MAP_SIZE = 128 // Minecraft地图默认大小
    }
    
    override fun sendQRCode(
        player: ProxyPlayer,
        qrCodeImage: BufferedImage,
        title: String,
        description: String
    ): Boolean {
        return try {
            // 检查是否为Bukkit环境
            val bukkitPlayer = player.cast<org.bukkit.entity.Player>()
            if (bukkitPlayer == null) {
                console().sendWarn("qrcodeMapPlayerNotBukkit", player.name)
                return false
            }
            
            // 缩放二维码图片到地图大小
            val scaledImage = scaleImageToMapSize(qrCodeImage)
            
            // 创建地图
            val map = org.bukkit.Bukkit.createMap(bukkitPlayer.world)
            map.isUnlimitedTracking = true
            
            // 渲染二维码到地图
            val mapRenderer = QRCodeMapRenderer(scaledImage, title, description)
            map.renderers.clear()
            map.addRenderer(mapRenderer)
            
            // 创建地图物品
            val mapItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.FILLED_MAP)
            val mapMeta = mapItem.itemMeta as org.bukkit.inventory.meta.MapMeta
            mapMeta.setDisplayName("§a$title")
            mapMeta.lore = listOf(
                "§7$description",
                "§7使用地图查看二维码",
                "§8由BilibiliVideo生成"
            )
            mapMeta.mapView = map
            mapItem.itemMeta = mapMeta
            
            // 发送给玩家
            bukkitPlayer.inventory.addItem(mapItem)
            player.sendInfo("qrcodeMapSent", title)
            
            console().sendInfo("qrcodeMapCreated", player.name, map.id.toString())
            true
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeMapFailed", player.name, e.message ?: "")
            false
        }
    }
    
    override fun getSenderName(): String = "地图发送器"
    
    override fun isAvailable(player: ProxyPlayer?): Boolean {
        return try {
            // 检查是否在Bukkit环境
            Class.forName("org.bukkit.Bukkit")
            BukkitPlugin.getInstance() != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 缩放图片到地图尺寸
     */
    private fun scaleImageToMapSize(originalImage: BufferedImage): BufferedImage {
        val scaledImage = BufferedImage(MAP_SIZE, MAP_SIZE, BufferedImage.TYPE_INT_RGB)
        val g2d = scaledImage.createGraphics()
        
        // 设置高质量渲染
        g2d.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        
        // 白色背景
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, MAP_SIZE, MAP_SIZE)
        
        // 计算居中位置
        val imageSize = minOf(MAP_SIZE - 16, originalImage.width) // 留出边距
        val x = (MAP_SIZE - imageSize) / 2
        val y = (MAP_SIZE - imageSize) / 2
        
        // 绘制缩放后的二维码
        g2d.drawImage(originalImage, x, y, imageSize, imageSize, null)
        g2d.dispose()
        
        return scaledImage
    }
}

/**
 * 二维码地图渲染器
 */
private class QRCodeMapRenderer(
    private val qrCodeImage: BufferedImage,
    private val title: String,
    private val description: String
) : org.bukkit.map.MapRenderer() {
    
    private var rendered = false
    
    override fun render(
        map: org.bukkit.map.MapView,
        canvas: org.bukkit.map.MapCanvas,
        player: org.bukkit.entity.Player
    ) {
        if (rendered) return
        
        try {
            // 将BufferedImage转换为MapCanvas可用的格式
            for (x in 0 until minOf(qrCodeImage.width, 128)) {
                for (y in 0 until minOf(qrCodeImage.height, 128)) {
                    val rgb = qrCodeImage.getRGB(x, y)
                    val color = Color(rgb)
                    
                    // 转换为最接近的Minecraft地图颜色
                    val mapColor = getClosestMapColor(color)
                    canvas.setPixel(x, y, mapColor)
                }
            }
            
            rendered = true
            
        } catch (e: Exception) {
            console().sendWarn("qrcodeMapRenderFailed", e.message ?: "")
        }
    }
    
    /**
     * 获取最接近的Minecraft地图颜色
     */
    private fun getClosestMapColor(color: Color): Byte {
        // 简单的黑白二值化处理
        val brightness = (color.red + color.green + color.blue) / 3
        return if (brightness > 127) {
            // 白色 -> 雪白色
            org.bukkit.map.MapPalette.matchColor(Color.WHITE)
        } else {
            // 黑色 -> 黑色
            org.bukkit.map.MapPalette.matchColor(Color.BLACK)
        }
    }
}