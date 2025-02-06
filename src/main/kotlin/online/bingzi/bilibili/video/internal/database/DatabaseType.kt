package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.config.DatabaseConfig

/**
 * 数据库类型枚举
 * 该枚举定义了项目中支持的数据库类型，包括本地和远程数据库。
 */
enum class DatabaseType {
    /**
     * SQLite数据库
     * SQLite是一个轻量级的本地数据库，通常用于小型应用或本地存储。
     */
    SQLITE,

    /**
     * MySQL数据库
     * MySQL是一个流行的远程关系型数据库，用于处理大规模数据和高并发用户访问。
     */
    MYSQL;

    companion object {
        /**
         * 数据库类型实例
         * 该属性用于获取当前配置的数据库类型实例。
         * 如果配置中未找到正确的数据库类型，将默认返回SQLite。
         */
        val INSTANCE: DatabaseType by lazy {
            try {
                // 尝试从数据库配置中读取数据库类型，并将其转换为大写形式以匹配枚举值
                valueOf(DatabaseConfig.config.getString("sql.type")!!.uppercase())
            } catch (ignore: Exception) {
                // 如果发生异常（例如找不到配置或类型不匹配），则默认为SQLite
                SQLITE
            }
        }
    }
}