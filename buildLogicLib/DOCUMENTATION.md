# SkyBuildLogic - buildLogicLib 使用指南

## 一、库概述

`buildLogicLib` 是一个 **Android Gradle Convention Plugins** 构建库，提供一组预定义的 Gradle 插件，用于统一 Android 项目的构建配置。使用者通过在自己的项目中引入这些插件，即可自动获得标准化的构建行为，避免在每个模块中重复编写构建逻辑。

### 核心定位
- 作为**第三方 Gradle 构建库**发布到 Maven 仓库，供其他项目集成使用
- 所有配置通过 `SkyBuildExtension` 扩展集中管理，子模块自动继承
- Compose 支持为可选功能，由 `enableCompose` 开关控制
- 基于 AGP 9.x + Kotlin 2.x 构建

---

## 二、模块结构

```
buildLogicLib/
├── convention/              # 核心模块：Convention 插件集合
│   ├── build.gradle.kts     # 插件注册、Maven 发布配置
│   └── src/main/java/com/sky/build_logic/
│       ├── AndroidApplicationConventionPlugin.kt       # Android 应用插件
│       ├── AndroidLibraryConventionPlugin.kt            # Android 库插件（含 Parcelize）
│       ├── AndroidCommonLibraryConventionPlugin.kt      # Android 通用库插件（轻量，无 Parcelize）
│       ├── AndroidHiltConventionPlugin.kt               # Hilt 依赖注入插件
│       ├── AndroidMavenPublishConventionPlugin.kt       # Maven 发布插件
│       ├── AndroidApplicationFlavorsConventionPlugin.kt # Flavor 配置插件
│       ├── JvmLibraryConventionPlugin.kt                # JVM 库插件
│       └── convention/
│           ├── SkyBuildExtension.kt     # 配置扩展定义（11 个属性）
│           ├── ProjectExtensions.kt     # 扩展注册/验证/日志/Provider 链工具
│           ├── KotlinAndroid.kt         # Kotlin + Android 编译配置（两条路径）
│           ├── SigningConfigs.kt        # 签名配置（从 local.properties 读取）
│           ├── AppBuildType.kt          # 构建类型枚举（DEBUG/RELEASE）
│           ├── PrintAssembleApks.kt     # APK 重命名复制 & 自动打开目录任务
│           └── PrintTestApks.kt         # 测试 APK 路径输出任务
├── hilt-noop-processor/     # 辅助模块：Hilt KSP 场景下的无操作注解处理器
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/sky/buildlogic/hilt/noop/
│       │   └── HiltNoOpProcessor.java
│       └── resources/META-INF/services/
│           └── javax.annotation.processing.Processor
├── settings.gradle.kts      # 包含 convention + hilt-noop-processor 两个子项目
└── gradle.properties        # 并行构建、缓存、按需配置
```

---

## 三、插件清单

| 插件 ID | 实现类 | 用途 | 自动应用的底层插件 |
|---------|--------|------|------------------|
| `sky.android.application` | `AndroidApplicationConventionPlugin` | Android 应用模块完整构建配置 | `com.android.application`、可选 `org.jetbrains.kotlin.plugin.compose` |
| `sky.android.library` | `AndroidLibraryConventionPlugin` | Android 库模块构建（含 Parcelize + Lint） | `com.android.library`、`org.jetbrains.kotlin.plugin.parcelize`、可选 `org.jetbrains.kotlin.plugin.compose` |
| `sky.android.library.common` | `AndroidCommonLibraryConventionPlugin` | Android 通用库构建（轻量，无 Parcelize） | `com.android.library`、可选 `org.jetbrains.kotlin.plugin.compose` |
| `sky.android.hilt` | `AndroidHiltConventionPlugin` | Hilt 依赖注入 + KSP 配置 | `dagger.hilt.android.plugin`、`com.google.devtools.ksp` |
| `sky.android.publish` | `AndroidMavenPublishConventionPlugin` | Android Library 发布到 Maven 仓库 | `maven-publish` |
| `sky.android.application.flavors` | `AndroidApplicationFlavorsConventionPlugin` | 注册共享 skyBuild 扩展（Flavor 场景） | 无 |
| `sky.jvm.library` | `JvmLibraryConventionPlugin` | 纯 JVM Kotlin 库配置 | `org.jetbrains.kotlin.jvm` |

---

## 四、核心机制

### 4.1 SkyBuildExtension — 统一配置扩展

`SkyBuildExtension` 是整个库的配置核心，定义了所有模块共享的配置属性：

| 属性 | 类型 | 说明 | Application 必填 | Library 必填 |
|------|------|------|:---:|:---:|
| `compileSdk` | `Property<Int>` | 编译 SDK 版本 | ✅ | ✅ |
| `minSdk` | `Property<Int>` | 最低支持 SDK | ✅ | ✅ |
| `targetSdk` | `Property<Int>` | 目标 SDK 版本 | ✅ | ❌ |
| `applicationId` | `Property<String>` | 应用 ID（通过 Variant API 惰性设置） | ✅ | ❌ |
| `appName` | `Property<String>` | 应用名称（用于 APK 重命名） | ✅ | ❌ |
| `versionCode` | `Property<Int>` | 版本号 | ✅ | ❌ |
| `versionName` | `Property<String>` | 版本名称 | ✅ | ❌ |
| `enableViewBinding` | `Property<Boolean>` | 启用 ViewBinding | ✅ | ❌ |
| `enableDataBinding` | `Property<Boolean>` | 启用 DataBinding | ✅ | ❌ |
| `enableBuildConfig` | `Property<Boolean>` | 启用 BuildConfig 生成 | ✅ | ❌ |
| `enableCompose` | `Property<Boolean>` | 启用 Compose 支持（控制是否应用 Compose Compiler 插件） | ✅ | ❌ |

**所有属性均无默认值**，消费者必须显式配置，否则构建时抛出异常并给出配置示例提示。

### 4.2 配置共享机制

配置通过 `rootProject.extra` 属性设置，插件自动读取并创建共享的 `SkyBuildExtension` 实例：

```
rootProject.extra["skyBuild.*"]
        ↓  (第一个插件调用时)
  rootExt (rootProject.extensions 上的 SkyBuildExtension)
        ↓  linkToRoot()
  localExt (子模块 extensions 上的 SkyBuildExtension)
```

**详细流程：**

1. **Library / Flavors 插件**先调用 `registerSharedSkyBuildExtension()`，在 `rootProject.extensions` 上创建 rootExt 并从 `rootProject.extra` 填充值
2. **所有插件**调用 `ensureSkyBuildExtension()` 获取本地扩展：
   - 若本地已存在 `SkyBuildExtension` → 直接返回
   - 若 rootExt 已存在 → 创建 localExt 并通过 `linkToRoot()` 建立 Provider 链
   - 若都不存在（首个插件调用）→ 从 `rootProject.extra` 创建 rootExt，再链接
3. `linkToRoot()` 将 localExt 的每个属性 `set(rootExt.xxx)`，形成惰性 Provider 链，配置变更自动传播

### 4.3 两条编译配置路径

库内部提供两条不同的编译配置函数，适用于不同场景：

| 函数 | 使用插件 | 参数类型 | 特点 |
|------|---------|---------|------|
| `configureKotlinAndroid()` | Application、CommonLibrary | `CommonExtension` | 完整配置：compileSdk/minSdk、Java 17、packaging 排除规则、Kotlin JVM 17 + opt-in |
| `configureAndroidLibrary()` | Library | `LibraryExtension` | 基础配置 + **Lint 配置**：checkDependencies=true、禁用 UnusedResources/TypographyQuotes、warningsAsErrors=true、HTML+XML 报告 |

**Kotlin 编译统一配置：**
- Java/Kotlin 兼容性：Java 17
- JVM Target：JVM 17
- 自动 opt-in：`RequiresOptIn`、`ExperimentalCoroutinesApi`、`FlowPreview`
- 可选 `warningsAsErrors`：通过 `gradle.properties` 中的 `warningsAsErrors=true` 开启

**Application 模块的 packaging 排除规则：**
- JNI libs：`META-INF/{AL2.0,LGPL2.1}`
- Resources：`**/*.md`、`**/*.version`、`**/*.properties`、`META-INF/{AL2.0,LGPL2.1}`、`META-INF/CHANGES`、`DebugProbesKt.bin`、`kotlin-tooling-metadata.json` 等

### 4.4 签名配置

应用模块的签名信息**强制从 `local.properties` 读取**：

```properties
app.storeFile=path/to/your.jks
app.storePassword=your_password
app.keyAlias=your_key_alias
```

- debug/release 构建类型均使用同一套签名配置（来自 local.properties）
- `keyPassword` 与 `storePassword` 相同
- 两个构建类型的 `isMinifyEnabled` 均为 `false`
- 均配置 ABI 过滤器：`arm64-v8a`、`armeabi-v7a`、`x86`、`x86_64`
- `applicationIdSuffix`：DEBUG 和 RELEASE 均无后缀（枚举值 `null`）

### 4.5 APK 重命名与输出

构建完成后自动将 APK **复制**（非移动）到 `build/outputs/renamed-apk/` 目录：

**输出目录结构：** `build/outputs/renamed-apk/{flavorName}/{buildType}/`

**命名格式：** `{appName}_{flavorName}_{buildType}_v{versionName}_{versionCode}_{buildTime}.apk`

- `buildTime` 格式：`yyyyMMddHHmmss`，时区 `Asia/Shanghai`
- 若无 Flavor，`flavorName` 部分省略
- 复制前自动清理输出目录中的旧 APK 文件
- **Release 构建完成后自动在 Finder/Explorer 中打开输出目录**
- 任务名：`renameAndOpen{VariantName}Apk`，在对应 `assemble{Variant}` 完成后执行

### 4.6 测试 APK 输出

对包含 `androidTest` 源文件的变体，注册 `{variantName}PrintTestApk` 任务，在构建时输出测试 APK 的文件路径。仅当存在非生成的测试源文件时才输出。

### 4.7 Hilt 集成

`sky.android.hilt` 插件自动完成以下操作：

1. 应用 `dagger.hilt.android.plugin` 和 `com.google.devtools.ksp`
2. 从消费项目的 version catalog 查找 Hilt 依赖，**找不到则使用内置默认版本（2.60）**：
   - `implementation` ← `hilt-android`
   - `ksp` / `kspAndroidTest` / `kspTest` ← `hilt-compiler`
3. 引入 `hilt-noop-processor` 到 `annotationProcessor`，消除 KSP 场景下 javac 的"未识别选项"警告

**hilt-noop-processor 版本获取机制：**
- 优先从 JAR `MANIFEST.MF` 的 `Implementation-Version` 读取（与 convention 插件版本同步）
- 读不到则回退到硬编码版本 `1.0.0`

**HiltNoOpProcessor 工作原理：**
- 继承 `AbstractProcessor`，声明 `@SupportedAnnotationTypes("*")`
- `process()` 方法为空（返回 false），仅通过 `getSupportedOptions()` 声明识别 Hilt 注入的编译器选项（如 `dagger.fastInit`、`dagger.hilt.android.internal.*` 等），消除 javac 警告

### 4.8 Maven 发布

`sky.android.publish` 插件自动配置 Android Library 的 Maven 发布：

**发布内容：**
- `bundleReleaseAar` — 主 AAR 发布物
- `generateSourcesJar` — 源码 JAR（classifier: sources）
- `generateJavadocJar` — Javadoc JAR（classifier: javadoc）
- POM 中自动写入 `implementation` 依赖

**发布配置从 `local.properties` 读取：**

```properties
mavenCentral.username=your_username
mavenCentral.password=your_password
mavenCentral.groupId=com.yourcompany
mavenCentral.artifactId=your-library
mavenCentral.version=1.0.0
mavenCentral.repoUrl=https://your-maven-repo.com/releases
```

所有配置项均为必填，缺少时构建报错。

---

## 五、使用指南

### 5.1 集成准备

#### 1. 引入插件依赖

在消费项目的 `settings.gradle.kts` 中配置插件仓库：

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
        }
        maven {
            credentials {
                username = "677b4e5b259532263f6b30a6"
                password = "RnVrdxoghjKo"
            }
            url = uri("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/central")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        fun aliyunMaven(repoUrl: String) {
            maven {
                credentials {
                    username = "677b4e5b259532263f6b30a6"
                    password = "RnVrdxoghjKo"
                }
                url = uri(repoUrl)
            }
        }
        aliyunMaven("https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic")
    }
}
```

在 `gradle/libs.versions.toml` 中声明插件版本：

```toml
[versions]
buildLogic = "1.1.1"

[plugins]
sky-android-application = { id = "sky.android.application", version.ref = "buildLogic" }
sky-android-application-flavors = { id = "sky.android.application.flavors", version.ref = "buildLogic" }
sky-android-hilt = { id = "sky.android.hilt", version.ref = "buildLogic" }
sky-android-library = { id = "sky.android.library", version.ref = "buildLogic" }
sky-android-library-common = { id = "sky.android.library.common", version.ref = "buildLogic" }
sky-android-publish = { id = "sky.android.publish", version.ref = "buildLogic" }
sky-jvm-library = { id = "sky.jvm.library", version.ref = "buildLogic" }
```

#### 2. 配置 local.properties

```properties
# 签名配置（应用模块必需）
app.storeFile=path/to/your.jks
app.storePassword=your_store_password
app.keyAlias=your_key_alias

# Maven 发布配置（使用 sky.android.publish 时必需）
mavenCentral.username=your_username
mavenCentral.password=your_password
mavenCentral.groupId=com.yourcompany
mavenCentral.artifactId=your-library
mavenCentral.version=发布的版本号，例如:x.y.z
mavenCentral.repoUrl=https://your-maven-repo.com/releases
```

### 5.2 根项目配置

在根项目 `build.gradle.kts` 中设置共享配置：

```kotlin
// 插件依赖已由 convention 插件通过 api 传递，无需在根项目重复声明
// SkyBuild 共享配置（所有属性均为必填，缺少任意一项构建时将抛出异常提示）
extra["skyBuild.appName"] = "SkyMVVM"
extra["skyBuild.applicationId"] = "com.sky.mvvm.sample"
extra["skyBuild.versionCode"] = 100
extra["skyBuild.versionName"] = "1.0.0"
extra["skyBuild.compileSdk"] = 36
extra["skyBuild.minSdk"] = 28
extra["skyBuild.targetSdk"] = 35
extra["skyBuild.enableViewBinding"] = true
extra["skyBuild.enableDataBinding"] = true
extra["skyBuild.enableBuildConfig"] = true
extra["skyBuild.enableCompose"] = true
```

> **说明：** 使用 convention 插件后，以下插件的 classpath 已由 convention 模块通过 `api` 依赖自动传递，
> 根项目**无需**再声明 `plugins { ... apply false }`，各子模块直接通过 sky convention 插件应用即可：
>
> | 插件 ID | 由插件内部 apply |
> |---------|-----------------|
> | `com.android.application` | `sky.android.application` |
> | `com.android.library` | `sky.android.library` / `sky.android.library.common` |
> | `org.jetbrains.kotlin.android` | `sky.android.application` / `sky.android.library` / `sky.android.library.common` |
> | `org.jetbrains.kotlin.jvm` | `sky.jvm.library` |
> | `org.jetbrains.kotlin.plugin.compose` | `sky.android.application` / `sky.android.library`（当 `enableCompose = true`） |
> | `com.google.devtools.ksp` | `sky.android.hilt` |
> | `dagger.hilt.android.plugin` | `sky.android.hilt` |

### 5.3 应用模块 (app)

```kotlin
// app/build.gradle.kts
plugins {
    alias(libs.plugins.sky.android.application)
    // 可选：如果需要 Product Flavor
    alias(libs.plugins.sky.android.application.flavors)
    // 可选：如果使用 Hilt
    alias(libs.plugins.sky.android.hilt)
}

android {
    namespace = "com.example.myapp"

    // Flavor 配置使用标准 AGP DSL（AGP 9.x 不允许在 afterEvaluate 中修改）
    flavorDimensions += "contentType"
    productFlavors {
        create("dev") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
        create("uat") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
        create("prod") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_launcher"
        }
    }
}

dependencies {
    // hilt-noop-processor  如果引用了alias(libs.plugins.sky.android.hilt)插件，就必须引用这个
    annotationProcessor(libs.hilt.noop.processor)
}
```

**插件自动完成的配置：**
- `compileSdk`、`minSdk`、`targetSdk` 从 skyBuild 读取
- `versionCode`、`versionName` 从 skyBuild 读取
- `applicationId` 通过 Variant API 惰性设置
- `viewBinding`、`dataBinding`、`buildConfig` 从 skyBuild 读取
- 签名配置从 local.properties 读取
- `testInstrumentationRunner` = `androidx.test.runner.AndroidJUnitRunner`
- `vectorDrawables.useSupportLibrary` = `true`
- 若 `enableCompose = true`，自动应用 Compose Compiler 插件
- 注册 APK 重命名任务和测试 APK 输出任务

### 5.4 Android Library 模块（含 Parcelize + Lint）

```kotlin
// feature/build.gradle.kts
plugins {
    alias(libs.plugins.sky.android.library)
}

android {
    namespace = "com.example.feature"
}

dependencies {
    // 项目依赖...
}
```

**插件自动完成的配置：**
- `compileSdk`、`minSdk` 从 skyBuild 读取
- 应用 `kotlin-parcelize` 插件
- 若 `enableCompose = true`，自动应用 Compose Compiler 插件
- Lint 配置：`checkDependencies=true`、禁用 `UnusedResources`/`TypographyQuotes`、`warningsAsErrors=true`、生成 HTML+XML 报告
- 自动添加 `testImplementation(kotlin("test"))` 和 `androidTestImplementation(kotlin("test"))`

### 5.5 通用 Library 模块（无 Parcelize，无 Lint）

```kotlin
// core/build.gradle.kts
plugins {
    alias(libs.plugins.sky.android.library.common)
}

android {
    namespace = "com.example.core"
}
```

**与 `sky.android.library` 的区别：**
- 不应用 `kotlin-parcelize` 插件
- 不配置 Lint 规则
- 使用 `configureKotlinAndroid`（CommonExtension 路径），包含 packaging 排除规则

### 5.6 JVM 纯 Kotlin 库模块

```kotlin
// utils/build.gradle.kts
plugins {
    alias(libs.plugins.sky.jvm.library)
}

dependencies {
    // 项目依赖...
}
```

**插件自动完成的配置：**
- 应用 `org.jetbrains.kotlin.jvm` 插件
- Java 17 兼容性
- Kotlin JVM Target 17
- 自动 opt-in 和可选 warningsAsErrors

### 5.7 发布 Library 到 Maven

```kotlin
// mylibrary/build.gradle.kts
plugins {
    alias(libs.plugins.sky.android.library)
    alias(libs.plugins.sky.android.publish)
}

android {
    namespace = "com.example.mylibrary"
}
```

发布时自动：
- 生成 AAR + sources JAR + javadoc JAR
- 将 `implementation` 依赖写入 POM
- 发布到 `local.properties` 中配置的 Maven 仓库

### 5.8 Hilt 依赖注入

```kotlin
// 在需要使用 Hilt 的模块中
plugins {
    alias(libs.plugins.sky.android.hilt)
}
```

自动完成：
- 应用 Hilt Gradle 插件和 KSP 插件
- 添加 `hilt-android` 到 `implementation`
- 添加 `hilt-compiler` 到 `ksp`、`kspAndroidTest`、`kspTest`
- 添加 `hilt-noop-processor` 到 `annotationProcessor`（消除 KSP 场景下 javac 警告）

**依赖查找 fallback 机制：** 优先从消费项目的 `libs.versions.toml` 查找（如 `hilt-android`、`hilt-compiler`、`hilt-noop-processor`），找不到则使用内置默认坐标（Hilt 版本 `2.60`，noop-processor 版本从 JAR manifest 读取）。

---

## 六、构建时日志

每个应用了 sky 插件的模块在构建时会输出最终的 SkyBuild 配置（每个项目仅输出一次）：

```
 [SkyBuild] :app final config:
    compileSdk       = 36
    minSdk           = 28
    targetSdk        = 35
    applicationId    = com.sky.mvvm.sample
    appName          = SkyMVVM
    versionCode      = 101
    versionName      = 1.0.0
    enableViewBinding  = true
    enableDataBinding  = true
    enableBuildConfig  = true
    enableCompose      = true
```

未配置的属性显示为 `(not set)`。日志在 `afterEvaluate` 阶段输出。

---

## 七、发布 buildLogicLib 自身

### 版本管理

在 `local.properties` 中配置：

```properties
buildLogic.version=1.0.0
buildLogic.repoUrl=https://packages.aliyun.com/6732fc8f356ccaf8531a1487/maven/skybuildlogic
buildLogic.username=your_username
buildLogic.password=your_password
```

### 发布命令

```bash
cd buildLogicLib
./gradlew publish
```

### 发布内容

- **convention 模块**：
  - 主 JAR（含 7 个 Gradle 插件描述符）
  - 7 个 Plugin Marker Maven 发布物（每个插件 ID 对应一个）
  - JAR 的 `MANIFEST.MF` 中包含 `Implementation-Version` 属性
- **hilt-noop-processor 模块**：独立 JAR
- convention 的 `publish` 任务自动依赖 hilt-noop-processor 的 `publish` 任务，确保同步发布

---

## 八、技术栈要求

| 依赖 | 版本 |
|------|------|
| AGP (Android Gradle Plugin) | 9.x（本库编译使用 9.2.1） |
| Kotlin | 2.x（本库编译使用 2.4.0） |
| KSP | 与 Kotlin 版本匹配（本库使用 2.3.9） |
| Hilt | 2.60（内置默认版本） |
| Java | 17 |
| Gradle | 与 AGP 9.x 兼容的版本 |

### convention 模块的 api 依赖

```
api(com.android.tools.build:gradle)           // AGP
api(org.jetbrains.kotlin:kotlin-gradle-plugin) // Kotlin Gradle Plugin
api(com.google.devtools.ksp:gradle-plugin)     // KSP Gradle Plugin
api(org.jetbrains.kotlin:compose-compiler-gradle-plugin) // Compose Compiler Plugin
api(com.google.dagger:hilt-android-gradle-plugin)        // Hilt Gradle Plugin
```

---

## 九、注意事项

1. **所有 skyBuild 属性必须显式配置**，不设默认值，缺少时构建会报错并给出配置示例
2. **签名配置强制从 `local.properties` 读取**，不硬编码在构建脚本中
3. **Product Flavor 必须使用标准 AGP DSL** 在 `android {}` 块中配置，AGP 9.x 不允许在 `afterEvaluate` 中修改 flavorDimensions
4. **hilt-noop-processor 版本与 convention 插件同步**，通过 JAR manifest 的 `Implementation-Version` 自动获取
5. **Version Catalog 依赖查找有 fallback 机制**：优先从消费项目的 `libs.versions.toml` 查找，找不到则使用内置默认坐标
6. **`sky.android.library` 与 `sky.android.library.common` 的区别**：前者包含 Parcelize 插件和 Lint 配置，后者更轻量
7. **Compose 支持为可选**：由 `enableCompose` 属性控制是否应用 `org.jetbrains.kotlin.plugin.compose` 插件
8. **APK 重命名是复制操作**：原始 APK 保留在原位，重命名后的副本输出到 `build/outputs/renamed-apk/`
9. **Release 构建自动打开目录**：Release 变体的 APK 重命名任务完成后，自动在系统文件管理器中打开输出目录
