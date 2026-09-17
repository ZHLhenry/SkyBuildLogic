## [v1.2.5] - 2026-09-17
- 新增 R8 混淆内聚能力（Library + App），由两个可选开关控制，默认均关闭：
  - `skyBuild.enableLibraryMinify`：Library 模块 release 启用 R8 混淆（默认 false）
  - `skyBuild.enableAppMinify`：App 模块 release 启用 R8 混淆，并同步开启资源压缩 `isShrinkResources`（默认 false）
- 混淆规则目录约定（`.keep` 后缀）：
  - `src/main/keepRules`：公开 API keep 清单（单一事实源）。约定插件显式汇总传给库自身 R8（否则公开 API 会被按无引用裁剪），同时随 AAR 分发给消费者
  - `src/main/minifyRules`：仅库自身混淆 pass 生效的元数据规则（keepattributes 等），放在 keepRules 之外以避免泄漏进消费者 App 的 R8 配置
  - 模块根目录 `consumer-rules.keep`：旧约定兼容，存在时注册为消费者规则随 AAR 分发
- fail-fast 保护：开启 `enableLibraryMinify=true` 但 `src/main/keepRules/` 下无任何 `.keep` 文件时，构建直接抛异常阻断，避免 classes.jar 被裁剪为空的延迟问题
- App 签名配置调整（`SigningConfigs.kt`）：release `proguardFiles` 由硬编码 `proguard-rules.pro` 改为汇总 `src/main/keepRules/*.keep`；release `isMinifyEnabled` 由 `enableAppMinify` 控制，debug 始终不混淆、不压缩资源
- Library 插件（`sky.android.library` / `sky.android.library.common`）新增 `configureLibraryMinify` 调用，内聚 release 混淆开关与规则通道
- 构建日志新增 `enableLibraryMinify` / `enableAppMinify` 输出

## [v1.2.4] - 2026-09-10
- 依赖版本升级：
  - AGP: 9.3.1 → 9.4.0
  - Gradle: 9.6.1 → 9.7.1
  - Kotlin: 2.4.0 → 2.4.20
  - coreKtx: 1.18.0 → 1.19.0
  - junitVersion: 1.2.1 → 1.3.0
  - espressoCore: 3.6.1 → 3.7.0
  - composeBom: 2026.06.01 → 2026.09.00
  - buildLogic: 1.2.3 → 1.2.4
- 内置默认值修正与升级：
  - Hilt 内置默认版本由 2.60 修正为 2.60.1（与 `libs.versions.toml` 保持一致）
  - Compose BOM 内置默认版本（`DEFAULT_COMPOSE_BOM_VERSION`）由 2026.06.01 升级为 2026.09.00

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