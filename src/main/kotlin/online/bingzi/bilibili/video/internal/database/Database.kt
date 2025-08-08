package online.bingzi.bilibili.video.internal.database

import online.bingzi.bilibili.video.internal.helper.infoMessageAsLang
import org.bukkit.entity.Player
import taboolib.common.platform.ProxyPlayer
import taboolib.module.database.Table
import java.util.*
import javax.sql.DataSource

/**
 * 数据库接口
 * 该接口定义了与数据库交互的基本操作，包括获取表和数据源等功能。
 */
interface Database {
    // 代表数据库表的属性
    val table: Table<*, *>

    // 代表数据源的属性
    val dataSource: DataSource

    companion object {
        // 延迟初始化的数据库实例，确保数据库在第一次使用时才被创建
        private val INSTANCE by lazy {
            infoMessageAsLang("Database") // 输出数据库初始化信息
            val database = when (DatabaseType.INSTANCE) {
                DatabaseType.SQLITE -> DatabaseSQLite() // 根据类型选择SQLite数据库实现
                DatabaseType.MYSQL -> DatabaseMySQL()   // 根据类型选择MySQL数据库实现
            }
            infoMessageAsLang("Databased") // 输出数据库已创建的信息
            database
        }

        // 延迟初始化的表实例
        private val tableInstance by lazy {
            INSTANCE.table
        }

        // 延迟初始化的数据源实例
        private val dataSourceInstance by lazy {
            INSTANCE.dataSource
        }

        /**
         * 根据mid搜索玩家
         *
         * @param player 代理玩家对象，表示要搜索的玩家
         * @param mid 玩家在数据库中的唯一标识符
         * @return 如果找到玩家则返回true，否则返回false
         */
        fun searchPlayerByMid(player: ProxyPlayer, mid: String): Boolean {
            return tableInstance.select(dataSourceInstance) {
                where {
                    "key" eq "mid" // 查询条件：键为"mid"
                    "value" eq mid  // 查询条件：值为传入的mid
                }
            }.firstOrNull {
                player.uniqueId.toString() != getString("user") // 确保返回的用户不是当前玩家
            } ?: false // 如果没有找到，返回false
        }

        /**
         * 获取玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @return 对应键的值，如果未找到则返回null
         */
        fun UUID.getPlayerDataContainer(key: String): String? {
            return tableInstance.select(dataSourceInstance) {
                where {
                    "user" eq this@getPlayerDataContainer.toString() // 查询条件：用户为当前UUID
                    "key" eq key // 查询条件：键为传入的key
                }
            }.firstOrNull {
                getString("value") // 返回对应的值
            }
        }

        /**
         * 设置玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @param value 要设置的值
         */
        fun UUID.setPlayerDataContainer(key: String, value: String) {
            // 查找是否已有对应的用户数据
            val find = tableInstance.find(dataSourceInstance) {
                where {
                    "user" eq this@setPlayerDataContainer.toString() // 查询条件：用户为当前UUID
                    "key" eq key // 查询条件：键为传入的key
                }
            }
            // 如果找到了，则更新数据
            if (find) {
                tableInstance.update(dataSourceInstance) {
                    where {
                        "user" eq this@setPlayerDataContainer.toString() // 更新条件：用户为当前UUID
                        "key" eq key // 更新条件：键为传入的key
                    }
                    set("value", value) // 设置更新的值
                }
            } else {
                // 如果未找到，则插入新数据
                tableInstance.insert(dataSourceInstance, "user", "key", "value") {
                    value(this@setPlayerDataContainer.toString(), key, value) // 插入用户、键和值
                }
            }
        }

        /**
         * 获取玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @return 对应键的值，如果未找到则返回null
         */
        fun Player.getDataContainer(key: String): String? {
            return this.uniqueId.getPlayerDataContainer(key) // 调用UUID扩展函数获取值
        }

        /**
         * 设置玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @param value 要设置的值
         */
        fun Player.setDataContainer(key: String, value: String) {
            this.uniqueId.setPlayerDataContainer(key, value) // 调用UUID扩展函数设置值
        }

        /**
         * 获取代理玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @return 对应键的值，如果未找到则返回null
         */
        fun ProxyPlayer.getDataContainer(key: String): String? {
            return this.uniqueId.getPlayerDataContainer(key) // 调用UUID扩展函数获取值
        }

        /**
         * 设置代理玩家数据容器中的值
         *
         * @param key 数据容器的键
         * @param value 要设置的值
         */
        fun ProxyPlayer.setDataContainer(key: String, value: String) {
            this.uniqueId.setPlayerDataContainer(key, value) // 调用UUID扩展函数设置值
        }
    }
}