package online.bingzi.bilibili.video.internal.config

import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration

/**
 * VideoConfig 对象
 *
 * 该对象负责管理视频相关的配置信息，包括配置文件的加载和重载。
 * 主要功能是提供一个可用于获取和更新视频配置的接口。
 */
object VideoConfig {
    /**
     * 配置文件
     *
     * 该属性用于存储从配置文件 video.yml 中加载的配置信息。
     * 配置文件的内容将被解析为 Configuration 类型。
     */
    @Config(value = "video.yml")
    lateinit var config: Configuration
        private set

    /**
     * 接收映射
     *
     * 该属性是一个 Map，键为字符串类型，值为字符串列表。
     * 它用于存储配置文件中每个键对应的字符串列表，例如视频相关的分类或标签。
     */
    lateinit var receiveMap: Map<String, List<String>>

    /**
     * 注册自动重载
     *
     * 该方法在生命周期开启时被调用，用于注册配置重载的动作。
     * 当配置文件发生变化时，将自动调用 reloadAction() 方法来更新 receiveMap。
     */
    @Awake(LifeCycle.ENABLE)
    fun registerAutoReload() {
        // 注册配置重载监听器
        config.onReload { reloadAction() }
    }

    /**
     * 重载操作
     *
     * 该方法用于在配置文件重载时执行的具体操作。
     * 它会更新 receiveMap 属性，将配置文件中的每个键及其对应的字符串列表进行映射。
     */
    @Awake(LifeCycle.ENABLE)
    fun reloadAction() {
        // 更新 receiveMap，将配置文件的每个键映射到对应的字符串列表
        receiveMap = config.getKeys(false).associateWith { config.getStringList(it) }
    }
}