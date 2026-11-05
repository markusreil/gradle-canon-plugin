import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
    alias(libs.plugins.kotlin.jvm)
    jacoco
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        ktfmt().googleStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
        }
    }
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

tasks.register<JacocoReport>("functionalTestReport") {
    dependsOn("functionalTest")
    sourceSets(sourceSets.named("main").get())
    executionData.setFrom(
            tasks.named("functionalTest").map {
                it.extensions.getByType(JacocoTaskExtension::class.java).destinationFile
            })
}

tasks.named("check") {
    dependsOn("functionalTest")
    dependsOn(tasks.withType<JacocoReport>())
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
