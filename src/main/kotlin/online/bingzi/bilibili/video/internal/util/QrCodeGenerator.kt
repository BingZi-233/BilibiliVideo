package online.bingzi.bilibili.video.internal.util

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * 二维码生成工具。
 *
 * 只负责把字符串内容编码成黑白二维码图片，大小默认 128x128，
 * 方便直接绘制到 Minecraft 地图上。
 */
object QrCodeGenerator {

    fun generateQrImage(
        content: String,
        size: Int = 128,
        margin: Int = 1
    ): BufferedImage {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to margin
        )

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_RGB)
        val black = Color.BLACK.rgb
        val white = Color.WHITE.rgb

        for (x in 0 until size) {
            for (y in 0 until size) {
                image.setRGB(x, y, if (bitMatrix[x, y]) black else white)
            }
        }
        return image
    }
}

