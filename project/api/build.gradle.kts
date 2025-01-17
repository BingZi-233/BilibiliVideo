dependencies {
    compileOnly(project(":project:core"))
    // nms 模块
    compileOnly(project(":project:nms"))
    // ORMLite 核心
    compileOnly("com.j256.ormlite:ormlite-core:6.1")
    // ORMLite JDBC 驱动
    compileOnly("com.j256.ormlite:ormlite-jdbc:6.1")
    // 引入 Caffeine 缓存
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.2")
    // 引入 Retrofit 网络请求
    compileOnly("com.squareup.retrofit2:retrofit:2.11.0")
    // 引入 服务端 核心
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
}

// 子模块
taboolib { subproject = true }
