import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
    alias(libs.plugins.kotlin.jvm)
}

group = "com.mreil.canon"

gradlePlugin {
    plugins {
        create("canonVersioning") {
            id = "com.mreil.canon.versioning"
            implementationClass = "com.mreil.canon.versioning.VersioningPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.semver4j)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter)
                implementation(libs.assertj.core)
                implementation(project(":plugins:canon-core"))
            }
        }
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter()
            dependencies {
                implementation(gradleTestKit())
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter)
                implementation(libs.assertj.core)
            }
            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.pluginUnderTestMetadata)
                        val coreJar =
                                project(":plugins:canon-core").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        dependsOn(coreJar)
                        inputs.files(coreJar)
                        doFirst {
                            systemProperty(
                                    "canonCoreJar", coreJar.get().asFile.absolutePath)
                        }
                    }
                }
            }
        }
    }
}

sourceSets {
    named("functionalTest") {
        runtimeClasspath += files(tasks.pluginUnderTestMetadata)
    }
}

tasks.named("check") {
    dependsOn("functionalTest")
}
