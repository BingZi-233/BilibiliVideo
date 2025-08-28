package online.bingzi.bilibili.bilibilivideo.internal.database.factory

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.dao.*
import online.bingzi.bilibili.bilibilivideo.internal.database.impl.mysql.*
import online.bingzi.bilibili.bilibilivideo.internal.database.impl.sqlite.*
import javax.sql.DataSource

/**
 * DAO工厂类
 * 根据数据库配置返回相应的DAO实现（MySQL或SQLite）
 */
object DaoFactory {
    
    private lateinit var dataSource: DataSource
    
    /**
     * 初始化DataSource
     */
    fun initialize() {
        dataSource = DatabaseConfig.createDataSource()
    }
    
    /**
     * 获取PlayerBindingDao实现
     */
    fun getPlayerBindingDao(): PlayerBindingDao {
        return if (DatabaseConfig.enable) {
            PlayerBindingDaoMySQLImpl(dataSource)
        } else {
            PlayerBindingDaoSQLiteImpl(dataSource)
        }
    }
    
    /**
     * 获取BilibiliAccountDao实现
     */
    fun getBilibiliAccountDao(): BilibiliAccountDao {
        return if (DatabaseConfig.enable) {
            BilibiliAccountDaoMySQLImpl(dataSource)
        } else {
            BilibiliAccountDaoSQLiteImpl(dataSource)
        }
    }
    
    /**
     * 获取VideoTripleStatusDao实现
     */
    fun getVideoTripleStatusDao(): VideoTripleStatusDao {
        return if (DatabaseConfig.enable) {
            VideoTripleStatusDaoMySQLImpl(dataSource)
        } else {
            VideoTripleStatusDaoSQLiteImpl(dataSource)
        }
    }
    
    /**
     * 获取UpFollowStatusDao实现
     */
    fun getUpFollowStatusDao(): UpFollowStatusDao {
        return if (DatabaseConfig.enable) {
            UpFollowStatusDaoMySQLImpl(dataSource)
        } else {
            UpFollowStatusDaoSQLiteImpl(dataSource)
        }
    }
    
    /**
     * 获取DataSource
     */
    fun getDataSource(): DataSource = dataSource
}