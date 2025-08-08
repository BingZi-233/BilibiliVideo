package online.bingzi.bilibili.video.internal.helper

/**
 * AV/BV号转换助手
 * 
 * 用于在AV号（aid）和BV号（bvid）之间进行转换。
 * BV号是B站在2020年3月引入的新视频编号系统，用于替代原有的AV号。
 * 
 * @author BingZi-233
 * @since 2.0.0
 */
object BvidConverter {
    
    /**
     * 编码表
     * 用于base58编码的字符集
     */
    private const val TABLE = "FcwAPNKTMug3GV5Lj7EJnHpWsx4tb8haYeviqBz6rkCy12mUSDQX9RdoZf"
    
    /**
     * 反向映射表
     * 用于快速查找字符在编码表中的位置
     */
    private val REVERSE_TABLE = mutableMapOf<Char, Int>().apply {
        TABLE.forEachIndexed { index, char ->
            put(char, index)
        }
    }
    
    /**
     * 异或码
     */
    private const val XOR_CODE = 23442827791579L
    
    /**
     * 掩码
     */
    private const val MASK_CODE = 2251799813685247L
    
    /**
     * 最大AID（2^51）
     */
    private const val MAX_AID = 1L shl 51
    
    /**
     * Base58基数
     */
    private const val BASE = 58
    
    /**
     * 位置交换映射
     * BV号中某些位置的字符需要交换
     */
    private val SWAP_POSITIONS = arrayOf(
        intArrayOf(3, 9),
        intArrayOf(4, 7)
    )
    
    /**
     * 将AV号转换为BV号
     * 
     * @param aid AV号（纯数字）
     * @return BV号字符串
     */
    fun av2bv(aid: Long): String {
        // 初始化BV号数组
        val bytes = charArrayOf('B', 'V', '1', '0', '0', '0', '0', '0', '0', '0', '0', '0')
        
        // 计算中间值
        var tmp = (MAX_AID or aid) xor XOR_CODE
        
        // Base58编码
        var i = 11
        while (tmp > 0) {
            bytes[i] = TABLE[(tmp % BASE).toInt()]
            tmp /= BASE
            i--
        }
        
        // 交换指定位置的字符
        swapCharacters(bytes)
        
        return String(bytes)
    }
    
    /**
     * 将BV号转换为AV号
     * 
     * @param bvid BV号字符串
     * @return AV号
     * @throws IllegalArgumentException 如果BV号格式不正确
     */
    fun bv2av(bvid: String): Long {
        // 验证BV号格式
        require(bvid.length == 12) { "BV号长度必须为12位" }
        require(bvid.startsWith("BV1") || bvid.startsWith("bv1")) { "BV号必须以BV1或bv1开头" }
        
        // 转换为字符数组并统一大小写
        val bytes = bvid.uppercase().toCharArray()
        
        // 交换指定位置的字符（反向操作）
        swapCharacters(bytes)
        
        // Base58解码
        var tmp = 0L
        for (i in 3..11) {
            val index = REVERSE_TABLE[bytes[i]] 
                ?: throw IllegalArgumentException("BV号包含无效字符: ${bytes[i]}")
            tmp = tmp * BASE + index
        }
        
        // 计算AV号
        return (tmp and MASK_CODE) xor XOR_CODE
    }
    
    /**
     * 交换字符数组中指定位置的字符
     * 
     * @param chars 字符数组
     */
    private fun swapCharacters(chars: CharArray) {
        for (positions in SWAP_POSITIONS) {
            val temp = chars[positions[0]]
            chars[positions[0]] = chars[positions[1]]
            chars[positions[1]] = temp
        }
    }
    
    /**
     * 验证AV号是否有效
     * 
     * @param aid AV号
     * @return 是否有效
     */
    fun isValidAid(aid: Long): Boolean {
        return aid > 0 && aid < MAX_AID
    }
    
    /**
     * 验证BV号是否有效
     * 
     * @param bvid BV号
     * @return 是否有效
     */
    fun isValidBvid(bvid: String): Boolean {
        if (bvid.length != 12) return false
        if (!bvid.startsWith("BV1", ignoreCase = true)) return false
        
        // 检查是否只包含编码表中的字符
        val upperBvid = bvid.uppercase()
        for (i in 3..11) {
            if (upperBvid[i] !in TABLE) return false
        }
        
        return true
    }
    
    /**
     * 从URL或文本中提取BV号
     * 
     * @param text 包含BV号的文本
     * @return BV号，如果未找到则返回null
     */
    fun extractBvid(text: String): String? {
        val regex = Regex("(BV1[0-9A-Za-z]{9})", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.value
    }
    
    /**
     * 从URL或文本中提取AV号
     * 
     * @param text 包含AV号的文本
     * @return AV号，如果未找到则返回null
     */
    fun extractAid(text: String): Long? {
        val regex = Regex("av(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toLongOrNull()
    }
    
    /**
     * 智能转换
     * 自动识别输入是AV号还是BV号并进行相应转换
     * 
     * @param input AV号或BV号
     * @return 转换结果（如果输入是AV号返回BV号，反之亦然）
     * @throws IllegalArgumentException 如果输入格式不正确
     */
    fun smartConvert(input: String): String {
        return when {
            // 纯数字，作为AV号处理
            input.all { it.isDigit() } -> {
                av2bv(input.toLong())
            }
            // av开头，提取数字部分
            input.startsWith("av", ignoreCase = true) -> {
                val aid = input.substring(2).toLongOrNull()
                    ?: throw IllegalArgumentException("无效的AV号格式")
                av2bv(aid)
            }
            // BV号，转换为AV号
            isValidBvid(input) -> {
                "av${bv2av(input)}"
            }
            else -> throw IllegalArgumentException("无法识别的输入格式：$input")
        }
    }
    
    /**
     * 批量转换AV号为BV号
     * 
     * @param aids AV号列表
     * @return BV号列表
     */
    fun batchAv2Bv(aids: List<Long>): List<String> {
        return aids.map { av2bv(it) }
    }
    
    /**
     * 批量转换BV号为AV号
     * 
     * @param bvids BV号列表
     * @return AV号列表
     */
    fun batchBv2Av(bvids: List<String>): List<Long> {
        return bvids.map { bv2av(it) }
    }
}