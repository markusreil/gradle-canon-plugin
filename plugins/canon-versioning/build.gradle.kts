import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.named

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
    }
}
