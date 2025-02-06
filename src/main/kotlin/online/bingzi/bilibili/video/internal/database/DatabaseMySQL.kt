package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.config.DatabaseConfig
import online.bingzi.bilibili.video.internal.config.DatabaseConfig.host
import taboolib.module.database.ColumnOptionSQL
import taboolib.module.database.ColumnTypeSQL
import taboolib.module.database.Table

// DatabaseMySQL 类实现了 Database 接口，用于与 MySQL 数据库进行交互。
// 该类负责定义数据库表的结构，并创建数据源以供操作数据库。
class DatabaseMySQL : Database {

    // dataSource 属性用于存储数据库连接池的数据源。
    // 通过调用 host.createDataSource() 创建数据源。
    override val dataSource = host.createDataSource()

    // table 属性定义了数据库表的结构，包括列的名称、类型及其选项。
    // 该表的名称和数据库连接信息均通过 DatabaseConfig 提供。
    override val table = Table(DatabaseConfig.table, host) {
        // 添加 id 列，采用默认的自增整型作为主键。
        add { id() }

        // 添加 user 列，类型为 VARCHAR，长度为 36，并设置为主键选项。
        add("user") {
            type(ColumnTypeSQL.VARCHAR, 36) {
                options(ColumnOptionSQL.KEY)
            }
        }

        // 添加 key 列，类型为 VARCHAR，长度为 64。
        add("key") {
            type(ColumnTypeSQL.VARCHAR, 64)
        }

        // 添加 value 列，类型为 VARCHAR，长度为 256。
        add("value") {
            type(ColumnTypeSQL.VARCHAR, 256)
        }
    }

    // 初始化块，负责在类实例化时创建数据库表。
    // 通过调用 table.workspace(dataSource) 来创建表，并执行相关操作。
    init {
        table.workspace(dataSource) { createTable() }.run()
    }
}