@file:Suppress("DEPRECATION")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.sky.buildLogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "sky.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "sky.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCommonLibrary") {
            id = "sky.android.library.common"
            implementationClass = "AndroidCommonLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "sky.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidPublish") {
            id = "sky.android.publish"
            implementationClass = "AndroidMavenPublishConventionPlugin"
        }
        register("androidFlavors") {
            id = "sky.android.application.flavors"
            implementationClass = "AndroidApplicationFlavorsConventionPlugin"
        }
        register("jvmLibrary") {
            id = "sky.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}

// 发布 convention 插件时，自动同时发布 hilt-noop-processor
tasks.named("publish").configure {
    dependsOn(project(":hilt-noop-processor").tasks.named("publish"))
}

// ==================== Maven Publishing ====================
// kotlin-dsl (java-gradle-plugin) 已自动创建：
//   - pluginMaven 发布物（主 JAR，含 Gradle 插件描述符）
//   - <name>PluginMarkerMaven 发布物（每个插件 ID 的 Plugin Marker）
// 只需配置版本和发布仓库即可。
val localProps = Properties().apply {
    val localPropertiesFile = rootProject.file("../local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

version = localProps.getProperty("buildLogic.version")
    ?: error("请在 local.properties 中配置 buildLogic.version")

// 将 version 写入 JAR MANIFEST.MF，使运行时可通过 implementationVersion 读取
tasks.jar {
    manifest {
        attributes(
            "Implementation-Version" to version
        )
    }
}

val publishRepoUrl = localProps.getProperty("buildLogic.repoUrl", "")
val publishUsername = localProps.getProperty("buildLogic.username", "")
val publishPassword = localProps.getProperty("buildLogic.password", "")

publishing {
    repositories {
        if (publishRepoUrl.isNotBlank()) {
            maven {
                isAllowInsecureProtocol = publishRepoUrl.startsWith("http://")
                url = uri(publishRepoUrl)
                credentials {
                    username = publishUsername
                    password = publishPassword
                }
            }
        }
    }
}