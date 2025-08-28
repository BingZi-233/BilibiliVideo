package online.bingzi.bilibili.bilibilivideo.internal.database.factory

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.table.*
import taboolib.module.database.Host
import taboolib.module.database.Table
import javax.sql.DataSource

/**
 * 表工厂类
 * 负责创建和初始化所有数据表
 */
object TableFactory {
    
    private val tables = mutableListOf<Table<*, *>>()
    
    /**
     * 创建所有表
     */
    fun createTables(host: Host<*>): List<Table<*, *>> {
        if (tables.isNotEmpty()) {
            return tables
        }
        
        // 创建所有表
        val playerBindingTable = PlayerBindingTable.createTable(host)
        val bilibiliAccountTable = BilibiliAccountTable.createTable(host)
        val videoTripleStatusTable = VideoTripleStatusTable.createTable(host)
        val upFollowStatusTable = UpFollowStatusTable.createTable(host)
        
        tables.addAll(listOf(
            playerBindingTable,
            bilibiliAccountTable,
            videoTripleStatusTable,
            upFollowStatusTable
        ))
        
        return tables
    }
    
    /**
     * 初始化所有表到数据库
     */
    fun initializeTables(dataSource: DataSource) {
        val host = DatabaseConfig.createHost()
        val allTables = createTables(host)
        
        // 创建表到数据库
        allTables.forEach { table ->
            try {
                table.createTable(dataSource)
                taboolib.common.platform.function.info("成功创建表: ${table.tableName}")
            } catch (e: Exception) {
                taboolib.common.platform.function.severe("创建表失败: ${table.tableName}, 错误: ${e.message}")
            }
        }
    }
    
    /**
     * 获取表名列表
     */
    fun getTableNames(): List<String> {
        return tables.map { it.tableName }
    }
}