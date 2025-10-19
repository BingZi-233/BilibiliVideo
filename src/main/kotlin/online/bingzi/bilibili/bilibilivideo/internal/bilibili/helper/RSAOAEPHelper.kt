package online.bingzi.bilibili.bilibilivideo.internal.bilibili.helper

import taboolib.common.platform.function.warning
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher

/**
 * RSA-OAEP 加密工具类
 * 用于 Bilibili Cookie 刷新流程中生成 CorrespondPath
 *
 * @author BingZi-233
 * @since 1.0.0
 */
object RSAOAEPHelper {

    /**
     * Bilibili 官方公钥 (PEM 格式)
     * 用于 Cookie 刷新时的 RSA-OAEP 加密
     */
    private const val PUBLIC_KEY_PEM = """
-----BEGIN PUBLIC KEY-----
MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg
Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71
nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40
JNrRuoEUXpabUzGB8QIDAQAB
-----END PUBLIC KEY-----
"""

    /**
     * 生成 CorrespondPath
     * 将时间戳使用 RSA-OAEP 加密后转换为小写十六进制字符串
     *
     * @param timestamp 时间戳(毫秒)
     * @return 加密后的小写十六进制字符串,失败返回空字符串
     */
    fun generateCorrespondPath(timestamp: Long): String {
        return try {
            // 1. 拼接待加密字符串
            val data = "refresh_$timestamp"

            // 2. RSA-OAEP 加密
            val encrypted = encryptRSAOAEP(data.toByteArray(Charsets.UTF_8))

            // 3. 转换为小写十六进制字符串
            bytesToHex(encrypted)
        } catch (e: Exception) {
            warning("生成 CorrespondPath 失败: ${e.message}")
            ""
        }
    }

    /**
     * 使用 RSA-OAEP 加密数据
     *
     * @param data 待加密的字节数组
     * @return 加密后的字节数组
     * @throws Exception 加密失败时抛出异常
     */
    private fun encryptRSAOAEP(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, getPublicKey())
        return cipher.doFinal(data)
    }

    /**
     * 解析 PEM 格式公钥
     * 将 PEM 格式的公钥字符串解析为 PublicKey 对象
     *
     * @return RSA 公钥对象
     * @throws Exception 解析失败时抛出异常
     */
    private fun getPublicKey(): java.security.PublicKey {
        // 1. 移除 PEM 头尾
        val publicKeyPEM = PUBLIC_KEY_PEM
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "") // 移除所有空白字符

        // 2. Base64 解码
        val decoded = Base64.getDecoder().decode(publicKeyPEM)

        // 3. 生成 PublicKey
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * 字节数组转小写十六进制字符串
     *
     * @param bytes 字节数组
     * @return 小写十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
