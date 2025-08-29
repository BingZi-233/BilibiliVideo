package online.bingzi.bilibili.bilibilivideo.internal.database

import online.bingzi.bilibili.bilibilivideo.internal.database.factory.TableFactory
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.info
import taboolib.common.platform.function.severe

/**
 * 数据库管理器
 * 负责数据库的初始化、表创建和连接管理
 */
object DatabaseManager {
    
    private var isInitialized = false
    
    /**
     * 插件启用时初始化数据库
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
     * 插件禁用时清理资源
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
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 获取数据库连接信息
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