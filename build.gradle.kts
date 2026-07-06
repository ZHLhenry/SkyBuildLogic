// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// SkyBuild 共享配置（所有属性均为必填，缺少任意一项构建时将抛出异常提示）
extra["skyBuild.compileSdk"] = 36
extra["skyBuild.minSdk"] = 28
extra["skyBuild.targetSdk"] = 35
extra["skyBuild.applicationId"] = "com.sky.skybuildlogic"
extra["skyBuild.appName"] = "SkyBuildLogic"
extra["skyBuild.versionCode"] = 100
extra["skyBuild.versionName"] = "1.0.0"
extra["skyBuild.enableViewBinding"] = true
extra["skyBuild.enableDataBinding"] = true
extra["skyBuild.enableBuildConfig"] = true
extra["skyBuild.enableCompose"] = true