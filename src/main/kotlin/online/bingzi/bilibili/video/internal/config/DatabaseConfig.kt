package online.bingzi.bilibili.video.internal.config

import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.module.database.HostSQL
import taboolib.module.database.getHost

/**
 * DatabaseConfig 对象
 * 该对象负责管理数据库的配置，包括加载配置文件和提供数据库连接信息。
 *
 * 主要功能包括：
 * 1. 从配置文件中加载数据库连接信息。
 * 2. 提供数据库表名和连接主机的访问方式。
 *
 * @constructor 创建一个空的 DatabaseConfig 对象
 */
object DatabaseConfig {
    /**
     * 配置文件对象，用于读取数据库相关的配置。
     * 通过 @Config 注解指定配置文件名为 "database.yml"。
     */
    @Config("database.yml")
    lateinit var config: Configuration
        private set // 该属性只允许在内部设置，外部无法修改

    /**
     * 延迟加载的 HostSQL 对象，用于获取数据库的连接主机信息。
     * 通过调用 config.getHost("sql") 方法从配置中获取主机信息。
     */
    val host: HostSQL by lazy {
        config.getHost("sql") // 从配置中获取名为 "sql" 的数据库主机信息
    }

    /**
     * 延迟加载的字符串属性，表示数据库表的名称。
     * 如果配置中未指定表名，则默认使用 "BilibiliVideo" 作为表名。
     */
    val table: String by lazy {
        config.getString("sql.table") ?: "BilibiliVideo" // 获取配置中的表名，若未指定则使用默认表名
    }
}