package online.bingzi.bilibili.video.internal.database.dao

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.table.TableUtils
import online.bingzi.bilibili.video.api.event.database.DatabaseTableCreateEvent
import online.bingzi.bilibili.video.internal.database.DatabaseManager
import online.bingzi.bilibili.video.internal.database.entity.BilibiliBinding
import online.bingzi.bilibili.video.internal.database.entity.BilibiliCookie
import online.bingzi.bilibili.video.internal.database.entity.Player
import online.bingzi.bilibili.video.internal.database.entity.PlayerRewardStats
import online.bingzi.bilibili.video.internal.database.entity.QQBinding
import online.bingzi.bilibili.video.internal.database.entity.RewardConfig
import online.bingzi.bilibili.video.internal.database.entity.UploaderVideo
import online.bingzi.bilibili.video.internal.database.entity.UploaderConfig
import online.bingzi.bilibili.video.internal.database.entity.VideoRewardRecord
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.function.console
import taboolib.module.lang.sendInfo
import taboolib.module.lang.sendWarn
import java.sql.SQLException

/**
 * 数据库DAO管理器
 * 管理所有数据表的DAO实例和操作
 */
object DatabaseDaoManager {

    // DAO实例
    lateinit var playerDao: Dao<Player, String>
        private set

    lateinit var qqBindingDao: Dao<QQBinding, Long>
        private set

    lateinit var bilibiliBindingDao: Dao<BilibiliBinding, Long>
        private set

    lateinit var bilibiliCookieDao: Dao<BilibiliCookie, Long>
        private set

    lateinit var uploaderVideoDao: Dao<UploaderVideo, Long>
        private set

    lateinit var uploaderConfigDao: Dao<UploaderConfig, Long>
        private set

    lateinit var videoRewardRecordDao: Dao<VideoRewardRecord, Long>
        private set

    lateinit var rewardConfigDao: Dao<RewardConfig, Long>
        private set

    lateinit var playerRewardStatsDao: Dao<PlayerRewardStats, Long>
        private set

    /**
     * 初始化所有DAO
     */
    @Awake(LifeCycle.ENABLE)
    fun init() {
        try {
            if (!DatabaseManager.initialize()) {
                throw SQLException("数据库连接初始化失败")
            }

            val connectionSource = DatabaseManager.getConnectionSource()

            // 创建DAO实例
            playerDao = DaoManager.createDao(connectionSource, Player::class.java)
            qqBindingDao = DaoManager.createDao(connectionSource, QQBinding::class.java)
            bilibiliBindingDao = DaoManager.createDao(connectionSource, BilibiliBinding::class.java)
            bilibiliCookieDao = DaoManager.createDao(connectionSource, BilibiliCookie::class.java)
            uploaderVideoDao = DaoManager.createDao(connectionSource, UploaderVideo::class.java)
            uploaderConfigDao = DaoManager.createDao(connectionSource, UploaderConfig::class.java)
            videoRewardRecordDao = DaoManager.createDao(connectionSource, VideoRewardRecord::class.java)
            rewardConfigDao = DaoManager.createDao(connectionSource, RewardConfig::class.java)
            playerRewardStatsDao = DaoManager.createDao(connectionSource, PlayerRewardStats::class.java)

            // 创建表结构（如果不存在）
            createTablesIfNotExists()

            console().sendInfo("databaseInitialized")

        } catch (e: Exception) {
            console().sendWarn("databaseInitFailed", e.message ?: "Unknown error")
            throw e
        }
    }

    /**
     * 关闭数据库连接
     */
    @Awake(LifeCycle.DISABLE)
    fun close() {
        DatabaseManager.close()
    }

    /**
     * 创建所有表结构
     */
    private fun createTablesIfNotExists() {
        val connectionSource = DatabaseManager.getConnectionSource()

        DatabaseManager.entityClasses.forEach { entityClass ->
            try {
                TableUtils.createTableIfNotExists(connectionSource, entityClass)
                console().sendInfo("databaseTableCreated", entityClass.simpleName)
                
                // 触发表创建事件
                val tableCreateEvent = DatabaseTableCreateEvent(
                    tableName = entityClass.simpleName,
                    success = true
                )
                tableCreateEvent.call()
            } catch (e: SQLException) {
                console().sendWarn("databaseTableCreateFailed", entityClass.simpleName, e.message ?: "Unknown error")
                
                // 触发失败事件
                val failEvent = DatabaseTableCreateEvent(
                    tableName = entityClass.simpleName,
                    success = false,
                    errorMessage = e.message
                )
                failEvent.call()
                
                throw e
            }
        }
    }
}

