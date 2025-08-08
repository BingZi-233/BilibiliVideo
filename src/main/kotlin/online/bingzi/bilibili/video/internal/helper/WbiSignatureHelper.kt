package online.bingzi.bilibili.video.internal.helper

import java.security.MessageDigest
import java.net.URLEncoder

/**
 * WBI签名助手
 * 
 * 用于生成Bilibili API的WBI签名，这是一种Web端风险控制机制。
 * WBI签名需要在请求参数中添加w_rid和wts字段。
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object WbiSignatureHelper {
    
    /**
     * 密钥编码表
     * 用于重新排列img_key和sub_key生成mixin_key
     */
    private val MIXIN_KEY_ENC_TAB = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )
    
    /**
     * 需要过滤的字符
     */
    private val FILTER_CHARS = charArrayOf('!', '\'', '(', ')', '*')
    
    /**
     * 生成混合密钥
     * 
     * @param imgKey 从nav接口获取的img_key
     * @param subKey 从nav接口获取的sub_key
     * @return 生成的32位mixin_key
     */
    fun generateMixinKey(imgKey: String, subKey: String): String {
        // 拼接原始密钥
        val rawWbiKey = imgKey + subKey
        
        // 根据编码表重新排列字符
        val mixinKey = StringBuilder()
        for (i in 0..31) {
            mixinKey.append(rawWbiKey[MIXIN_KEY_ENC_TAB[i]])
        }
        
        return mixinKey.toString()
    }
    
    /**
     * 对请求参数进行WBI签名
     * 
     * @param params 原始请求参数Map
     * @param imgKey 从nav接口获取的img_key
     * @param subKey 从nav接口获取的sub_key
     * @return 添加了w_rid和wts的新参数Map
     */
    fun signParams(params: Map<String, Any>, imgKey: String, subKey: String): Map<String, Any> {
        // 生成mixin_key
        val mixinKey = generateMixinKey(imgKey, subKey)
        
        // 添加时间戳
        val wts = System.currentTimeMillis() / 1000
        val newParams = params.toMutableMap()
        newParams["wts"] = wts
        
        // 按key字母顺序排序参数
        val sortedParams = newParams.toSortedMap()
        
        // 构建查询字符串
        val queryString = buildQueryString(sortedParams)
        
        // 计算w_rid (MD5)
        val wRid = md5(queryString + mixinKey)
        
        // 添加w_rid到参数中
        sortedParams["w_rid"] = wRid
        
        return sortedParams
    }
    
    /**
     * 构建查询字符串
     * 
     * @param params 排序后的参数Map
     * @return URL编码后的查询字符串
     */
    private fun buildQueryString(params: Map<String, Any>): String {
        return params.entries.joinToString("&") { (key, value) ->
            val filteredValue = filterValue(value.toString())
            "$key=${urlEncode(filteredValue)}"
        }
    }
    
    /**
     * 过滤参数值中的特殊字符
     * 
     * @param value 原始参数值
     * @return 过滤后的参数值
     */
    private fun filterValue(value: String): String {
        return value.filterNot { it in FILTER_CHARS }
    }
    
    /**
     * URL编码
     * 
     * @param value 需要编码的字符串
     * @return URL编码后的字符串
     */
    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
    }
    
    /**
     * 计算MD5哈希值
     * 
     * @param input 输入字符串
     * @return MD5哈希值（小写十六进制）
     */
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 从图片URL中提取密钥
     * 
     * @param imgUrl nav接口返回的wbi_img.img_url
     * @return 提取的img_key
     */
    fun extractImgKey(imgUrl: String): String {
        // 提取文件名（不含扩展名）作为key
        val filename = imgUrl.substringAfterLast('/').substringBeforeLast('.')
        return filename
    }
    
    /**
     * 从子URL中提取密钥
     * 
     * @param subUrl nav接口返回的wbi_img.sub_url
     * @return 提取的sub_key
     */
    fun extractSubKey(subUrl: String): String {
        // 提取文件名（不含扩展名）作为key
        val filename = subUrl.substringAfterLast('/').substringBeforeLast('.')
        return filename
    }
}