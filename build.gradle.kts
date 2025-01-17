import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.22"
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
}

taboolib {
    description {
        desc("Bilibili视频一键三连奖励系统，目前维护在GitHub上。遇到问题请先在GitHub上提出Issue，遇到长时间无反应请联系[冰子]。")
        contributors {
            name("坏黑")
            name("冰子")
            name("南瓜")
        }
        dependencies {
            name("PlaceholderAPI").with("bukkit").optional(true)
        }
        links {
            name("homepage").url("https://github.com/BingZi-233/BilibiliVideo")
        }
    }
    env {
        install(Kether)
        install(Database)
        install(Metrics)
        install(Basic)
        install(Bukkit)
        install(BukkitNMS)
        install(BukkitNMSUtil)
        install(BukkitNMSItemTag)
        install(CommandHelper)
        install(BukkitHook)
    }
    version {
        taboolib = "6.2.2"
    }
    relocate("com.google.zxing", "online.bingzi.libs.zxing")
    relocate("com.google.gson", "online.bingzi.libs.gson")
    relocate("com.github.benmanes.caffeine", "online.bingzi.libs.caffeine")
}

repositories {
    mavenCentral()
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    taboo("com.github.ben-manes.caffeine:caffeine:2.9.3")
    taboo("com.squareup.retrofit2:retrofit:2.9.0")
    taboo("com.squareup.retrofit2:converter-gson:2.9.0")
    taboo("com.google.zxing:core:3.5.2")
    taboo("com.google.code.gson:gson:2.10.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:v12004:universal")
    compileOnly("ink.ptms.core:v12103:12103:mapped")
    compileOnly("ink.ptms.core:v12103:v12103:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
