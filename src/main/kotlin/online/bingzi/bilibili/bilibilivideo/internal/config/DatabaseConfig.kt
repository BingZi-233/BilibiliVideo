package online.bingzi.bilibili.bilibilivideo.internal.config

import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.module.configuration.ConfigNode
import taboolib.module.database.Host
import taboolib.module.database.HostSQL
import taboolib.module.database.HostSQLite
import java.io.File
import javax.sql.DataSource

/**
 * 数据库配置管理类
 * 负责为 TabooLib Database 模块提供连接对象
 */
object DatabaseConfig {
    
    // 基本配置
    @ConfigNode("database.enable")
    var enable: Boolean = true
    
    // MySQL 配置
    @ConfigNode("database.mysql.host")
    var mysqlHost: String = "localhost"
    
    @ConfigNode("database.mysql.port")
    var mysqlPort: Int = 3306
    
    @ConfigNode("database.mysql.database")
    var mysqlDatabase: String = "bilibili_video"
    
    @ConfigNode("database.mysql.username")
    var mysqlUsername: String = "root"
    
    @ConfigNode("database.mysql.password")
    var mysqlPassword: String = ""
    
    @ConfigNode("database.mysql.use-ssl")
    var mysqlUseSsl: Boolean = false
    
    @ConfigNode("database.mysql.charset")
    var mysqlCharset: String = "utf8mb4"
    
    // SQLite 配置
    @ConfigNode("database.sqlite.file")
    var sqliteFile: String = "data/database.db"
    
    // 数据表配置
    @ConfigNode("database.table.prefix")
    var tablePrefix: String = "bv_"
    
    // 高级配置
    @ConfigNode("database.advanced.auto-reconnect")
    var advancedAutoReconnect: Boolean = true
    
    /**
     * 创建 TabooLib Database 模块所需的 Host 对象
     * @return Host<*> 对象，根据配置返回 HostSQL 或 HostSQLite
     */
    fun createHost(): Host<*> {
        return if (enable) {
            // 创建 MySQL Host
            val host = HostSQL(
                host = mysqlHost,
                port = mysqlPort.toString(),
                user = mysqlUsername,
                password = mysqlPassword,
                database = mysqlDatabase
            )
            
            // 配置连接参数
            host.flags.clear()
            host.flags.add("characterEncoding=$mysqlCharset")
            host.flags.add("useSSL=$mysqlUseSsl")
            host.flags.add("allowPublicKeyRetrieval=true") // 针对 MySQL8
            if (advancedAutoReconnect) {
                host.flags.add("autoReconnect=true")
            }
            
            host
        } else {
            // 创建 SQLite Host
            HostSQLite(getSqliteFile())
        }
    }
    
    /**
     * 创建 DataSource 对象
     * @param autoRelease 是否自动释放，默认为 true
     * @param withoutConfig 是否不使用配置，默认为 false
     * @return DataSource 对象
     */
    fun createDataSource(autoRelease: Boolean = true, withoutConfig: Boolean = false): DataSource {
        return createHost().createDataSource(autoRelease, withoutConfig)
    }
    
    /**
     * 获取带前缀的表名
     * @param tableName 原表名
     * @return 带前缀的完整表名
     */
    fun getTableName(tableName: String): String {
        return tablePrefix + tableName
    }
    
    /**
     * 获取 SQLite 数据库文件
     */
    private fun getSqliteFile(): File {
        val file = if (sqliteFile.startsWith("/")) {
            File(sqliteFile)
        } else {
            newFile(getDataFolder(), sqliteFile)
        }
        
        // 确保父目录存在
        file.parentFile?.mkdirs()
        
        return file
    }
}