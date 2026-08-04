package com.sky.build_logic.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose 依赖集合类型。
 *
 * - [RUNTIME_ONLY]：仅 BOM + runtime，适用于不暴露 Compose UI 的公共模块。
 * - [CORE_UI]：完整核心 UI 套件（foundation / ui / material3 / tooling-preview 等），
 *   适用于 Application 与对外暴露 Compose API 的 Library。
 */
internal enum class ComposeDependencySet {
    RUNTIME_ONLY,
    CORE_UI
}

/**
 * 当 enableCompose=true 时，自动内聚 Compose 依赖。
 *
 * 版本号由 [SkyBuildExtension.composeBomVersion] 控制，默认使用插件内置版本；
 * 消费者可在根项目 build.gradle.kts 中通过 `extra["skyBuild.composeBomVersion"]` 覆盖。
 *
 * @param dependencySet 依赖集合类型，[ComposeDependencySet.RUNTIME_ONLY] 或 [ComposeDependencySet.CORE_UI]
 * @param coreScope 核心 Compose 库的作用域；Library 通常传 "api"，Application / 内部模块传 "implementation"
 * @param bomScope BOM platform 的作用域；Library 通常传 "api"，Application / 内部模块传 "implementation"
 */
internal fun Project.configureComposeDependencies(
    dependencySet: ComposeDependencySet,
    coreScope: String = "implementation",
    bomScope: String = "implementation"
) {
    val skyExt = ensureSkyBuildExtension()
    val bomVersion = skyExt.composeBomVersion.get()

    dependencies {
        // BOM platform：统一约束所有 androidx.compose.* 版本
        val composeBom = platform("androidx.compose:compose-bom:$bomVersion")
        add(bomScope, composeBom)
        add("androidTestImplementation", composeBom)

        // Compose Runtime 是所有 Compose 模块的基础
        add(coreScope, "androidx.compose.runtime:runtime")

        if (dependencySet == ComposeDependencySet.CORE_UI) {
            // 核心 UI 套件
            add(coreScope, "androidx.compose.foundation:foundation")
            add(coreScope, "androidx.compose.ui:ui")
            add(coreScope, "androidx.compose.ui:ui-graphics")
            add(coreScope, "androidx.compose.material3:material3")
            add(coreScope, "androidx.compose.ui:ui-tooling-preview")

            // Debug / 测试工具
            add("debugImplementation", "androidx.compose.ui:ui-tooling")
            add("debugImplementation", "androidx.compose.ui:ui-test-manifest")
            add("androidTestImplementation", "androidx.compose.ui:ui-test-junit4")
        }
    }
}
