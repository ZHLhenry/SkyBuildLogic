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
        lint {
            checkDependencies = true
            // 忽略指定的 Lint 规则
            disable += "UnusedResources" // 禁用未使用资源的检查
            disable += "TypographyQuotes" // 禁用引号样式的检查
            // 将 Lint 警告视为错误
            warningsAsErrors = true
            // 生成 HTML 格式的 Lint 报告
            htmlReport = true
            htmlOutput = file("${buildDir}/reports/lint/lint-report.html")
            // 生成 XML 格式的 Lint 报告
            xmlReport = true
            xmlOutput = file("${buildDir}/reports/lint/lint-report.xml")
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
        }
    }
}
