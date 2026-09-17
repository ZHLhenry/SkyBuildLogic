package com.sky.build_logic.convention

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.util.Properties

internal fun Project.applySigningConfigs(
    applicationExtension: ApplicationExtension,
) {
    val skyExt = ensureSkyBuildExtension()
    // App 模块 keep 规则统一放在 src/main/keepRules 目录（.keep 后缀），在此显式汇总
    val keepRulesFiles = fileTree("src/main/keepRules").matching { include("**/*.keep") }.files
    val localProperties = Properties().apply {
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            load(localPropertiesFile.inputStream())
        }
    }

    // Debug 签名配置（必填）
    val debugStoreFile = localProperties.getProperty("app.debug.storeFile")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.debug.storeFile（debug 签名文件路径）")
    val debugStorePassword = localProperties.getProperty("app.debug.storePassword")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.debug.storePassword（debug 签名密码）")
    val debugKeyAlias = localProperties.getProperty("app.debug.keyAlias")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.debug.keyAlias（debug 密钥别名）")

    // Release 签名配置（必填）
    val releaseStoreFile = localProperties.getProperty("app.release.storeFile")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.release.storeFile（release 签名文件路径）")
    val releaseStorePassword = localProperties.getProperty("app.release.storePassword")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.release.storePassword（release 签名密码）")
    val releaseKeyAlias = localProperties.getProperty("app.release.keyAlias")?.takeIf { it.isNotBlank() }
        ?: error("请在 local.properties 中配置 app.release.keyAlias（release 密钥别名）")

    applicationExtension.apply {
        signingConfigs {
            getByName("debug") {
                storeFile = file(debugStoreFile)
                storePassword = debugStorePassword
                keyAlias = debugKeyAlias
                keyPassword = debugStorePassword
            }
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseStorePassword
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
                    *keepRulesFiles.toTypedArray()
                )
                ndk {
                    abiFilters.addAll(arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
                }
            }
            getByName("release") {
                applicationIdSuffix = AppBuildType.RELEASE.applicationIdSuffix
                // App 混淆开关由 skyBuild.enableAppMinify 内聚控制，debug 始终不混淆
                isMinifyEnabled = skyExt.enableAppMinify.get()
                // 资源压缩与混淆开关同步（R8 资源压缩前提为 isMinifyEnabled=true）
                isShrinkResources = skyExt.enableAppMinify.get()
                signingConfig = signingConfigs.getByName("release")
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    *keepRulesFiles.toTypedArray()
                )
                ndk {
                    abiFilters.addAll(arrayOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"))
                }
            }
        }
    }
}
