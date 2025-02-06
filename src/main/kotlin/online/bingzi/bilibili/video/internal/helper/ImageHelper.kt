package online.bingzi.bilibili.video.internal.helper

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * 扩展函数：将字符串（URL）转换为二维码的BufferedImage。
 * 该函数使用ZXing库生成二维码，并返回一个包含二维码图像的BufferedImage对象。
 */
fun String.toBufferedImage(size: Int): BufferedImage {
    // 创建QRCodeWriter实例，用于生成二维码
    val writer = QRCodeWriter()

    // 生成二维码的位矩阵，指定格式为QR_CODE和图像的尺寸
    val bitMatrix = writer.encode(this, BarcodeFormat.QR_CODE, size, size)

    // 创建一个新的BufferedImage对象，指定宽度、高度和图像格式
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)

    // 获取图形上下文，用于绘制二维码
    val graphics = image.createGraphics()

    // 设置背景颜色为白色，并填充整个图像
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, size, size)

    // 设置绘制颜色为黑色
    graphics.color = Color.BLACK

    // 遍历位矩阵，绘制每个点
    for (x in 0 until size) {
        for (y in 0 until size) {
            // 如果位矩阵中的当前位置为true，则绘制一个黑色的像素
            if (bitMatrix.get(x, y)) {
                graphics.fillRect(x, y, 1, 1)
            }
        }
    }

    // 释放图形上下文资源
    graphics.dispose()

    // 返回生成的二维码图像
    return image
}

/**
 * 扩展函数：向玩家发送虚拟物品。
 * 该函数将创建一个数据包，将指定的物品发送到调用该函数的玩家的指定插槽中。
 *
 * @param itemStack 要发送的物品堆栈，类型为ItemStack。
 */
fun Player.sendVirtualItem(itemStack: ItemStack) {
    // 创建一个装载物品数据的包，使用ProtocolLibrary
    val packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.SET_SLOT)

    // 设置包中的数据：插槽ID，指定要放置物品的插槽编号为45
    packet.integers.write(1, 45)  // 插槽ID

    // 将物品堆栈放入包中，更新插槽的物品
    packet.itemModifier.write(0, itemStack)

    try {
        // 发送包给指定的玩家
        ProtocolLibrary.getProtocolManager().sendServerPacket(this, packet)
    } catch (e: Exception) {
        // 捕获并打印异常信息
        e.printStackTrace()
    }
}