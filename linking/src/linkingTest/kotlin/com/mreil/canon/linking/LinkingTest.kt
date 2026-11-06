package com.mreil.canon.linking

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Coordinate consumption: master plus sample resolve from the published repo and behave. */
@ExtendWith(SoftAssertionsExtension::class)
class LinkingTest {

    @TempDir lateinit var projectDir: Path

    @Test
    fun publishedBundleCoexistsAndPropagates(softly: SoftAssertions) {
        val version = canonVersion()
        write(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    maven(url = uri(System.getProperty("testRepoUrl")))
                    gradlePluginPortal()
                }
            }
            rootProject.name = "canon-linking"
            include("child")
            """
                .trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon") version "$version"
                id("com.example.canon.extra") version "$version"
            }

            version = "1.2.3"

            canon {
                versioning {
                    scheme.set("custom")
                }
                extra {
                    mode.set("custom")
                }
            }
            """
                .trimIndent(),
        )
        write(
            "child/build.gradle.kts",
            """
            canon {
                versioning {
                    require(scheme.get() == "custom") { "expected inherited scheme" }
                }
                extra {
                    require(mode.get() == "custom") { "expected inherited mode" }
                }
            }
            """
                .trimIndent(),
        )

        val result: BuildResult =
            GradleRunner.create().withProjectDir(projectDir.toFile()).withArguments("help").build()

        softly.assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun publishedOverrideFails(softly: SoftAssertions) {
        val version = canonVersion()
        write(
            "settings.gradle.kts",
            """
            pluginManagement {
                repositories {
                    maven(url = uri(System.getProperty("testRepoUrl")))
                    gradlePluginPortal()
                }
            }
            rootProject.name = "canon-linking-override"
            include("child")
            """
                .trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon") version "$version"
            }

            version = "1.2.3"

            canon {
                versioning {
                    scheme.set("custom")
                }
            }
            """
                .trimIndent(),
        )
        write(
            "child/build.gradle.kts",
            """
            canon {
                versioning {
                    scheme.set("evil")
                }
            }
            """
                .trimIndent(),
        )

        softly
            .assertThatThrownBy {
                GradleRunner.create()
                    .withProjectDir(projectDir.toFile())
                    .withArguments("help")
                    .build()
            }
            .isInstanceOf(UnexpectedBuildFailure::class.java)
            .hasMessageContaining(":child")
            .hasMessageContaining("versioning.scheme")
            .hasMessageContaining("custom")
    }

    private fun canonVersion(): String =
        requireNotNull(System.getProperty("canonVersion")) {
            "linkingTest requires the canonVersion system property"
        }

    private fun write(relativePath: String, content: String) {
        try {
            val file: Path = projectDir.resolve(relativePath)
            Files.createDirectories(file.parent)
            Files.writeString(file, content, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
