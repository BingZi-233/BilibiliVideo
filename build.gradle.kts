import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    java
    id("io.izzel.taboolib") version "2.0.27"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

taboolib {
    env {
        install(Basic)
        install(I18n)
        install(Metrics)
        install(MinecraftChat)
        install(CommandHelper)
        install(Bukkit)
        install(Kether)
        install(BukkitHook)
        install(BukkitUtil)
        install("database")
    }
    description {
        name = "BilibiliVideo"
        contributors {
            name("BingZi-233")
        }
        links {
            name("https://github.com/BingZi-233/BilibiliVideo")
        }
        dependencies {
            name("OneBot")
        }
    }
    version { taboolib = "6.2.3-ee81cb0" }
    relocate("okhttp3", "online.bingzi.bilibili.video.libs.okhttp3")
    relocate("okio", "online.bingzi.bilibili.video.libs.okio")
    relocate("com.google.gson", "online.bingzi.bilibili.video.libs.gson")
    relocate("com.squareup.retrofit2", "online.bingzi.bilibili.video.libs.retrofit2")
    relocate("com.google.zxing", "online.bingzi.bilibili.video.libs.zxing")
    relocate("com.j256.ormlite", "online.bingzi.bilibili.video.libs.ormlite")
    relocate("org.sqlite", "online.bingzi.bilibili.video.libs.sqlite")
    relocate("com.mysql", "online.bingzi.bilibili.video.libs.mysql")
    relocate("com.github.benmanes.caffeine", "online.bingzi.bilibili.video.libs.caffeine")
}

repositories {
    mavenCentral()
    maven("https://repo.aeoliancloud.com/repository/releases/")
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    // OneBot依赖
    compileOnly("online.bingzi:onebot:1.2.0-e9aeb2f")
    taboo("com.squareup.okhttp3:okhttp:4.12.0")
    taboo("com.google.code.gson:gson:2.10.1")
    // Retrofit依赖
    taboo("com.squareup.retrofit2:retrofit:2.9.0")
    taboo("com.squareup.retrofit2:converter-gson:2.9.0")
    // 二维码生成库
    taboo("com.google.zxing:core:3.5.3")
    taboo("com.google.zxing:javase:3.5.3")
    // OrmLite依赖
    taboo("com.j256.ormlite:ormlite-core:6.1")
    taboo("com.j256.ormlite:ormlite-jdbc:6.1")
    // 数据库驱动
    taboo("org.xerial:sqlite-jdbc:3.45.1.0")
    taboo("com.mysql:mysql-connector-j:8.3.0")
    // 缓存库
    taboo("com.github.ben-manes.caffeine:caffeine:2.9.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
