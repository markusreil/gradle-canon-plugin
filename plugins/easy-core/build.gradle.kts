import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.kotlin.dsl.named

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
    }
}
