package online.bingzi.bilibili.video.internal.helper

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
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