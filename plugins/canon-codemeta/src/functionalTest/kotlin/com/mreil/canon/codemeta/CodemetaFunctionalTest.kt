package com.mreil.canon.codemeta

import java.io.File
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Real-build codemeta: init creates then fails, present file builds green. */
@ExtendWith(SoftAssertionsExtension::class)
class CodemetaFunctionalTest {

    @TempDir lateinit var projectDir: Path

    @Test
    fun initCreatesStubThenFails(softly: SoftAssertions) {
        writeSettings("canon-codemeta-init")
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon.core")
                id("com.mreil.canon.codemeta")
            }
            """
                .trimIndent(),
        )

        softly
            .assertThatThrownBy {
                GradleRunner.create()
                    .withProjectDir(projectDir.toFile())
                    .withPluginClasspath(pluginClasspath())
                    .withArguments("initCodemeta")
                    .build()
            }
            .isInstanceOf(UnexpectedBuildFailure::class.java)
            .hasMessageContaining("Fill in")

        val stub = projectDir.resolve("codemeta.json").toFile()
        softly.assertThat(stub.isFile).isTrue()
        softly.assertThat(stub.readText()).contains("TODO")
    }

    @Test
    fun presentFileBuildsGreen(softly: SoftAssertions) {
        writeSettings("canon-codemeta-present")
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon.core")
                id("com.mreil.canon.codemeta")
            }
            """
                .trimIndent(),
        )
        write(
            "codemeta.json",
            """{"name": "canon", "description": "d", "license": "Apache-2.0"}""",
        )

        val result: BuildResult =
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath(pluginClasspath())
                .withArguments("help")
                .build()

        softly.assertThat(result.output).contains("BUILD SUCCESSFUL")
    }

    private fun writeSettings(name: String) {
        write(
            "settings.gradle.kts",
            """
            rootProject.name = "$name"
            """
                .trimIndent(),
        )
    }

    private fun pluginClasspath(): List<File> {
        val stream =
            requireNotNull(
                javaClass.getResourceAsStream("/plugin-under-test-metadata.properties"),
            ) {
                "plugin-under-test-metadata.properties not on test classpath"
            }
        val metadata = Properties()
        stream.use { metadata.load(it) }
        val files =
            metadata
                .getProperty("implementation-classpath")
                .split(File.pathSeparator)
                .map(::File)
                .toMutableList()
        files.add(File(System.getProperty("canonCoreJar")))
        files.add(File(System.getProperty("canonApiJar")))
        files.add(File(System.getProperty("canonCodemetaApiJar")))
        return files
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
