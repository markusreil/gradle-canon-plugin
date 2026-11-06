package com.mreil.canon.master

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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Single-id master bundle wiring on a real build. */
@ExtendWith(SoftAssertionsExtension::class)
class CanonMasterFunctionalTest {

    @TempDir lateinit var projectDir: Path

    @Test
    fun masterIdAloneWiresBundleAndPropagation(softly: SoftAssertions) {
        write(
            "settings.gradle.kts",
            """
            rootProject.name = "canon-master-l2"
            include("child")
            """
                .trimIndent(),
        )
        write(
            "build.gradle.kts",
            """
            plugins {
                id("com.mreil.canon")
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
                    require(scheme.get() == "custom") { "expected inherited scheme, got ${'$'}{scheme.get()}" }
                }
            }
            """
                .trimIndent(),
        )

        val result: BuildResult =
            GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath(pluginClasspath())
                .withArguments("help")
                .build()

        softly.assertThat(result.output).contains("BUILD SUCCESSFUL")
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
        files.add(File(System.getProperty("canonVersioningJar")))
        files.add(File(System.getProperty("canonApiJar")))
        files.add(File(System.getProperty("canonSemverJar")))
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
