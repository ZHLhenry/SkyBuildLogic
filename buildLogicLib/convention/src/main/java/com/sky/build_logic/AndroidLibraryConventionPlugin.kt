import com.android.build.api.dsl.LibraryExtension
import com.sky.build_logic.convention.configureAndroidLibrary
import com.sky.build_logic.convention.ensureSkyBuildExtension
import com.sky.build_logic.convention.registerSharedSkyBuildExtension
import com.sky.build_logic.convention.validateForLibrary
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 注册共享扩展到 rootProject，供 app 等依赖模块继承
            registerSharedSkyBuildExtension()
            val skyExt = ensureSkyBuildExtension().apply { validateForLibrary(path) }

            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.plugin.parcelize")
                if (skyExt.enableCompose.get()) {
                    apply("org.jetbrains.kotlin.plugin.compose")
                }
            }
            extensions.configure<LibraryExtension> {
                configureAndroidLibrary(commonExtension = this)
            }
            dependencies {
                add("testImplementation", kotlin("test"))
                add("androidTestImplementation", kotlin("test"))
            }
        }
    }
}
