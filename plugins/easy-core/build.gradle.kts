import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
            dependencies {
                implementation(platform(libs.junit.bom))
                implementation(libs.junit.jupiter)
                implementation(libs.assertj.core)
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
