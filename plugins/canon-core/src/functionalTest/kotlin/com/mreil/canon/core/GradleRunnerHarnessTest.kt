package com.mreil.canon.core

import java.io.IOException
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
class GradleRunnerHarnessTest {

    @TempDir lateinit var projectDir: Path

    @Test
    fun helpSucceedsOnEmptyBuild(softly: SoftAssertions) {
        write(
            "settings.gradle.kts",
            """
            rootProject.name = "harness"
            """
                .trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            // empty harness build
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

    private fun write(fileName: String, content: String) {
        try {
            Files.writeString(projectDir.resolve(fileName), content, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }
}
