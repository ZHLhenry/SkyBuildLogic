import com.sky.build_logic.convention.registerSharedSkyBuildExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 此插件仅负责注册 skyBuild 共享扩展，使子模块自动继承根项目的配置。
 *
 * 共享配置在根项目 build.gradle.kts 中通过 extra 属性设置：
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
 * Product Flavor 配置请使用标准 AGP DSL 在 android {} 块中完成，
 * 因为 AGP 9.x 不允许在 afterEvaluate 中修改 flavorDimensions。
 *
 * 使用示例：
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.sky.android.application)
 *     alias(libs.plugins.sky.android.application.flavors)
 * }
 *
 * android {
 *     namespace = "com.example.myapp"
 *
 *     flavorDimensions += "contentType"
 *     productFlavors {
 *         create("dev") {
 *             dimension = "contentType"
 *             manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
 *         }
 *         create("uat") {
 *             dimension = "contentType"
 *             manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
 *         }
 *         create("prod") {
 *             dimension = "contentType"
 *             manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
 *         }
 *     }
 * }
 * ```
 */
class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 注册共享扩展到 rootProject，供其他模块继承
            registerSharedSkyBuildExtension()
        }
    }
}
