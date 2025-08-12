package online.bingzi.bilibili.video

import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info

/*

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
*/

object BilibiliVideo : Plugin() {

    override fun onEnable() {
        info("Successfully running BilibiliVideo!")
    }

    override fun onDisable() {
        info("BilibiliVideo has been disabled.")
    }
}