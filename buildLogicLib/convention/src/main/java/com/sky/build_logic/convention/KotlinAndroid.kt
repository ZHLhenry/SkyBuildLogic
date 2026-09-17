@file:Suppress("DEPRECATION")

package com.sky.build_logic.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.android.build.api.dsl.LibraryExtension
import org.gradle.kotlin.dsl.assign
import org.jetbrains.kotlin.gradle.dsl.JvmTarget


internal fun Project.configureAndroidLibrary(commonExtension: LibraryExtension) {
    val skyExt = ensureSkyBuildExtension()
    commonExtension.apply {
        compileSdk = skyExt.compileSdk.get()
        defaultConfig {
            minSdk = skyExt.minSdk.get()
        }
        // enableCompose=true 时自动开启 compose，消费者无需手动声明 buildFeatures { compose = true }
        buildFeatures {
            compose = skyExt.enableCompose.get()
        }
        lint {
            checkDependencies = true
            // 忽略指定的 Lint 规则
            disable += "UnusedResources" // 禁用未使用资源的检查
            disable += "TypographyQuotes" // 禁用引号样式的检查
            // 将 Lint 警告视为错误
            warningsAsErrors = true
            // 生成 HTML 格式的 Lint 报告
            htmlReport = true
            htmlOutput = layout.buildDirectory.file("reports/lint/lint-report.html").get().asFile
            // 生成 XML 格式的 Lint 报告
            xmlReport = true
            xmlOutput = layout.buildDirectory.file("reports/lint/lint-report.xml").get().asFile
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

}

/**
 * 配置 Library 模块的 release 构建类型：内聚 R8 混淆开关与规则通道。
 *
 * 规则目录约定（`.keep` 后缀）：
 * - `src/main/keepRules`：公开 API keep 清单（单一事实源）。AGP 会自动把该目录打包进
 *   AAR 的 proguard.txt 分发给消费者；但 AGP 不会把它传给 Library 自身 R8，故本函数
 *   显式汇总进 proguardFiles（否则公开 API 被按无引用裁剪，classes.jar 为空）；
 * - `src/main/minifyRules`：仅库自身混淆 pass 生效的规则（keepattributes 等元数据规则），
 *   放在 keepRules 之外以避免泄漏进消费者 App 的 R8 配置；
 * - 模块根目录 `consumer-rules.keep`：旧约定兼容，存在时注册为额外消费者规则。
 */
internal fun Project.configureLibraryMinify(commonExtension: LibraryExtension) {
    val skyExt = ensureSkyBuildExtension()
    val consumerRulesFile = file("consumer-rules.keep")
    val keepRulesFiles = fileTree("src/main/keepRules").matching { include("**/*.keep") }.files
    val minifyRulesFiles = fileTree("src/main/minifyRules").matching { include("**/*.keep") }.files
    // fail-fast：开启混淆却无 keep 规则时，R8 会把公开 API 全部按无引用裁剪（classes.jar 为空），
    // 且构建本身不报任何错误，问题会延迟到消费方编译/运行才暴露，故在此显式阻断
    if (skyExt.enableLibraryMinify.get() && keepRulesFiles.isEmpty()) {
        throw org.gradle.api.GradleException(
            "[SkyBuild] ${project.path} 开启了 skyBuild.enableLibraryMinify=true，但 src/main/keepRules/ 下未找到任何 .keep 规则文件。" +
                "请先补充 .keep 规则文件 清单。"
        )
    }
    commonExtension.apply {
        buildTypes {
            release {
                isMinifyEnabled = skyExt.enableLibraryMinify.get()
                // keepRules（公开 API 单一事实源）+ minifyRules（pass 1 专属）显式传给库自身 R8
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    *keepRulesFiles.toTypedArray(),
                    *minifyRulesFiles.toTypedArray()
                )
                if (consumerRulesFile.exists()) {
                    consumerProguardFiles("consumer-rules.keep")
                }
            }
        }
    }
}

/**
 * Configure base Kotlin with Android options
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    val skyExt = ensureSkyBuildExtension()
    commonExtension.apply {
        compileSdk = skyExt.compileSdk.get()
        defaultConfig.minSdk = skyExt.minSdk.get()

        compileOptions.apply {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        packaging.apply {
            jniLibs {
                excludes += setOf("META-INF/{AL2.0,LGPL2.1}")
            }
            resources {
                excludes += setOf(
                    "**/*.md",
                    "**/*.version",
                    "**/*.properties",
                    "**/**/*.properties",
                    "META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/CHANGES",
                    "DebugProbesKt.bin",
                    "kotlin-tooling-metadata.json",
                    "META-INF/gradle/incremental.annotation.processors"
                )
            }
        }
    }
    configureKotlin()
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // Up to Java 11 APIs are available through desugaring
        // https://developer.android.com/studio/write/java11-minimal-support-table
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin()
}

/**
 * Configure base Kotlin options
 */
@Suppress("DEPRECATION")
private fun Project.configureKotlin() {
    val skyExt = ensureSkyBuildExtension()
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            // Set JVM target to 17
            jvmTarget.set(JvmTarget.JVM_17)
            // Treat all Kotlin warnings as errors (disabled by default)
            // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
            val warningsAsErrors: String? by project
            allWarningsAsErrors.set(warningsAsErrors.toBoolean())
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
            )
            // enableCompose=true 时自动 opt-in Material3 Experimental API，
            // 消费者无需再在各模块手动声明 freeCompilerArgs.add(ExperimentalMaterial3Api)
            if (skyExt.enableCompose.get()) {
                freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
            }
        }
    }
}
