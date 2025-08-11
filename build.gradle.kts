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
    }
    description {
        name = "BilibiliVideo"
        contributors {
            name("BingZi-233")
        }
        links {
            name("https://github.com/BingZi-233/BilibiliVideo")
        }
    }
    version { taboolib = "6.2.3-ee81cb0" }
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    // 二维码生成库
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.google.zxing:javase:3.5.3")
    // Kotlin协程库
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // OneBot依赖
    implementation("online.bingzi:onebot:1.0.0-97bdc38")
    // OrmLite依赖
    implementation("com.j256.ormlite:ormlite-core:6.1")
    implementation("com.j256.ormlite:ormlite-jdbc:6.1")
    // 数据库驱动
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
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
