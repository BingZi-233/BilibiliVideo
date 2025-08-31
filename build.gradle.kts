import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    java
    id("io.izzel.taboolib") version "2.0.27"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    `maven-publish`
}

taboolib {
    env {
        install(Basic)
        install(BukkitHook)
        install(BukkitUtil)
        install(CommandHelper)
        install(I18n)
        install(Metrics)
        install(MinecraftChat)
        install(Bukkit)
        install(Kether)
        install(Database)
    }
    description {
        name = "BilibiliVideo"
        contributors {
            name("BingZi-233")
        }
    }
    version { taboolib = "6.2.3-2eb93b5" }
    relocate("com.google.gson", "online.bingzi.bilibili.bilibilivideo.library.gson")
    relocate("okhttp3","online.bingzi.bilibili.bilibilivideo.library.okhttp3")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    taboo("com.squareup.okhttp3:okhttp:4.12.0")
    taboo("com.squareup.okhttp3:logging-interceptor:4.12.0")
    taboo("com.google.code.gson:gson:2.10.1")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "online.bingzi"
            artifactId = "bilibilivideo"
            // 使用 gradle.properties 中的版本号
            version = project.version.toString()

            // 直接发布API JAR文件
            val apiJarFile = file("build/libs/${rootProject.name}-${rootProject.version}-api.jar")
            artifact(apiJarFile)

            pom {
                name.set("BilibiliVideo")
                description.set("链接BilibiliVideo的Minecraft插件")
                url.set("https://github.com/BingZi-233/BilibiliVideo")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("BingZi-233")
                        name.set("BingZi-233")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "AeolianCloud"
            url = uri("https://repo.aeoliancloud.com/repository/releases/")
            credentials {
                username = System.getenv("MAVEN_USERNAME") ?: project.findProperty("mavenUsername") as String?
                password = System.getenv("MAVEN_PASSWORD") ?: project.findProperty("mavenPassword") as String?
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
            // 添加上传方法配置
            isAllowInsecureProtocol = false
        }
    }
}

// 确保发布任务依赖于API构建任务
tasks.withType<PublishToMavenRepository> {
    dependsOn("taboolibBuildApi")
}