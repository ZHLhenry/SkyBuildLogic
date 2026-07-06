pluginManagement {
    repositories {
        // 本地 Maven 仓库（build-logic 发布后从此处解析插件）
        mavenLocal()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // build-logic convention 插件发布仓库（Plugin Marker + 主 JAR）
        maven {
            credentials {
                username = "677b4e5b259532263f6b30a6"
                password = "RnVrdxoghjKo"
            }
            url = uri("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic")
            content {
                includeGroupByRegex("sky\\..*")
                includeGroup("com.sky.buildLogic")
            }
        }
        maven {
            url = uri("${rootDir}/build/repo")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "SkyBuildLogic"
include(":app")
includeBuild("buildLogicLib")