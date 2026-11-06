import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    `jvm-test-suite`
    alias(libs.plugins.kotlin.jvm)
    jacoco
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.maven.publish)
}

group = "com.mreil.canon"

kotlin {
    jvmToolchain(17)
}

detekt {
    config.setFrom(rootProject.file("config/detekt.yml"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        providers.gradleProperty("testRepoUrl").orNull?.let { url ->
            maven {
                name = "testLocal"
                setUrl(url)
            }
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
    }
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

spotless {
    kotlin {
        ktfmt().googleStyle().configure {
            it.setBlockIndent(4)
            it.setContinuationIndent(4)
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.withType<JacocoReport>())
}
