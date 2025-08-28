package online.bingzi.bilibili.bilibilivideo

import online.bingzi.bilibili.bilibilivideo.internal.database.DatabaseManager
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

object BilibiliVideo : Plugin() {

    override fun onEnable() {
        info("正在启动 BilibiliVideo 插件...")
        
        // 数据库管理器会自动通过 @Awake(LifeCycle.ENABLE) 初始化
        // 这里只需要输出启动信息
        info("BilibiliVideo 插件启动完成！")
        info("数据库状态: ${DatabaseManager.getDatabaseInfo()}")
    }
    
    override fun onDisable() {
        info("BilibiliVideo 插件已关闭")
    }
}