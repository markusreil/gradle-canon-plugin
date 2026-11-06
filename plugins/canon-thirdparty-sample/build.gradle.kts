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
    alias(libs.plugins.detekt)
    alias(libs.plugins.maven.publish)
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
        create("canonExtra") {
            id = "com.example.canon.extra"
            implementationClass = "com.example.canon.extra.ExtraPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

detekt {
    config.setFrom(rootProject.file("config/detekt.yml"))
}

publishing {
    repositories {
        providers.gradleProperty("testRepoUrl").orNull?.let { url ->
            maven {
                name = "testLocal"
                setUrl(url)
            }
        }
    }
}

dependencies {
    implementation(project(":plugins:canon-api"))
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
                implementation(project(":plugins:canon-versioning"))
                implementation(project(":plugins:canon-master"))
            }
        }
        register<JvmTestSuite>("functionalTest") {
            useJUnitJupiter()
            dependencies {
                implementation(gradleTestKit())
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter)
                implementation(libs.assertj.core)
                implementation(libs.semver4j)
            }
            targets {
                all {
                    testTask.configure {
                        dependsOn(tasks.pluginUnderTestMetadata)
                        val coreJar =
                                project(":plugins:canon-core").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        val versioningJar =
                                project(":plugins:canon-versioning").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        val masterJar =
                                project(":plugins:canon-master").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        val apiJar =
                                project(":plugins:canon-api").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        val serviceJar =
                                project(":plugins:canon-versioning-api").tasks.named<Jar>("jar").flatMap {
                                    it.archiveFile
                                }
                        dependsOn(coreJar, versioningJar, masterJar, apiJar, serviceJar)
                        inputs.files(coreJar, versioningJar, masterJar, apiJar, serviceJar)
                        doFirst {
                            systemProperty(
                                    "canonCoreJar", coreJar.get().asFile.absolutePath)
                            systemProperty(
                                    "canonVersioningJar", versioningJar.get().asFile.absolutePath)
                            systemProperty(
                                    "canonMasterJar", masterJar.get().asFile.absolutePath)
                            systemProperty("canonApiJar", apiJar.get().asFile.absolutePath)
                            systemProperty(
                                    "canonSemverJar", serviceJar.get().asFile.absolutePath)
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
