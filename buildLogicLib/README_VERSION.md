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