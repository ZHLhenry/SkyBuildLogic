import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import java.io.FileInputStream
import java.util.Properties

class AndroidMavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maven-publish")

            val localProperties = Properties().apply {
                val localPropertiesFile = rootProject.file("local.properties")
                if (localPropertiesFile.exists()) {
                    load(FileInputStream(localPropertiesFile))
                }
            }

            // 循环打印输出 localProperties 的信息
            localProperties.forEach { (key, value) ->
                println("Property: $key = $value")
            }

            val mavenCentralUserName = localProperties.getProperty("mavenCentral.username")
                ?: error("请在 local.properties 中配置 mavenCentral.username（Maven 仓库用户名）")
            val mavenCentralPassword = localProperties.getProperty("mavenCentral.password")
                ?: error("请在 local.properties 中配置 mavenCentral.password（Maven 仓库密码）")
            val mavenCentralGroupId = localProperties.getProperty("mavenCentral.groupId")
                ?: error("请在 local.properties 中配置 mavenCentral.groupId（Maven groupId）")
            val mavenCentralArtifactId = localProperties.getProperty("mavenCentral.artifactId")
                ?: error("请在 local.properties 中配置 mavenCentral.artifactId（Maven artifactId）")
            val mavenCentralVersion = localProperties.getProperty("mavenCentral.version")
                ?: error("请在 local.properties 中配置 mavenCentral.version（Maven 发布版本）")
            val mavenCentralRepoUrl = localProperties.getProperty("mavenCentral.repoUrl")
                ?: error("请在 local.properties 中配置 mavenCentral.repoUrl（Maven 仓库地址）")

            val android = extensions.getByType(LibraryExtension::class.java)

            val generateSourcesJar = tasks.register("generateSourcesJar", Jar::class.java) {
                archiveClassifier.set("sources")
                from(android.sourceSets["main"].java.directories)
            }

            val generateJavadocJar = tasks.register("generateJavadocJar", Jar::class.java) {
                archiveClassifier.set("javadoc")
                from(android.sourceSets["main"].java.directories)
            }

            afterEvaluate {
                extensions.configure<PublishingExtension> {
                    publications {
                        create("release", MavenPublication::class.java) {
                            groupId = mavenCentralGroupId
                            artifactId = mavenCentralArtifactId
                            version = mavenCentralVersion

                            artifact(tasks.getByName("bundleReleaseAar"))
                            artifact(generateSourcesJar)
                            artifact(generateJavadocJar)

                            pom.withXml {
                                val dependenciesNode = asNode().appendNode("dependencies")
                                configurations["implementation"].allDependencies.forEach { dependency ->
                                    if (dependency.version != "unspecified" && dependency.name != "unspecified") {
                                        val dependencyNode = dependenciesNode.appendNode("dependency")
                                        dependencyNode.appendNode("groupId", dependency.group)
                                        dependencyNode.appendNode("artifactId", dependency.name)
                                        dependencyNode.appendNode("version", dependency.version)
                                    }
                                }
                            }
                        }
                    }

                    repositories {
                        maven {
                            isAllowInsecureProtocol = true
                            url = uri(mavenCentralRepoUrl)
                            credentials {
                                username = mavenCentralUserName
                                password = mavenCentralPassword
                            }
                        }
                    }
                }
            }
        }
    }
}
