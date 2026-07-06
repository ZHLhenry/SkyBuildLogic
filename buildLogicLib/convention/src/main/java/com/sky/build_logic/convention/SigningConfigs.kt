package com.sky.build_logic.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.util.Properties

internal fun Project.applySigningConfigs(
    applicationExtension: ApplicationExtension,
) {
    val skyExt = ensureSkyBuildExtension()
    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

    val jksStoreFile = localProperties.getProperty("app.storeFile")
        ?: error("请在 local.properties 中配置 app.storeFile（签名文件路径）")
    val jksStorePassword = localProperties.getProperty("app.storePassword")
        ?: error("请在 local.properties 中配置 app.storePassword（签名密码）")
    val jksKeyAlias = localProperties.getProperty("app.keyAlias")
        ?: error("请在 local.properties 中配置 app.keyAlias（密钥别名）")

    applicationExtension.apply {
        signingConfigs {
            getByName("debug") {
                storeFile = file(jksStoreFile)
                storePassword = jksStorePassword
                keyAlias = jksKeyAlias
                keyPassword = jksStorePassword
            }
        }

        // applicationId, versionCode, versionName 通过 Variant API 惰性设置
        // 在 AndroidApplicationConventionPlugin 的 onVariants 中配置

        buildTypes {
            getByName("debug") {
                applicationIdSuffix = AppBuildType.DEBUG.applicationIdSuffix
                isMinifyEnabled = false
                signingConfig = signingConfigs.getByName("debug")
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                ndk {
                    abiFilters.addAll(arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
                }
            }
            getByName("release") {
                applicationIdSuffix = AppBuildType.RELEASE.applicationIdSuffix
                isMinifyEnabled = false
                signingConfig = signingConfigs.getByName("debug")
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                ndk {
                    abiFilters.addAll(arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
                }
            }
        }
    }
}
