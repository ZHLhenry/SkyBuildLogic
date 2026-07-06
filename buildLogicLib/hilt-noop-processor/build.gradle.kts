import java.util.Properties

plugins {
    `java-library`
    `maven-publish`
}

group = "com.sky.buildLogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// ==================== Maven Publishing ====================
val localProps = Properties().apply {
    val localPropertiesFile = rootProject.file("../local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

version = localProps.getProperty("buildLogic.version")
    ?: error("请在 local.properties 中配置 buildLogic.version")

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        val repoUrl = localProps.getProperty("buildLogic.repoUrl", "")
        if (repoUrl.isNotBlank()) {
            maven {
                isAllowInsecureProtocol = repoUrl.startsWith("http://")
                url = uri(repoUrl)
                credentials {
                    username = localProps.getProperty("buildLogic.username", "")
                    password = localProps.getProperty("buildLogic.password", "")
                }
            }
        }
    }
}
