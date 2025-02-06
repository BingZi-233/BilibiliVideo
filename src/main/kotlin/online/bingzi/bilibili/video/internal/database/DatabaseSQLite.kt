package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.config.DatabaseConfig
import taboolib.module.database.ColumnTypeSQLite
import taboolib.module.database.Table
import taboolib.module.database.getHost
import taboolib.platform.util.bukkitPlugin
import java.io.File

// 数据库 SQLite 类，负责与 SQLite 数据库进行交互
// 主要功能包括创建数据库表和定义数据结构
// 该类实现了 Database 接口
class DatabaseSQLite : Database {
    // 数据库文件的路径，存放在插件的数据文件夹中，文件名为 "data.db"
    private val host = File(bukkitPlugin.dataFolder, "data.db").getHost()

    // 数据源，通过 host 创建，用于与数据库进行连接
    override val dataSource = host.createDataSource()

    // 数据库表的定义，包括表名和列的结构
    override val table = Table(DatabaseConfig.table, host) {
        // 定义主键 id 列
        add { id() }
        // 定义 user 列，类型为 TEXT，最大长度为 36
        add("user") {
            type(ColumnTypeSQLite.TEXT, 36)
        }
        // 定义 key 列，类型为 TEXT，最大长度为 64
        add("key") {
            type(ColumnTypeSQLite.TEXT, 64)
        }
        // 定义 value 列，类型为 TEXT，最大长度为 256
        add("value") {
            type(ColumnTypeSQLite.TEXT, 256)
        }
    }

    // 初始化块，创建数据库表
    // 在创建对象时会执行此块，确保数据库表被创建
    init {
        // 使用数据源工作区创建表
        table.workspace(dataSource) { createTable() }.run()
    }
}