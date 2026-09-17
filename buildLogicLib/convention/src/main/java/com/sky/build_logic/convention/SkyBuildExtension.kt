package com.sky.build_logic.convention

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/** Compose BOM 内置默认版本 */
internal const val DEFAULT_COMPOSE_BOM_VERSION = "2026.09.00"

/**
 * SkyBuild 配置扩展，使用者在根项目 build.gradle.kts 中通过 extra 属性进行配置。
 * 插件会自动从 rootProject.extra 中读取 "skyBuild.*" 前缀的属性值，
 * 并创建 SkyBuildExtension 实例供所有子模块共享继承。
 *
 * 使用示例（在根项目 build.gradle.kts 中）：
 * ```kotlin
 * extra["skyBuild.appName"] = "SkyMVVM"
 * extra["skyBuild.applicationId"] = "com.sky.mvvm.sample"
 * extra["skyBuild.versionCode"] = 101
 * extra["skyBuild.versionName"] = "1.0.0"
 * extra["skyBuild.compileSdk"] = 37
 * extra["skyBuild.minSdk"] = 28
 * extra["skyBuild.targetSdk"] = 35
 * extra["skyBuild.enableViewBinding"] = true
 * extra["skyBuild.enableDataBinding"] = true
 * extra["skyBuild.enableBuildConfig"] = true
 * extra["skyBuild.enableCompose"] = false
 * extra["skyBuild.composeBomVersion"] = "2026.02.01"
 * extra["skyBuild.enableLibraryMinify"] = true
 * extra["skyBuild.enableAppMinify"] = true
 * ```
 *
 * 各子模块通过应用 sky convention 插件自动继承上述共享配置，
 * 无需在子模块中重复声明。
 *
 * 注意：除 composeBomVersion、enableLibraryMinify 与 enableAppMinify 外，所有属性均无默认值，消费者必须显式配置，
 * 否则构建时将抛出异常提示配置。composeBomVersion 在 enableCompose=true 时生效，
 * 未配置则使用插件内置默认值 [DEFAULT_COMPOSE_BOM_VERSION]；
 * enableLibraryMinify 与 enableAppMinify 未配置时默认 false（不混淆）。
 *
 * 注意：Product Flavor 配置请使用标准 AGP DSL 在 android {} 块中配置，
 * 因为 AGP 9.x 不允许在 afterEvaluate 中修改 flavorDimensions。
 */
abstract class SkyBuildExtension @Inject constructor(objects: ObjectFactory) {

    /** 编译 SDK 版本（必填） */
    val compileSdk: Property<Int> = objects.property(Int::class.java)

    /** 最低支持 SDK 版本（必填） */
    val minSdk: Property<Int> = objects.property(Int::class.java)

    /** 目标 SDK 版本（必填） */
    val targetSdk: Property<Int> = objects.property(Int::class.java)

    /** 应用 ID（必填） */
    val applicationId: Property<String> = objects.property(String::class.java)

    /** 应用名称（必填） */
    val appName: Property<String> = objects.property(String::class.java)

    /** 版本号 versionCode（必填） */
    val versionCode: Property<Int> = objects.property(Int::class.java)

    /** 版本名称 versionName（必填） */
    val versionName: Property<String> = objects.property(String::class.java)

    /** 是否启用 ViewBinding（必填） */
    val enableViewBinding: Property<Boolean> = objects.property(Boolean::class.java)

    /** 是否启用 DataBinding（必填） */
    val enableDataBinding: Property<Boolean> = objects.property(Boolean::class.java)

    /** 是否启用 BuildConfig 生成（必填） */
    val enableBuildConfig: Property<Boolean> = objects.property(Boolean::class.java)

    /** 是否启用 Compose 支持（必填） */
    val enableCompose: Property<Boolean> = objects.property(Boolean::class.java)

    /**
     * Compose BOM 版本号（可选）。
     * 仅当 [enableCompose] 为 true 时生效；未配置时使用插件内置默认值。
     */
    val composeBomVersion: Property<String> = objects.property(String::class.java)
        .convention(DEFAULT_COMPOSE_BOM_VERSION)

    /**
     * 是否对 Library 模块的 release 产物启用 R8 混淆（可选，默认 false）。
     *
     * 开启后 AAR 的 classes.jar 在打包前即被压缩混淆，公开 API 由模块
     * `src/main/keepRules` 目录下 `.keep` 规则文件保留（约定插件显式汇总传递给 R8）；
     * 模块根目录的 `consumer-rules.keep` 若存在则自动注册为消费者规则随 AAR 分发。
     * 仅作用于 `sky.android.library` / `sky.android.library.common` 插件，不影响 App 模块。
     */
    val enableLibraryMinify: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)

    /**
     * 是否对 App 模块的 release 构建启用 R8 混淆（可选，默认 false）。
     *
     * 开启后 App release 执行 R8 代码压缩与混淆，规则汇总自模块
     * `src/main/keepRules` 目录的 `.keep` 文件；并与各 library 随 AAR 分发的
     * 消费者规则（consumer-rules.keep）协同生效；同时开启资源压缩（isShrinkResources）。
     * 仅影响 release，debug 始终不混淆、不压缩资源。
     */
    val enableAppMinify: Property<Boolean> = objects.property(Boolean::class.java)
        .convention(false)
}
