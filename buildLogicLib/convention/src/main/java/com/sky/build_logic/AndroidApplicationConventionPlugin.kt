import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.sky.build_logic.convention.ensureSkyBuildExtension
import com.sky.build_logic.convention.applySigningConfigs
import com.sky.build_logic.convention.configureKotlinAndroid
import com.sky.build_logic.convention.configurePrintApksTask
import com.sky.build_logic.convention.configurePrintAssembleApksTask
import com.sky.build_logic.convention.validateForApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val skyExt = ensureSkyBuildExtension()
            skyExt.validateForApplication(path)

            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                compileSdk = skyExt.compileSdk.get()
                defaultConfig {
                    minSdk = skyExt.minSdk.get()
                    targetSdk = skyExt.targetSdk.get()
                    versionCode = skyExt.versionCode.get()
                    versionName = skyExt.versionName.get()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                buildFeatures {
                    viewBinding = skyExt.enableViewBinding.get()
                    dataBinding = skyExt.enableDataBinding.get()
                    buildConfig = skyExt.enableBuildConfig.get()
                }
                applySigningConfigs(this)
                configureKotlinAndroid(this)
            }

            // applicationId 通过 Variant API 惰性设置
            extensions.configure<ApplicationAndroidComponentsExtension> {
                onVariants { variant ->
                    variant.applicationId.set(skyExt.applicationId)
                }
                configurePrintApksTask(this)
                configurePrintAssembleApksTask(this)
            }
        }
    }

}
