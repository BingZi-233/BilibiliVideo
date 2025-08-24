package online.bingzi.bilibili.video.internal.qrcode

import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo

/**
 * 二维码生成器管理工具
 * 提供运行时配置监控、缓存管理和性能统计功能
 * 
 * @author BingZi-233
 * @since 1.0.0
 */
object QRCodeGeneratorManager {
    
    /**
     * 初始化二维码生成器
     * 在插件启动时调用，确保配置正确加载
     */
    fun initialize() {
        try {
            // 触发配置加载和验证
            QRCodeGenerator.getConfigSummary()
            console().sendInfo("qrcodeGeneratorInitialized")
        } catch (e: Exception) {
            console().sendInfo("qrcodeConfigurationError", e.message ?: "未知错误")
        }
    }
    
    /**
     * 获取详细的状态报告
     * 用于管理员命令或调试信息
     * 
     * @return 详细的状态信息
     */
    fun getDetailedStatus(): List<String> {
        return listOf(
            "==== 二维码生成器状态报告 ====",
            "",
            "配置信息:",
            "  ${QRCodeGenerator.getConfigSummary()}",
            "",
            "性能信息:",
            "  ${QRCodeGenerator.getCacheStats()}",
            "",
            "功能状态:",
            "  - 配置化参数: ✓ 已启用",
            "  - 对象缓存复用: ✓ 已启用", 
            "  - 智能尺寸推荐: ✓ 已启用",
            "  - URL验证: ✓ 已启用",
            "",
            "内存使用:",
            "  - MatrixToImageConfig缓存: ${getMatrixConfigCacheMemoryEstimate()}",
            "",
            "============================"
        )
    }
    
    /**
     * 执行缓存清理
     * 释放不必要的内存占用
     * 
     * @return 清理结果描述
     */
    fun performCacheCleanup(): String {
        val beforeStats = QRCodeGenerator.getCacheStats()
        QRCodeGenerator.clearCache()
        val afterStats = QRCodeGenerator.getCacheStats()
        
        return "缓存清理完成: $beforeStats -> $afterStats"
    }
    
    /**
     * 执行配置验证
     * 检查当前配置是否有效
     * 
     * @return 验证结果列表
     */
    fun validateConfiguration(): List<String> {
        val results = mutableListOf<String>()
        
        try {
            // 测试基础功能
            val testResult = QRCodeGenerator.generateQRCode("test", 100)
            results.add(if (testResult != null) "✓ 基础生成功能正常" else "✗ 基础生成功能异常")
            
            // 测试尺寸推荐
            val sizes = listOf(10, 50, 100, 300).map { QRCodeGenerator.getRecommendedSize(it) }
            val sizesValid = sizes.zipWithNext().all { it.first <= it.second }
            results.add(if (sizesValid) "✓ 尺寸推荐逻辑正常" else "✗ 尺寸推荐逻辑异常")
            
            // 测试URL验证
            val urlTestPassed = QRCodeGenerator.isValidUrl("https://example.com") && 
                              !QRCodeGenerator.isValidUrl("invalid-url")
            results.add(if (urlTestPassed) "✓ URL验证功能正常" else "✗ URL验证功能异常")
            
            results.add("配置验证完成")
            
        } catch (e: Exception) {
            results.add("✗ 配置验证过程中发生错误: ${e.message}")
        }
        
        return results
    }
    
    /**
     * 获取性能基准测试结果
     * 测试不同场景下的生成性能
     * 
     * @return 性能测试结果
     */
    fun getPerformanceBenchmark(): List<String> {
        val results = mutableListOf("==== 性能基准测试 ====", "")
        
        try {
            val testCases = mapOf(
                "短URL" to "https://b23.tv/abc123",
                "普通URL" to "https://www.bilibili.com/video/BV1xx411c7mD",
                "长URL" to "https://www.bilibili.com/video/BV1xx411c7mD?p=1&t=123&source=test",
                "JSON数据" to """{"type":"video","bvid":"BV1xx411c7mD","title":"测试视频"}"""
            )
            
            testCases.forEach { (name, content) ->
                val times = mutableListOf<Long>()
                
                // 执行3次测试取平均值
                repeat(3) {
                    val startTime = System.nanoTime()
                    QRCodeGenerator.generateQRCode(content)
                    val duration = (System.nanoTime() - startTime) / 1_000_000 // 转换为毫秒
                    times.add(duration)
                }
                
                val avgTime = times.average()
                val recommendedSize = QRCodeGenerator.getRecommendedSize(content.length)
                
                results.add("$name (${content.length}字符, ${recommendedSize}px): ${String.format("%.2f", avgTime)}ms")
            }
            
            results.add("")
            results.add(QRCodeGenerator.getCacheStats())
            results.add("========================")
            
        } catch (e: Exception) {
            results.add("性能测试失败: ${e.message}")
        }
        
        return results
    }
    
    /**
     * 估算MatrixToImageConfig缓存的内存使用量
     * 
     * @return 内存使用量估算（字符串形式）
     */
    private fun getMatrixConfigCacheMemoryEstimate(): String {
        val cacheStats = QRCodeGenerator.getCacheStats()
        val objectCount = cacheStats.substringAfter("缓存: ").substringBefore(" 个对象").toIntOrNull() ?: 0
        
        // 每个MatrixToImageConfig对象大约占用几十字节内存
        val estimatedBytes = objectCount * 64 // 粗略估算
        
        return when {
            estimatedBytes < 1024 -> "${estimatedBytes}B"
            estimatedBytes < 1024 * 1024 -> "${estimatedBytes / 1024}KB"
            else -> "${estimatedBytes / (1024 * 1024)}MB"
        }
    }
}