package online.bingzi.bilibili.video

import online.bingzi.bilibili.video.internal.network.EnhancedLoginService
import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.platform.BukkitPlugin


@RuntimeDependencies(
    // OkHttp 5.0.0 及其依赖
    RuntimeDependency(
        value = "!com.squareup.okhttp3:okhttp:5.0.0",
        test = "!okhttp3.OkHttpClient",
        relocate = ["!okhttp3", "!online.bingzi.bilibili.video.libs.okhttp3", "!okio", "!online.bingzi.bilibili.video.libs.okio"]
    ),
    RuntimeDependency(
        value = "!com.squareup.okio:okio:3.9.1",
        test = "!okio.Okio",
        relocate = ["!okio", "!online.bingzi.bilibili.video.libs.okio"]
    ),
    // Gson
    RuntimeDependency(
        value = "!com.google.code.gson:gson:2.10.1",
        test = "!com.google.gson.Gson",
        relocate = ["!com.google.gson", "!online.bingzi.bilibili.video.libs.gson"]
    ),
    // ZXing 二维码生成库
    RuntimeDependency(
        value = "!com.google.zxing:core:3.5.3",
        test = "!com.google.zxing.BarcodeFormat",
        relocate = ["!com.google.zxing", "!online.bingzi.bilibili.video.libs.zxing"]
    ),
    RuntimeDependency(
        value = "!com.google.zxing:javase:3.5.3",
        test = "!com.google.zxing.client.j2se.MatrixToImageWriter",
        relocate = ["!com.google.zxing", "!online.bingzi.bilibili.video.libs.zxing"]
    ),
    // OrmLite
    RuntimeDependency(
        value = "!com.j256.ormlite:ormlite-core:6.1",
        test = "!com.j256.ormlite.dao.Dao",
        relocate = ["!com.j256.ormlite", "!online.bingzi.bilibili.video.libs.ormlite"]
    ),
    RuntimeDependency(
        value = "!com.j256.ormlite:ormlite-jdbc:6.1",
        test = "!com.j256.ormlite.jdbc.JdbcConnectionSource",
        relocate = ["!com.j256.ormlite", "!online.bingzi.bilibili.video.libs.ormlite"]
    ),
    // 数据库驱动
    RuntimeDependency(
        value = "!org.xerial:sqlite-jdbc:3.45.1.0",
        test = "!org.sqlite.JDBC",
        relocate = ["!org.sqlite", "!online.bingzi.bilibili.video.libs.sqlite"]
    ),
    RuntimeDependency(
        value = "!com.mysql:mysql-connector-j:8.3.0",
        test = "!com.mysql.cj.jdbc.Driver",
        relocate = ["!com.mysql", "!online.bingzi.bilibili.video.libs.mysql"]
    )
)
object BilibiliVideo : Plugin() {

    override fun onEnable() {
        info("Successfully running BilibiliVideo!")

        // 启动定时任务清理过期的登录会话
        startLoginSessionCleanupTask()
    }

    override fun onDisable() {
        info("BilibiliVideo has been disabled.")
    }

    /**
     * 启动登录会话清理任务
     */
    private fun startLoginSessionCleanupTask() {
        try {
            // 每10分钟清理一次过期会话
            val bukkitPlugin = BukkitPlugin.getInstance()
            org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(
                bukkitPlugin,
                Runnable {
                    EnhancedLoginService.cleanupExpiredSessions()
                },
                12000L, // 10分钟后开始 (20 ticks/秒 * 60秒 * 10分钟)
                12000L  // 每10分钟执行一次
            )
        } catch (e: Exception) {
            // 如果不在Bukkit环境，跳过定时任务
            info("Not in Bukkit environment, skipping periodic tasks")
        }
    }
}