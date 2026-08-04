## [v1.2.3] - 2026-08-04
- 新增 Compose 依赖自动内聚能力：
  - 新增 `ComposeDependencies.kt`，`enableCompose=true` 时自动注入 Compose BOM + Runtime + UI 套件，无需各模块手写依赖
  - Application 使用 `CORE_UI` 集合，Library 按需使用 `CORE_UI` 或 `RUNTIME_ONLY` 集合
  - BOM 版本由 `skyBuild.composeBomVersion` 控制，默认 `2026.06.01`，支持消费方覆盖
- 依赖版本升级：
  - `lifecycleRuntimeKtx`: 2.10.0 → 2.11.0
  - `activityCompose`: 1.9.3 → 1.13.0
  - `composeBom`: 2026.02.01 → 2026.06.01
  - `buildLogic`: 1.2.1 → 1.2.3
- 升级 `compileSdk` 至 37（`build.gradle.kts`），满足 `lifecycle-runtime-ktx:2.11.0` 等依赖的 `minCompileSdk ≥ 37` 要求（AAR 元数据检查）
- 配置变更：
  - `gradle.properties` 中 `android.disallowKotlinSourceSets=false` 改为 `true`
- 文档与注释准确性修正：
  - `README_DOC.md` 及多处源文件 KDoc 示例的 `compileSdk` 示例值由 36 更正为 37
  - Hilt 内置默认版本说明由 2.60 更正为 2.60.1（与 `libs.versions.toml` 一致）
  - 澄清 `skyBuildLogic.version` 无默认值，缺失会直接报错
  - 补充 `library.common` 实际应用的 `com.google.devtools.ksp` 插件（来自 configureHilt）
  - 修正 `PrintAssembleApks` 注释：APK 输出目录为 `outputs/renamed-apk/`（原注释误写 `outputs/apk/`）
  - 修正 compileSdk 说明：本库不注入 `lifecycle-runtime-compose`，该依赖由消费方自行声明或传递引入，与 `enableCompose` 开关无关
  - 修正 Compose BOM 默认版本描述为 `2026.06.01`（与代码一致）

## [v1.2.1] - 2026-07-26
- 新增 `useLocalBuildLogic` 属性支持，可在 `local.properties` 中切换本地 includeBuild 与远程 Maven 依赖
- 修复 `settings.gradle.kts` 中 `providers.gradleProperty()` 无法读取 `local.properties` 的问题，改为显式加载
- 依赖版本升级：
  - AGP: 9.2.1 → 9.3.1
  - Kotlin: 2.2.10 → 2.4.0
  - Hilt: 2.60 → 2.60.1
  - coreKtx: 1.16.0 → 1.18.0

## [v1.2.0] - 2026-07-09
- 签名配置升级：支持 debug/release 分离签名配置（app.debug.* / app.release.*），release 构建使用独立 release 签名
- 修复 Release 构建使用 debug 签名的 Bug
- 修复 Maven 发布插件 isAllowInsecureProtocol 硬编码为 true 的安全问题，改为根据 URL 协议动态判断
- 修复 Lint 报告路径使用已弃用 buildDir API 的问题，改用 layout.buildDirectory
- 修复 PrintApkLocationTask 缺少 PathSensitive 注解的 Gradle 配置缓存兼容性问题
- 移除 MavenPublish 插件中 localProperties 的调试打印输出
- 重构 ProjectExtensions，抽取公共 populateFromExtra 方法消除重复代码

## [v1.1.1] - 2026-07-08
- 内置7个插件优化，优化各插件的构建流程
- 增加文档说明，规范插件使用流程

## [v1.0.0] - 2026-07-06
- skyBuildLogic包重磅首发