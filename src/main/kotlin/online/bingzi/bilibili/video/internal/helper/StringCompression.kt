package online.bingzi.bilibili.video.internal.helper

import online.bingzi.bilibili.video.internal.database.DatabaseType
import java.util.*
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * 扩展函数：对字符串进行压缩
 * 该函数使用Deflater算法对调用该函数的字符串进行压缩处理。
 * 在数据库类型为SQLITE时，返回原始字符串，不进行压缩。
 *
 * @param type 数据库类型，默认为DatabaseType.INSTANCE，
 *              主要用于决定是否进行压缩处理。
 * @return 返回压缩后的结果字符串，如果数据库类型为SQLITE，则返回原始字符串。
 */
fun String.compress(type: DatabaseType = DatabaseType.INSTANCE): String {
    // 检查数据库类型是否为SQLITE，如果是，则不进行压缩
    if (type == DatabaseType.SQLITE) {
        return this
    }

    // 将字符串转换为字节数组
    val input = this.toByteArray()
    // 创建Deflater对象以进行压缩
    val deflater = Deflater()
    // 设置要压缩的输入数据
    deflater.setInput(input)
    // 调用finish方法，指示输入数据已完成
    deflater.finish()

    // 创建一个字节数组作为输出缓冲区
    val buffer = ByteArray(input.size)
    // 执行压缩，并返回实际压缩后的字节数
    val compressedSize = deflater.deflate(buffer)

    // 将压缩后的字节数组编码为Base64字符串并返回
    return Base64.getEncoder().encodeToString(buffer.copyOf(compressedSize))
}

/**
 * 扩展函数：对压缩后的字符串进行解压
 * 该函数使用Inflater算法对调用该函数的字符串进行解压缩处理。
 * 在数据库类型为SQLITE时，返回原始压缩字符串，不进行解压。
 *
 * @param type 数据库类型，默认为DatabaseType.INSTANCE，
 *              主要用于决定是否进行解压处理。
 * @return 返回解压后的原始字符串，如果数据库类型为SQLITE，则返回原始压缩字符串。
 */
fun String.decompress(type: DatabaseType = DatabaseType.INSTANCE): String {
    // 检查数据库类型是否为SQLITE，如果是，则不进行解压
    if (type == DatabaseType.SQLITE) {
        return this
    }

    // 将Base64编码的字符串解码为字节数组
    val input = Base64.getDecoder().decode(this)
    // 创建Inflater对象以进行解压缩
    val inflater = Inflater()
    // 设置要解压缩的输入数据
    inflater.setInput(input)

    // 创建一个字节数组作为输出缓冲区
    val buffer = ByteArray(1024)
    // 使用StringBuilder构建解压后的字符串
    val output = StringBuilder()

    // 进行解压缩，直到所有数据均已处理完毕
    while (!inflater.finished()) {
        // 执行解压缩，并返回实际解压后的字节数
        val decompressedSize = inflater.inflate(buffer)
        // 将解压后的数据追加到输出字符串中
        output.append(java.lang.String(buffer, 0, decompressedSize))
    }

    // 返回解压后的完整字符串
    return output.toString()
}