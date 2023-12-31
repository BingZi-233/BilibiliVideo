plugins {
    `java-library`
    `maven-publish`
    id("io.izzel.taboolib") version "1.56"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
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
    install("common")
    install("common-5")
    install("module-chat")
    install("module-configuration")
    install("module-nms")
    install("module-nms-util")
    install("module-lang")
    install("module-kether")
    install("module-database")
    install("module-metrics")
    install("module-ui")
    install("platform-bukkit")
    install("expansion-command-helper")
    classifier = null
    version = "6.0.12-61"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.github.ben-manes.caffeine:caffeine:2.9.3")
    taboo("com.squareup.retrofit2:retrofit:2.9.0")
    taboo("com.squareup.retrofit2:converter-gson:2.9.0")
    compileOnly("com.google.zxing:core:3.5.2")
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v11902:11902-minimize:mapped")
    compileOnly("ink.ptms.core:v11902:11902-minimize:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.tabooproject.org/repository/releases")
            credentials {
                username = project.findProperty("taboolibUsername").toString()
                password = project.findProperty("taboolibPassword").toString()
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            groupId = project.group.toString()
        }
    }
}
