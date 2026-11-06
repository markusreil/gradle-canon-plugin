import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `jvm-test-suite`
    alias(libs.plugins.kotlin.jvm)
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
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

kotlin {
    jvmToolchain(17)
}

detekt {
    config.setFrom(rootProject.file("config/detekt.yml"))
}

testing {
    suites {
        register<JvmTestSuite>("linkingTest") {
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
                        val repoUrl = providers.gradleProperty("testRepoUrl").orNull
                        if (repoUrl != null) {
                            dependsOn(
                                ":plugins:canon-api:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-core:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-versioning:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-versioning-api:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-master:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-publish:publishAllPublicationsToTestLocalRepository",
                                ":plugins:canon-thirdparty-sample:publishAllPublicationsToTestLocalRepository",
                            )
                            systemProperty("testRepoUrl", repoUrl)
                        }
                        systemProperty("canonVersion", version.toString())
                    }
                }
            }
        }
    }
}

tasks.register<JacocoReport>("linkingTestReport") {
    dependsOn("linkingTest")
    executionData.setFrom(
        tasks.named("linkingTest").map {
            it.extensions.getByType(JacocoTaskExtension::class.java).destinationFile
        })
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
