package online.bingzi.bilibili.bilibilivideo.internal.database.factory

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.table.*
import taboolib.module.database.Host
import taboolib.module.database.Table
import javax.sql.DataSource

/**
 * 表工厂类
 * 负责创建和初始化所有数据表
 * 使用统一的表实现，根据Host类型自动适配MySQL或SQLite
 */
object TableFactory {
    
    private val tables = mutableListOf<Table<*, *>>()
    private lateinit var host: Host<*>
    private lateinit var dataSource: DataSource
    
    /**
     * 创建所有表
     * 使用统一的表实现，根据Host类型自动适配MySQL或SQLite
     */
    fun createTables(host: Host<*>): List<Table<*, *>> {
        if (tables.isNotEmpty()) {
            return tables
        }
        
        // 使用统一的表实现，类型推断在createTable方法内部进行
        tables.addAll(listOf(
            PlayerBindingTable.createTable(host),
            BilibiliAccountTable.createTable(host),
            VideoTripleStatusTable.createTable(host),
            UpFollowStatusTable.createTable(host)
        ))
        
        return tables
    }
    
    /**
     * 初始化所有表到数据库
     */
    fun initializeTables() {
        host = DatabaseConfig.createHost()
        dataSource = DatabaseConfig.createDataSource()
        
        val allTables = createTables(host)
        
        // 创建表到数据库
        allTables.forEach { table ->
            try {
                table.createTable(dataSource)
                taboolib.common.platform.function.info("成功创建表: ${table.name}")
            } catch (e: Exception) {
                taboolib.common.platform.function.severe("创建表失败: ${table.name}, 错误: ${e.message}")
            }
        }
    }
    
    /**
     * 获取表名列表
     */
    fun getTableNames(): List<String> {
        return tables.map { it.name }
    }
    
    /**
     * 获取DataSource
     */
    fun getDataSource(): DataSource = dataSource
    
    /**
     * 获取玩家绑定表
     */
    fun getPlayerBindingTable(): Table<*, *> {
        return tables.find { it.name.endsWith("player_binding") } 
            ?: PlayerBindingTable.createTable(host)
    }
    
    /**
     * 获取B站账户表
     */
    fun getBilibiliAccountTable(): Table<*, *> {
        return tables.find { it.name.endsWith("bilibili_account") }
            ?: BilibiliAccountTable.createTable(host)
    }
    
    /**
     * 获取视频三连状态表
     */
    fun getVideoTripleStatusTable(): Table<*, *> {
        return tables.find { it.name.endsWith("video_triple_status") }
            ?: VideoTripleStatusTable.createTable(host)
    }
    
    /**
     * 获取UP主关注状态表
     */
    fun getUpFollowStatusTable(): Table<*, *> {
        return tables.find { it.name.endsWith("up_follow_status") }
            ?: UpFollowStatusTable.createTable(host)
    }
}