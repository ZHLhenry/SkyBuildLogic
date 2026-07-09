package com.sky.build_logic.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.findLibraryOrDefault(alias: String, defaultNotation: String): Any {
    return findLibrary(alias).orElse(null) ?: defaultNotation
}

internal val Project.skyBuildExtension: SkyBuildExtension
    get() = extensions.getByType(SkyBuildExtension::class.java)

internal fun Project.ensureSkyBuildExtension(): SkyBuildExtension {
    // Case 1: 本地扩展已存在（由插件内部通过 extensions.create 创建）
    extensions.findByType(SkyBuildExtension::class.java)?.let { localExt ->
        logSkyBuildConfigOnce(this, localExt)
        return localExt
    }

    // Case 2: rootExt 已存在，创建本地扩展并惰性链接
    rootProject.extensions.findByType(SkyBuildExtension::class.java)?.let { rootExt ->
        val localExt = extensions.create("skyBuild", SkyBuildExtension::class.java)
        linkToRoot(localExt, rootExt)
        logSkyBuildConfigOnce(this, localExt)
        return localExt
    }

    // Case 3: 第一个调用的项目 - 创建 rootExt 从 rootProject.extra 读取共享值
    // 这确保第一个项目（通常是 app）也能获取正确的共享配置
    val rootExt = rootProject.extensions.findByType(SkyBuildExtension::class.java)
        ?: createRootExtFromExtra()
    if (rootExt != null && rootProject != this) {
        // 当前项目不是 rootProject，创建本地扩展并链接到 rootExt
        val localExt = extensions.create("skyBuild", SkyBuildExtension::class.java)
        linkToRoot(localExt, rootExt)
        logSkyBuildConfigOnce(this, localExt)
        return localExt
    }
    // 当前项目就是 rootProject（不太可能但安全处理）
    val localExt = rootExt ?: extensions.create("skyBuild", SkyBuildExtension::class.java)
    logSkyBuildConfigOnce(this, localExt)
    return localExt
}

/**
 * 将 localExt 的所有属性链接到 rootExt，形成惰性 Provider 链。
 * 若 rootExt 某属性未配置，localExt 对应属性同样保持未配置状态。
 */
private fun linkToRoot(localExt: SkyBuildExtension, rootExt: SkyBuildExtension) {
    localExt.compileSdk.set(rootExt.compileSdk)
    localExt.minSdk.set(rootExt.minSdk)
    localExt.targetSdk.set(rootExt.targetSdk)
    localExt.appName.set(rootExt.appName)
    localExt.versionCode.set(rootExt.versionCode)
    localExt.versionName.set(rootExt.versionName)
    localExt.enableViewBinding.set(rootExt.enableViewBinding)
    localExt.enableDataBinding.set(rootExt.enableDataBinding)
    localExt.enableBuildConfig.set(rootExt.enableBuildConfig)
    localExt.enableCompose.set(rootExt.enableCompose)
    localExt.applicationId.set(rootExt.applicationId)
}

/**
 * 从 rootProject.extra 读取共享配置值并填充到 SkyBuildExtension。
 * rootProject.extra 在 root build.gradle.kts 中设置，早于所有子项目评估。
 */
private fun Project.populateFromExtra(rootExt: SkyBuildExtension) {
    val rootExtra = rootProject.extensions.extraProperties
    fun <T> extraProp(key: String, setter: (T) -> Unit) {
        if (rootExtra.has(key)) {
            @Suppress("UNCHECKED_CAST")
            setter(rootExtra.get(key) as T)
        }
    }

    extraProp<Int>("skyBuild.compileSdk") { rootExt.compileSdk.set(it) }
    extraProp<Int>("skyBuild.minSdk") { rootExt.minSdk.set(it) }
    extraProp<Int>("skyBuild.targetSdk") { rootExt.targetSdk.set(it) }
    extraProp<String>("skyBuild.applicationId") { rootExt.applicationId.set(it) }
    extraProp<String>("skyBuild.appName") { rootExt.appName.set(it) }
    extraProp<Int>("skyBuild.versionCode") { rootExt.versionCode.set(it) }
    extraProp<String>("skyBuild.versionName") { rootExt.versionName.set(it) }
    extraProp<Boolean>("skyBuild.enableViewBinding") { rootExt.enableViewBinding.set(it) }
    extraProp<Boolean>("skyBuild.enableDataBinding") { rootExt.enableDataBinding.set(it) }
    extraProp<Boolean>("skyBuild.enableBuildConfig") { rootExt.enableBuildConfig.set(it) }
    extraProp<Boolean>("skyBuild.enableCompose") { rootExt.enableCompose.set(it) }
}

/**
 * 从 rootProject.extra 创建 rootExt。
 * rootProject.extra 在 root build.gradle.kts 中设置，早于所有子项目评估。
 */
private fun Project.createRootExtFromExtra(): SkyBuildExtension? {
    val rootExt = rootProject.extensions.create("skyBuild", SkyBuildExtension::class.java)
    populateFromExtra(rootExt)
    return rootExt
}

/**
 * 验证 Library 模块所需的 skyBuild 属性是否已全部配置。
 * 若缺少必要属性，抛出 GradleException 提示消费者补充配置。
 */
internal fun SkyBuildExtension.validateForLibrary(projectPath: String) {
    val missing = mutableListOf<String>()
    if (!compileSdk.isPresent) missing += "skyBuild.compileSdk"
    if (!minSdk.isPresent) missing += "skyBuild.minSdk"
    if (missing.isNotEmpty()) {
        throw org.gradle.api.GradleException(
            """
            |
            |[SkyBuild] 项目 $projectPath 缺少必要的 skyBuild 配置，请在根项目 build.gradle.kts 中补充：
            |  ${missing.joinToString("\n  ")}
            |
            |示例：
            |  extra["skyBuild.compileSdk"] = 36
            |  extra["skyBuild.minSdk"] = 28
            |
            """.trimMargin()
        )
    }
}

/**
 * 验证 Application 模块所需的 skyBuild 属性是否已全部配置。
 * 若缺少必要属性，抛出 GradleException 提示消费者补充配置。
 */
internal fun SkyBuildExtension.validateForApplication(projectPath: String) {
    val missing = mutableListOf<String>()
    if (!compileSdk.isPresent) missing += "skyBuild.compileSdk"
    if (!minSdk.isPresent) missing += "skyBuild.minSdk"
    if (!targetSdk.isPresent) missing += "skyBuild.targetSdk"
    if (!applicationId.isPresent) missing += "skyBuild.applicationId"
    if (!appName.isPresent) missing += "skyBuild.appName"
    if (!versionCode.isPresent) missing += "skyBuild.versionCode"
    if (!versionName.isPresent) missing += "skyBuild.versionName"
    if (!enableViewBinding.isPresent) missing += "skyBuild.enableViewBinding"
    if (!enableDataBinding.isPresent) missing += "skyBuild.enableDataBinding"
    if (!enableBuildConfig.isPresent) missing += "skyBuild.enableBuildConfig"
    if (!enableCompose.isPresent) missing += "skyBuild.enableCompose"
    if (missing.isNotEmpty()) {
        throw org.gradle.api.GradleException(
            """
            |
            |[SkyBuild] 项目 $projectPath 缺少必要的 skyBuild 配置，请在根项目 build.gradle.kts 中补充：
            |  ${missing.joinToString("\n  ")}
            |
            |示例：
            |  extra["skyBuild.compileSdk"] = 36
            |  extra["skyBuild.minSdk"] = 28
            |  extra["skyBuild.targetSdk"] = 35
            |  extra["skyBuild.applicationId"] = "com.example.app"
            |  extra["skyBuild.appName"] = "MyApp"
            |  extra["skyBuild.versionCode"] = 100
            |  extra["skyBuild.versionName"] = "1.0.0"
            |  extra["skyBuild.enableViewBinding"] = true
            |  extra["skyBuild.enableDataBinding"] = true
            |  extra["skyBuild.enableBuildConfig"] = true
            |  extra["skyBuild.enableCompose"] = true
            |
            """.trimMargin()
        )
    }
}

private const val SKY_BUILD_LOGGED_KEY = "com.sky.build_logic.skyBuildLogged"

private fun logSkyBuildConfigOnce(project: Project, skyExt: SkyBuildExtension) {
    if (project.extensions.extraProperties.has(SKY_BUILD_LOGGED_KEY)) return
    project.extensions.extraProperties.set(SKY_BUILD_LOGGED_KEY, true)

    project.afterEvaluate {
        val compileSdk = if (skyExt.compileSdk.isPresent) skyExt.compileSdk.get() else "(not set)"
        val minSdk = if (skyExt.minSdk.isPresent) skyExt.minSdk.get() else "(not set)"
        val targetSdk = if (skyExt.targetSdk.isPresent) skyExt.targetSdk.get() else "(not set)"
        val appId = if (skyExt.applicationId.isPresent) skyExt.applicationId.get() else "(not set)"
        val appName = if (skyExt.appName.isPresent) skyExt.appName.get() else "(not set)"
        val versionCode = if (skyExt.versionCode.isPresent) skyExt.versionCode.get() else "(not set)"
        val versionName = if (skyExt.versionName.isPresent) skyExt.versionName.get() else "(not set)"
        val enableViewBinding = if (skyExt.enableViewBinding.isPresent) skyExt.enableViewBinding.get() else "(not set)"
        val enableDataBinding = if (skyExt.enableDataBinding.isPresent) skyExt.enableDataBinding.get() else "(not set)"
        val enableBuildConfig = if (skyExt.enableBuildConfig.isPresent) skyExt.enableBuildConfig.get() else "(not set)"
        val enableCompose = if (skyExt.enableCompose.isPresent) skyExt.enableCompose.get() else "(not set)"

        project.logger.lifecycle(
            """
            |
            | [SkyBuild] ${project.path} final config:
            |    compileSdk       = $compileSdk
            |    minSdk           = $minSdk
            |    targetSdk        = $targetSdk
            |    applicationId    = $appId
            |    appName          = $appName
            |    versionCode      = $versionCode
            |    versionName      = $versionName
            |    enableViewBinding  = $enableViewBinding
            |    enableDataBinding  = $enableDataBinding
            |    enableBuildConfig  = $enableBuildConfig
            |    enableCompose      = $enableCompose
            |
            """.trimMargin()
        )
    }
}

/**
 * 在 rootProject 上创建共享的 skyBuild 扩展。
 * 从 rootProject.extra 读取共享配置值（在 root build.gradle.kts 中设置）。
 * 这些值在根项目构建脚本中设置，早于所有子项目评估。
 */
internal fun Project.registerSharedSkyBuildExtension(): SkyBuildExtension {
    rootProject.extensions.findByType(SkyBuildExtension::class.java)?.let {
        return it
    }

    val rootExt = rootProject.extensions.create("skyBuild", SkyBuildExtension::class.java)
    populateFromExtra(rootExt)
    return rootExt
}
