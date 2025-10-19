package online.bingzi.bilibili.bilibilivideo.internal.config

import taboolib.common.io.newFile
import taboolib.common.platform.function.getDataFolder
import taboolib.module.configuration.Config
import taboolib.module.configuration.ConfigNode
import taboolib.module.configuration.Configuration
import taboolib.module.database.Host
import taboolib.module.database.HostSQL
import taboolib.module.database.HostSQLite
import java.io.File
import javax.sql.DataSource

/**
 * 数据库配置管理类
 * 
 * 负责管理数据库配置信息并为TabooLib Database模块提供连接对象。
 * 支持MySQL和SQLite双数据库模式，通过enable配置项动态切换。
 * 使用TabooLib Configuration模块自动管理配置文件。
 * 
 * @since 1.0.0
 * @author BilibiliVideo
 */
object DatabaseConfig {
    /** 配置文件实例 */
    @Config("database.yml")
    lateinit var config: Configuration
        private set

    /** 数据库开关，true使用MySQL，false使用SQLite */
    @ConfigNode(value = "database.enable", bind = "database.yml")
    var enable: Boolean = false

    /** MySQL主机地址 */
    @ConfigNode(value = "database.mysql.host", bind = "database.yml")
    var mysqlHost: String = "localhost"

    /** MySQL端口号 */
    @ConfigNode(value = "database.mysql.port", bind = "database.yml")
    var mysqlPort: Int = 3306

    /** MySQL数据库名 */
    @ConfigNode(value = "database.mysql.database", bind = "database.yml")
    var mysqlDatabase: String = "bilibili_video"

    /** MySQL用户名 */
    @ConfigNode(value = "database.mysql.username", bind = "database.yml")
    var mysqlUsername: String = "root"

    /** MySQL密码 */
    @ConfigNode(value = "database.mysql.password", bind = "database.yml")
    var mysqlPassword: String = ""

    /** MySQL是否使用SSL */
    @ConfigNode(value = "database.mysql.use-ssl", bind = "database.yml")
    var mysqlUseSsl: Boolean = false

    /** MySQL字符编码 */
    @ConfigNode(value = "database.mysql.charset", bind = "database.yml")
    var mysqlCharset: String = "utf8mb4"

    /** SQLite数据库文件路径 */
    @ConfigNode(value = "database.sqlite.file", bind = "database.yml")
    var sqliteFile: String = "data/database.db"

    /** 数据表名前缀 */
    @ConfigNode(value = "database.table.prefix", bind = "database.yml")
    var tablePrefix: String = "bv_"

    /** MySQL自动重连开关 */
    @ConfigNode(value = "database.advanced.auto-reconnect", bind = "database.yml")
    var advancedAutoReconnect: Boolean = true

    /**
     * 创建TabooLib Database模块所需的Host对象
     * 
     * 根据enable配置项决定返回MySQL或SQLite的Host实例。
     * 对于MySQL会自动配置连接参数和字符编码。
     * 
     * @return Host对象，HostSQL或HostSQLite的实例
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
     * 创建DataSource对象
     * 
     * 基于createHost()方法创建的Host对象生成DataSource。
     * 
     * @param autoRelease 是否自动释放连接，默认true
     * @param withoutConfig 是否忽略额外配置，默认false
     * @return DataSource数据源对象
     */
    fun createDataSource(autoRelease: Boolean = true, withoutConfig: Boolean = false): DataSource {
        return createHost().createDataSource(autoRelease, withoutConfig)
    }

    /**
     * 获取带前缀的完整表名
     * 
     * 将配置的表前缀与原始表名组合，生成最终的数据库表名。
     * 
     * @param tableName 原始表名
     * @return 带前缀的完整表名
     */
    fun getTableName(tableName: String): String {
        return tablePrefix + tableName
    }

    /**
     * 获取SQLite数据库文件对象
     * 
     * 根据配置的SQLite文件路径创建File对象。
     * 支持绝对路径和相对路径，相对路径将基于插件数据目录。
     * 自动创建父目录确保文件路径存在。
     * 
     * @return SQLite数据库文件对象
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