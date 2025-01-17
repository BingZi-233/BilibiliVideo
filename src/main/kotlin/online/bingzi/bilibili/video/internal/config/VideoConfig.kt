package online.bingzi.bilibili.video.internal.config

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * Main config
 * 主配置文件
 *
 * @constructor Create empty Main config
 */
object VideoConfig {
    /**
     * Config
     * 配置文件
     */
    @Config(value = "video.yml")
    lateinit var config: Configuration
        private set

    lateinit var receiveMap: Map<String, List<String>>

    @Awake(LifeCycle.ENABLE)
    fun registerAutoReload() {
        config.onReload { reloadAction() }
    }

    @Awake(LifeCycle.ENABLE)
    fun reloadAction() {
        receiveMap = config.getKeys(false).associateWith { config.getStringList(it) }
    }
}
