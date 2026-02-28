import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    java
    id("io.izzel.taboolib") version "2.0.31"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    `maven-publish`
    id("idea")
}

taboolib {
    env {
        install(Basic)
        install(BukkitHook)
        install(BukkitNMS)
        install(BukkitNMSItemTag)
        install(BukkitNMSUtil)
        install(BukkitUtil)
        install(CommandHelper)
        install(I18n)
        install(Metrics)
        install(MinecraftChat)
        install(Bukkit)
        install(Kether)
        enableIsolatedClassloader = true
    }
    description {
        name = "BilibiliVideo"
        contributors {
            name("BingZi-233")
        }
    }
    version {
        taboolib = "6.2.4-99fb800"
        skipKotlinRelocate = true
    }
}

val ktormVersion = "3.6.0"
val hikariVersion = "4.0.3"
val sqliteDriverVersion = "3.45.1.0"
val mysqlDriverVersion = "8.3.0"
val okhttpVersion = "4.12.0"
val okioVersion = "3.6.0"
val gsonVersion = "2.11.0"
val zxingVersion = "3.5.2"

dependencies {
    compileOnly("ink.ptms.core:v12111:12111:mapped")
    compileOnly("ink.ptms.core:v12111:12111:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    compileOnly("org.ktorm:ktorm-core:$ktormVersion")
    compileOnly("org.ktorm:ktorm-support-mysql:$ktormVersion")
    compileOnly("org.ktorm:ktorm-support-sqlite:$ktormVersion")
    compileOnly("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.xerial:sqlite-jdbc:$sqliteDriverVersion")
    compileOnly("com.mysql:mysql-connector-j:$mysqlDriverVersion")
    compileOnly("com.squareup.okhttp3:okhttp:$okhttpVersion")
    compileOnly("com.squareup.okio:okio-jvm:$okioVersion")
    compileOnly("com.google.code.gson:gson:$gsonVersion")
    compileOnly("com.google.zxing:core:$zxingVersion")
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

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}