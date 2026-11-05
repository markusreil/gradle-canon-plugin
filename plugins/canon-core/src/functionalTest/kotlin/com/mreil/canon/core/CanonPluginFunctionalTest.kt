package com.mreil.canon.core

import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(SoftAssertionsExtension::class)
class CanonPluginFunctionalTest {

    @TempDir lateinit var projectDir: Path

    @Test
    fun canonBlockResolvesOnRootAndChild(softly: SoftAssertions) {
        write(
            "settings.gradle.kts",
            """
            rootProject.name = "canon-l2"
            include("child")
            """
                .trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon.core")
            }

            canon {
            }
            """
                .trimIndent(),
        )
        write(
            "child/build.gradle.kts",
            """
            canon {
            }
            """
                .trimIndent(),
        )

        val result: BuildResult =
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("help")
                .build()

        softly.assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun write(relativePath: String, content: String) {
        try {
            val file: Path = projectDir.resolve(relativePath)
            Files.createDirectories(file.parent)
            Files.writeString(file, content, StandardCharsets.UTF_8)
        } catch (e: java.io.IOException) {
            throw UncheckedIOException(e)
        }
    }
}
