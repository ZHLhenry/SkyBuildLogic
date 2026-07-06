package com.sky.build_logic.convention

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

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
 * extra["skyBuild.compileSdk"] = 36
 * extra["skyBuild.minSdk"] = 28
 * extra["skyBuild.targetSdk"] = 35
 * extra["skyBuild.enableViewBinding"] = true
 * extra["skyBuild.enableDataBinding"] = true
 * extra["skyBuild.enableBuildConfig"] = true
 * extra["skyBuild.enableCompose"] = false
 * ```
 *
 * 各子模块通过应用 sky convention 插件自动继承上述共享配置，
 * 无需在子模块中重复声明。
 *
 * 注意：所有属性均无默认值，消费者必须显式配置，否则构建时将抛出异常提示配置。
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
}
