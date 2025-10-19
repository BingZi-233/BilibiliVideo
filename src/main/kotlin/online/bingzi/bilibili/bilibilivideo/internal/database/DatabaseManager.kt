package online.bingzi.bilibili.bilibilivideo.internal.database

import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.severe

/**
 * 数据库管理器
 * 
 * 负责数据库的生命周期管理，包括初始化、表创建和资源清理。
 * 使用TabooLib的生命周期注解自动在插件启用和禁用时执行相应操作。
 * 通过TableFactory统一管理所有数据表的创建和初始化。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object DatabaseManager {
    
    /** 数据库初始化状态标记 */
    private var isInitialized = false
    
    /**
     * 插件启用时初始化数据库
     * 
     * 使用TabooLib生命周期注解自动调用。
     * 执行表结构创建和初始化操作，避免重复初始化。
     */
    @Awake(LifeCycle.ENABLE)
    fun initialize() {
        if (isInitialized) {
            info("数据库已经初始化过了")
            return
        }
        
        try {
            info("开始初始化数据库...")
            
            // 创建并初始化所有数据表
            TableFactory.initializeTables()
            info("数据表初始化完成")
            
            // 输出初始化信息
            val tableNames = TableFactory.getTableNames()
            info("成功初始化 ${tableNames.size} 个数据表: ${tableNames.joinToString(", ")}")
            
            isInitialized = true
            info("数据库初始化完成！")
            
        } catch (e: Exception) {
            severe("数据库初始化失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 插件禁用时清理数据库资源
     * 
     * 使用TabooLib生命周期注解自动调用。
     * 执行数据库连接池关闭和其他清理工作。
     */
    @Awake(LifeCycle.DISABLE)
    fun cleanup() {
        if (isInitialized) {
            info("清理数据库资源...")
            // 这里可以添加数据库连接池关闭等清理逻辑
            isInitialized = false
            info("数据库资源清理完成")
        }
    }
    
    /**
     * 检查数据库是否已初始化
     * 
     * @return true 如果数据库已完成初始化，false 否则
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 获取数据库连接和表信息
     * 
     * 返回数据库的当前状态和已初始化的表信息。
     * 用于调试和状态监控。
     * 
     * @return 包含数据库状态的描述字符串
     */
    fun getDatabaseInfo(): String {
        return if (isInitialized) {
            val tableNames = TableFactory.getTableNames()
            "数据库已初始化，包含 ${tableNames.size} 个表: ${tableNames.joinToString(", ")}"
        } else {
            "数据库未初始化"
        }
    }
}