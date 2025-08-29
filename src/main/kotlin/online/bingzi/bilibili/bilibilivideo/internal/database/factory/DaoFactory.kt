package online.bingzi.bilibili.bilibilivideo.internal.database.factory

import online.bingzi.bilibili.bilibilivideo.internal.config.DatabaseConfig
import online.bingzi.bilibili.bilibilivideo.internal.database.dao.*
import javax.sql.DataSource

/**
 * DAO工厂类
 * 根据数据库配置返回相应的DAO实现（MySQL或SQLite）
 * TODO: 需要实现具体的DAO实现类
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
     * TODO: 需要实现MySQL和SQLite的具体实现
     */
    fun getPlayerBindingDao(): PlayerBindingDao {
        throw NotImplementedError("PlayerBindingDao 的 MySQL/SQLite 实现尚未创建")
    }
    
    /**
     * 获取BilibiliAccountDao实现
     * TODO: 需要实现MySQL和SQLite的具体实现
     */
    fun getBilibiliAccountDao(): BilibiliAccountDao {
        throw NotImplementedError("BilibiliAccountDao 的 MySQL/SQLite 实现尚未创建")
    }
    
    /**
     * 获取VideoTripleStatusDao实现
     * TODO: 需要实现MySQL和SQLite的具体实现
     */
    fun getVideoTripleStatusDao(): VideoTripleStatusDao {
        throw NotImplementedError("VideoTripleStatusDao 的 MySQL/SQLite 实现尚未创建")
    }
    
    /**
     * 获取UpFollowStatusDao实现
     * TODO: 需要实现MySQL和SQLite的具体实现
     */
    fun getUpFollowStatusDao(): UpFollowStatusDao {
        throw NotImplementedError("UpFollowStatusDao 的 MySQL/SQLite 实现尚未创建")
    }
    
    /**
     * 获取DataSource
     */
    fun getDataSource(): DataSource = dataSource
}