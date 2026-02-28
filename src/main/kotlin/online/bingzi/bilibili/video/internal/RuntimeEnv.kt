package online.bingzi.bilibili.video.internal

import taboolib.common.env.RuntimeDependencies
import taboolib.common.env.RuntimeDependency

/**
 * 运行时依赖声明。
 *
 * 通过 TabooLib 的 RuntimeDependency 机制,在插件运行时动态下载并加载以下库:
 * - OkHttp / Gson:HTTP 与 JSON 解析
 * - Ktorm / HikariCP:数据库访问与连接池
 * - SQLite / MySQL JDBC 驱动
 * - ZXing:二维码生成
 */
@RuntimeDependencies(
    RuntimeDependency(
        value = "!com.squareup.okhttp3:okhttp:4.12.0",
        test = "!okhttp3.OkHttpClient"
    ),
    RuntimeDependency(
        value = "!com.squareup.okio:okio-jvm:3.6.0",
        test = "!okio.Buffer"
    ),
    RuntimeDependency(
        value = "!com.google.code.gson:gson:2.11.0",
        test = "!com.google.gson.Gson"
    ),
    RuntimeDependency(
        value = "!org.ktorm:ktorm-core:3.6.0",
        test = "!org.ktorm.database.Database"
    ),
    RuntimeDependency(
        value = "!org.ktorm:ktorm-support-mysql:3.6.0",
        test = "!org.ktorm.support.mysql.MySqlDialect"
    ),
    RuntimeDependency(
        value = "!org.ktorm:ktorm-support-sqlite:3.6.0",
        test = "!org.ktorm.support.sqlite.SQLiteDialect"
    ),
    RuntimeDependency(
        value = "!com.zaxxer:HikariCP:4.0.3",
        test = "!com.zaxxer.hikari.HikariDataSource"
    ),
    RuntimeDependency(
        value = "!org.xerial:sqlite-jdbc:3.45.1.0",
        test = "!org.sqlite.JDBC",
        transitive = false
    ),
    RuntimeDependency(
        value = "!com.mysql:mysql-connector-j:8.3.0",
        test = "!com.mysql.cj.jdbc.Driver",
        transitive = false
    ),
    RuntimeDependency(
        value = "!com.google.zxing:core:3.5.2",
        test = "!com.google.zxing.qrcode.QRCodeWriter"
    ),
)
object RuntimeEnv
