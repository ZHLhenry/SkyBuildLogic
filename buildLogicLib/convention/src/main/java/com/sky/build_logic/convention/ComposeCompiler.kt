package com.sky.build_logic.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * 内聚 Compose 编译器插件（org.jetbrains.kotlin.plugin.compose）的扩展配置。
 *
 * 必须在 compose 编译器插件 apply 之后调用（即各 ConventionPlugin 的 enableCompose=true 分支内）。
 */
internal fun Project.configureComposeCompiler() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // Kotlin 2.3+ 的 Compose 编译器会在 R8 混淆时向 mapping 文件追加 group key 映射条目，
        // 并由 report<Variant>ComposeMappingErrors 任务收集校验。该收集器的 tokenizer 存在缺陷
        // （Google IssueTracker 555304803），遇到含 $ 嵌套类、<clinit> 等签名时会输出大量
        // "Failed to collect Compose stack trace mapping (Failed to tokenize ...)" 警告。
        // 各项目均未启用 GroupKeys 诊断堆栈模式（Composer.setDiagnosticStackTraceMode），该映射无实际用途，
        // 故按官方建议整体关闭此功能以消除签名打包时的警告。
        includeComposeMappingFile.set(false)
    }
}
