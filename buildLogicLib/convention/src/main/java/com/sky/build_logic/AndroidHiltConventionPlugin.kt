import com.sky.build_logic.convention.findLibraryOrDefault
import com.sky.build_logic.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidHiltConventionPlugin : Plugin<Project> {

    companion object {
        /** Hilt 默认版本，消费项目可在 libs.versions.toml 中覆盖 */
        private const val HILT_VERSION = "2.60"

        /**
         * hilt-noop-processor 版本，与 convention 插件同步发布。
         * 尝试从 JAR manifest 读取，读不到则回退到硬编码版本。
         */
        private val NOOP_PROCESSOR_VERSION: String =
            AndroidHiltConventionPlugin::class.java.`package`?.implementationVersion ?: "1.0.0"
    }

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("dagger.hilt.android.plugin")
                apply("com.google.devtools.ksp")
            }

            val catalog = libs

            dependencies {
                // 优先从消费项目的 version catalog 查找，找不到则使用内置默认坐标
                "implementation"(catalog.findLibraryOrDefault(
                    "hilt-android", "com.google.dagger:hilt-android:$HILT_VERSION"))
                "ksp"(catalog.findLibraryOrDefault(
                    "hilt-compiler", "com.google.dagger:hilt-compiler:$HILT_VERSION"))
                "kspAndroidTest"(catalog.findLibraryOrDefault(
                    "hilt-compiler", "com.google.dagger:hilt-compiler:$HILT_VERSION"))
                "kspTest"(catalog.findLibraryOrDefault(
                    "hilt-compiler", "com.google.dagger:hilt-compiler:$HILT_VERSION"))

                // Hilt Gradle 插件在 KSP 场景下仍会向 javac 注入一些仅用于 KAPT 的内部选项，
                // 而 javac 端没有 Hilt 处理器，导致"以下选项未被任何处理程序识别"的警告。
                // 引入一个无操作处理器，仅用于声明识别这些选项以消除警告。
                "annotationProcessor"(catalog.findLibraryOrDefault(
                    "hilt-noop-processor",
                    "com.sky.buildLogic:hilt-noop-processor:$NOOP_PROCESSOR_VERSION"))
            }
        }
    }
}
