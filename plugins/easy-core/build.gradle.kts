import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
}

group = "com.mreil.canon"

gradlePlugin {
    plugins {
        create("canonCore") {
            id = "com.mreil.canon.core"
            implementationClass = "com.mreil.canon.core.CanonPlugin"
        }
    }
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
