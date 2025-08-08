package online.bingzi.bilibili.video.internal.helper

import online.bingzi.bilibili.video.internal.config.SettingConfig
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.MapMeta
import org.bukkit.map.MapCanvas
import org.bukkit.map.MapRenderer
import org.bukkit.map.MapView
import taboolib.common.platform.function.submit
import taboolib.common.util.unsafeLazy
import taboolib.library.reflex.Reflex.Companion.getProperty
import taboolib.library.reflex.Reflex.Companion.invokeConstructor
import taboolib.library.reflex.Reflex.Companion.invokeMethod
import taboolib.library.reflex.Reflex.Companion.setProperty
import taboolib.library.reflex.Reflex.Companion.unsafeInstance
import taboolib.library.xseries.XMaterial
import taboolib.module.nms.*
import taboolib.platform.util.ItemBuilder
import taboolib.platform.util.buildItem
import taboolib.platform.util.modifyMeta
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.imageio.ImageIO

/**
 * 创建地图画的扩展函数（堵塞方式）
 *
 * @param url 图像地址，字符串格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 * @return 返回创建的NMSMap对象
 */
fun buildMap(
    url: String,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
): NMSMap {
    // 使用URL打开输入流并读取图像，之后缩放到指定大小
    return NMSMap(URL(url).openStream().use { ImageIO.read(it) }.zoomed(width, height), hand, builder)
}

/**
 * 创建地图画的扩展函数（异步方式）
 *
 * @param url 图像地址，URL格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 * @return 返回一个CompletableFuture，最终结果为创建的NMSMap对象
 */
fun buildMap(
    url: URL,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
): CompletableFuture<NMSMap> {
    // 通过CompletableFuture异步处理图像读取和地图创建
    return CompletableFuture.supplyAsync {
        NMSMap(url.openStream().use { ImageIO.read(it) }.zoomed(width, height), hand, builder)
    }
}

/**
 * 创建地图画的扩展函数（堵塞方式）
 *
 * @param file 图像文件，File格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 * @return 返回创建的NMSMap对象
 */
fun buildMap(
    file: File,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
): NMSMap {
    // 读取图像文件并缩放到指定大小
    return NMSMap(ImageIO.read(file).zoomed(width, height), hand, builder)
}

/**
 * 创建地图画的扩展函数（堵塞方式）
 *
 * @param image 图像对象，BufferedImage格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 * @return 返回创建的NMSMap对象
 */
fun buildMap(
    image: BufferedImage,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
): NMSMap {
    // 缩放图像并创建NMSMap对象
    return NMSMap(image.zoomed(width, height), hand, builder)
}

/**
 * 玩家发送地图画的扩展函数（异步方式）
 *
 * @param url 图像地址，字符串格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 */
fun Player.sendMap(
    url: String,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
) {
    // 异步构建地图并发送给玩家
    buildMap(URL(url), hand, width, height, builder).thenAccept { it.sendTo(this) }
}

/**
 * 玩家发送地图画的扩展函数（异步方式）
 *
 * @param url 图像地址，URL格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 */
fun Player.sendMap(
    url: URL,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
) {
    // 异步构建地图并发送给玩家
    buildMap(url, hand, width, height, builder).thenAccept { it.sendTo(this) }
}

/**
 * 玩家发送地图画的扩展函数（异步方式）
 *
 * @param file 图像文件，File格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 */
fun Player.sendMap(
    file: File,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
) {
    // 构建地图并直接发送给玩家
    buildMap(file, hand, width, height, builder).sendTo(this)
}

/**
 * 玩家发送地图画的扩展函数（异步方式）
 *
 * @param image 图像对象，BufferedImage格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param width 图像宽度，默认为128
 * @param height 图像高度，默认为128
 * @param builder 自定义物品构建器的扩展函数
 */
fun Player.sendMap(
    image: BufferedImage,
    hand: NMSMap.Hand = NMSMap.Hand.MAIN,
    width: Int = 128,
    height: Int = 128,
    builder: ItemBuilder.() -> Unit = {}
) {
    // 构建地图并直接发送给玩家
    buildMap(image, hand, width, height, builder).sendTo(this)
}

/**
 * 调整图片分辨率的扩展函数
 *
 * 地图最佳显示分辨率为128*128
 *
 * @param width 目标宽度，默认为128
 * @param height 目标高度，默认为128
 * @return 返回缩放后的BufferedImage对象
 */
fun BufferedImage.zoomed(width: Int = 128, height: Int = 128): BufferedImage {
    // 创建一个新的BufferedImage对象以保存缩放后的图像
    val tag = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    // 绘制原图到新图像上，完成缩放
    tag.graphics.drawImage(this, 0, 0, width, height, null)
    return tag
}

/**
 * NMSMap类用于处理地图的创建和发送
 *
 * @param image 地图的图像数据，BufferedImage格式
 * @param hand 地图的手持方向，默认为主手（MAIN）
 * @param builder 自定义物品构建器的扩展函数
 */
class NMSMap(val image: BufferedImage, var hand: Hand = Hand.MAIN, val builder: ItemBuilder.() -> Unit = {}) {

    /**
     * 地图的手持方向枚举
     */
    enum class Hand {
        MAIN, OFF
    }

    companion object {
        // NMS类的引用
        val classPacketPlayOutSetSlot = nmsClass("PacketPlayOutSetSlot")
        val classPacketPlayOutMap = nmsClass("PacketPlayOutMap")
        val classCraftItemStack = obcClass("inventory.CraftItemStack")
        val classMapIcon by unsafeLazy { nmsClass("MapIcon") }

        /**
         * 提供用于寻找地图数据类的引用
         */
        val classMapData: Class<*> by unsafeLazy {
            try {
                // 尝试找Spigot的WorldMap.b
                val worldMap = if (MinecraftVersion.isEqual(MinecraftVersion.V1_21)) "c" else "b"
                Class.forName("net.minecraft.world.level.saveddata.maps.WorldMap\$$worldMap")
            } catch (e: ClassNotFoundException) {
                // 没有找到Spigot的WorldMap.b，尝试找Paper的MapItemSavedData.MapPatch
                Class.forName("net.minecraft.world.level.saveddata.maps.MapItemSavedData\$MapPatch")
            }
        }

        // 高版本兼容性处理
        val classMapId: Class<*> by unsafeLazy { Class.forName("net.minecraft.world.level.saveddata.maps.MapId") }
    }

    // 地图渲染器，用于将图像绘制到地图上
    val mapRenderer = object : MapRenderer() {

        var rendered = false

        override fun render(mapView: MapView, mapCanvas: MapCanvas, player: Player) {
            // 确保只渲染一次
            if (rendered) {
                return
            }
            // 绘制图像到地图画布上
            mapCanvas.drawImage(0, 0, image)
            rendered = true
        }
    }

    // 创建地图视图
    val mapView by unsafeLazy {
        val mapView = Bukkit.createMap(Bukkit.getWorlds()[0])
        mapView.addRenderer(mapRenderer)
        mapView
    }

    // 创建地图物品
    val mapItem by unsafeLazy {
        val map = if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_13)) {
            buildItem(XMaterial.FILLED_MAP, builder)
        } else {
            buildItem(XMaterial.FILLED_MAP) {
                damage = mapView.invokeMethod<Short>("getId")!!.toInt()
                builder(this)
            }
        }
        if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_13)) {
            map.modifyMeta<MapMeta> { mapView = this@NMSMap.mapView }
        } else {
            map
        }
    }

    /**
     * 将地图发送给指定玩家
     *
     * @param player 接收地图的玩家
     */
    fun sendTo(player: Player) {
        submit(delay = 3) {
            // 获取玩家的容器信息
            val container = if (MinecraftVersion.isUniversal) {
                player.getProperty<Any>("entity/inventoryMenu")
            } else {
                player.getProperty<Any>("entity/defaultContainer")
            }!!
            // 获取窗口ID
            val windowsId = if (MinecraftVersion.isUniversal) {
                container.getProperty<Int>("containerId")
            } else {
                container.getProperty<Int>("windowId")
            }!!
            // 构造NMS物品
            val nmsItem = classCraftItemStack.invokeMethod<Any>("asNMSCopy", mapItem, isStatic = true)
            // 创建并设置发送的包
            val itemPacket = classPacketPlayOutSetSlot.unsafeInstance().also {
                if (MinecraftVersion.isUniversal) {
                    it.setProperty("containerId", windowsId)
                    it.setProperty("stateId", 1)
                    it.setProperty("slot", getMainHandSlot(player))
                    it.setProperty("itemStack", nmsItem)
                } else {
                    it.setProperty("a", windowsId)
                    it.setProperty("b", getMainHandSlot(player))
                    it.setProperty("c", nmsItem)
                }
            }
            // 根据配置选择异步或同步发送包
            if (SettingConfig.sendMapAsync) {
                player.sendPacket(itemPacket)
            } else {
                player.sendPacketBlocking(itemPacket)
            }
            // 获取地图渲染的字节数组
            val buffer = mapView.invokeMethod<Any>("render", player)!!.getProperty<ByteArray>("buffer")
            // 创建地图数据包
            val packet = createMapPacket(mapView.id, buffer!!)
            // 根据配置选择异步或同步发送包
            if (SettingConfig.sendMapAsync) {
                player.sendPacket(packet)
            } else {
                player.sendPacketBlocking(packet)
            }
        }
    }

    /**
     * 创建地图数据包
     *
     * @param mapId 地图ID
     * @param buffer 地图数据缓冲区
     * @return 地图数据包
     */
    private fun createMapPacket(mapId: Int, buffer: ByteArray): Any {
        val packet = classPacketPlayOutMap.unsafeInstance()
        
        if (MinecraftVersion.isUniversal) {
            // 1.17+ 版本
            if (MinecraftVersion.isHigherOrEqual(MinecraftVersion.V1_21)) {
                val mapIdObj = classMapId.invokeConstructor(mapId)
                packet.setProperty("mapId", mapIdObj)
            } else {
                packet.setProperty("mapId", mapId)
            }
            packet.setProperty("scale", 0.toByte())
            packet.setProperty("locked", false)
            
            // 创建空的图标列表
            packet.setProperty("decorations", emptyList<Any>())
            
            // 创建地图数据
            val mapData = classMapData.invokeConstructor(0, 0, 128, 128, buffer)
            packet.setProperty("colorPatch", mapData)
        } else {
            // 1.16 及以下版本
            packet.setProperty("a", mapId)
            packet.setProperty("b", 0.toByte())
            packet.setProperty("c", false)
            packet.setProperty("d", emptyList<Any>())
            
            val mapData = classMapData.invokeConstructor(0, 0, 128, 128, buffer)
            packet.setProperty("e", mapData)
        }
        
        return packet
    }

    /**
     * 获取玩家主手或副手的槽位
     *
     * @param player 玩家对象
     * @return 返回主手或副手的槽位索引
     */
    private fun getMainHandSlot(player: Player): Int {
        // 判断手持方向并返回对应槽位
        if (hand == Hand.OFF) {
            return 45
        }
        return player.inventory.heldItemSlot + 36
    }
}