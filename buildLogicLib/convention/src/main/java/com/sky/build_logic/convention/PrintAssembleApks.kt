package com.sky.build_logic.convention

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal fun Project.configurePrintAssembleApksTask(extension: ApplicationAndroidComponentsExtension) {
    val skyExt = ensureSkyBuildExtension()
    extension.onVariants { variant ->
        val flavorName = variant.flavorName ?: ""
        val buildTypeName = variant.buildType ?: ""

        val apkFolder = variant.artifacts.get(SingleArtifact.APK)
        val loader = variant.artifacts.getBuiltArtifactsLoader()

        val taskName = "renameAndOpen${variant.name.replaceFirstChar { it.uppercase() }}Apk"
        tasks.register(taskName, RenameAndOpenApkTask::class.java) {
            this.apkDirectory.set(apkFolder)
            this.builtArtifactsLoader.set(loader)
            this.appName.set(skyExt.appName.get())
            this.versionName.set(skyExt.versionName.get())
            this.versionCode.set(skyExt.versionCode.get())
            this.flavorName.set(flavorName)
            this.buildTypeName.set(buildTypeName)
            this.openInFinder.set(buildTypeName == "release")
            this.buildOutputDir.set(project.layout.buildDirectory.dir("outputs/renamed-apk"))
        }

        tasks.matching { it.name == "assemble${variant.name.replaceFirstChar { it.uppercase() }}" }
            .configureEach {
                finalizedBy(taskName)
            }
    }
}

internal abstract class RenameAndOpenApkTask : DefaultTask() {

    @get:InputDirectory
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val buildTypeName: Property<String>

    @get:Input
    abstract val openInFinder: Property<Boolean>

    @get:OutputDirectory
    abstract val buildOutputDir: DirectoryProperty

    @TaskAction
    fun taskAction() {
        val dir = apkDirectory.get()
        val builtArtifacts = builtArtifactsLoader.get().load(dir) ?: return

        // 先收集所有 APK 文件信息
        val apkFiles = builtArtifacts.elements.filter { File(it.outputFile).name.endsWith(".apk") }

        // 创建输出目录：build/outputs/renamed-apk/{flavorName}/{buildTypeName}/
        val outputDir = File(
            buildOutputDir.get().asFile,
            "${flavorName.get()}${if (flavorName.get().isNotEmpty()) "/" else ""}${buildTypeName.get()}"
        )
        outputDir.mkdirs()

        // 清理旧的 APK 文件
        outputDir.listFiles { file -> file.extension == "apk" }?.forEach { it.delete() }

        apkFiles.forEach { artifact ->
            val originalFile = File(artifact.outputFile)
            val fallbackVersionName = artifact.versionName ?: versionName.get()
            val fallbackVersionCode = artifact.versionCode ?: versionCode.get()
            val newFileName =
                run {
                    val flavorPart = flavorName.get().takeIf { it.isNotEmpty() }?.let { "${it}_" } ?: ""
                    "${appName.get()}_${flavorPart}${buildTypeName.get()}_v${fallbackVersionName}_${fallbackVersionCode}_${getApkBuildTime()}.apk"
                }
            val renamedFile = File(outputDir, newFileName)
            // 复制一份到 build/outputs/apk/ 目录下
            originalFile.copyTo(renamedFile, overwrite = true)
            println("> Copied APK: ${renamedFile.absolutePath}")
        }

        if (openInFinder.get() && outputDir.exists()) {
            PrintAssembleApksUtil.openFile(outputDir)
        }
    }
}

internal fun getApkBuildTime(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    val time = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    return time.format(formatter)
}

internal object PrintAssembleApksUtil {

    private fun isMac(): Boolean {
        return System.getProperty("os.name").startsWith("Mac")
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name").startsWith("Window")
    }

    fun openFile(file: File) {
        try {
            if (isMac()) {
                println("----------------$file-------------------")
                ProcessBuilder("open", file.absolutePath).start()
            } else {
                ProcessBuilder("explorer", "/select,", file.absolutePath).start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
