package online.bingzi.bilibili.video.internal.helper

import java.security.KeyFactory
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * Cookie refresh helper
 * Cookie刷新帮助工具
 *
 * 该对象提供了生成与时间戳相关的加密路径的方法，主要用于刷新Cookie的过程。
 * 通过使用RSA非对称加密算法对时间戳进行加密，以确保数据的安全性。
 *
 * @constructor 创建一个空的Cookie刷新帮助工具
 */
object CookieRefreshHelper {
    /**
     * Get correspond path
     * 生成CorrespondPath算法
     *
     * 此方法根据传入的时间戳生成一个加密的路径字符串，主要用于刷新Cookie。
     *
     * @param timestamp 时间戳，类型为Long，通常是当前时间的毫秒数。
     *                  该参数用于生成唯一的加密字符串。
     * @return 返回一个String类型的加密路径，经过加密算法处理后的结果。
     */
    fun getCorrespondPath(timestamp: Long): String {
        // 定义一个PEM编码的RSA公钥字符串
        val publicKeyPEM = """
        -----BEGIN PUBLIC KEY-----
        MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg
        Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71
        nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40
        JNrRuoEUXpabUzGB8QIDAQAB
        -----END PUBLIC KEY-----
    """.trimIndent()

        // 将PEM编码的公钥转为公钥对象
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(
            X509EncodedKeySpec(
                Base64.getDecoder().decode(
                    publicKeyPEM
                        .replace("-----BEGIN PUBLIC KEY-----", "") // 去除公钥头部
                        .replace("-----END PUBLIC KEY-----", "")   // 去除公钥尾部
                        .replace("\n", "")                          // 去除换行符
                        .trim()                                   // 去除空白字符
                )
            )
        )

        // 初始化Cipher对象，用于RSA加密，并设置填充方式
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding").apply {
            init(
                Cipher.ENCRYPT_MODE, // 设置为加密模式
                publicKey,           // 使用上面生成的公钥
                OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT) // 设置填充参数
            )
        }

        // 对字符串"refresh_$timestamp"进行加密并转换为十六进制字符串
        return cipher.doFinal("refresh_$timestamp".toByteArray()).joinToString("") { "%02x".format(it) }
    }
}