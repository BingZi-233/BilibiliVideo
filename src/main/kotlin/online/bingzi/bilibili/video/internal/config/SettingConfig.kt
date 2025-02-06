package online.bingzi.bilibili.video.internal.config

import online.bingzi.bilibili.video.internal.cache.baffleCache
import online.bingzi.bilibili.video.internal.handler.ApiType
import online.bingzi.bilibili.video.internal.helper.debugStatus
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common5.Baffle
import taboolib.library.kether.ArgTypes.listOf
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import java.util.concurrent.TimeUnit

/**
 * SettingConfig
 * 该对象用于管理和存储应用程序的设置配置。
 * 主要功能包括加载配置文件、自动重载配置、更改相关设置等。
 */
object SettingConfig {

    /**
     * config
     * 配置文件，使用 YAML 格式存储应用程序的设置。
     * 通过 @Config 注解指定配置文件路径为 "setting.yml"。
     */
    @Config(value = "setting.yml")
    lateinit var config: Configuration
        private set

    /**
     * cooldown
     * 设置冷却时间（秒），用于限制某些操作的频率。
     * 默认值为 60 秒。
     */
    var cooldown: Long = 60

    /**
     * chainOperations
     * 存储链式操作的顺序，包含一系列 ApiType 类型的操作。
     * 默认值为空列表。
     */
    var chainOperations: List<ApiType> = listOf()

    /**
     * virtualization
     * 指示是否启用地图虚拟化功能。
     * 默认值为 true。
     */
    var virtualization: Boolean = true

    /**
     * sendMapAsync
     * 指示是否异步发送地图数据。
     * 默认值为 true。
     */
    var sendMapAsync: Boolean = true

    /**
     * registerAutoReload
     * 注册自动重载配置的功能。
     * 当配置文件被修改时，触发 reloadAction 方法以更新当前设置。
     */
    @Awake(LifeCycle.ENABLE)
    fun registerAutoReload() {
        config.onReload { reloadAction() }
    }

    /**
     * reloadAction
     * 重新加载配置文件中的设置，并更新所有相关的属性。
     * - 更新 cooldown 的值。
     * - 重置缓存。
     * - 重新构建链式操作列表。
     * - 更新调试状态。
     * - 更新虚拟化地图设置。
     * - 更新异步地图发送设置。
     */
    @Awake(LifeCycle.ENABLE)
    fun reloadAction() {
        // 从配置中获取冷却时间并更新
        cooldown = config.getLong("cooldown")

        // 驱逐所有缓存，确保使用最新的数据
        baffleCache.resetAll()

        // 根据新的冷却时间构建新的缓存对象
        baffleCache = Baffle.of(cooldown, TimeUnit.SECONDS)

        // 从配置中获取链式操作的列表并更新
        chainOperations = config.getEnumList("chainOperations", ApiType::class.java)

        // 从配置中获取调试状态并更新
        debugStatus = config.getBoolean("debug")

        // 从配置中获取虚拟化地图设置并更新
        virtualization = config.getBoolean("virtualization")

        // 从配置中获取异步地图发送设置并更新
        sendMapAsync = config.getBoolean("sendMapAsync")
    }
}