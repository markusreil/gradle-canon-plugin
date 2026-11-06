package com.mreil.canon.codemeta

import com.mreil.canon.codemeta.CodemetaService
import com.mreil.canon.core.CanonPlugin
import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/**
 * Codemeta plugin: holder, service wiring, init gating.
 */
@ExtendWith(SoftAssertionsExtension::class)
class CodemetaPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun registersHolderAndService(softly: SoftAssertions) {
        val root = rootProject()
        writeCodemeta(root, """{"name": "x", "description": "y"}""")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(CodemetaPlugin::class.java)

        softly.assertThat(codemetaOf(root)).isNotNull()
        softly.assertThat(codemetaOf(root).enabled.get()).isTrue()
        softly.assertThat(semverOf(root)).isNotNull()
    }

    @Test
    fun serviceReadsFixtureFile(softly: SoftAssertions) {
        val root = rootProject()
        writeCodemeta(
            root,
            """{"name": "canon", "description": "d", "license": "Apache-2.0"}""",
        )
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(CodemetaPlugin::class.java)

        softly.assertThat(semverOf(root).load().name).isEqualTo("canon")
        softly.assertThat(semverOf(root).load().license).isEqualTo("Apache-2.0")
    }

    @Test
    fun garbageFileFailsServiceRead(softly: SoftAssertions) {
        val root = rootProject()
        writeCodemeta(root, """not json""")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(CodemetaPlugin::class.java)

        softly
            .assertThatThrownBy { semverOf(root).load() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("codemeta.json")
    }

    @Test
    fun initTaskRunsFirst(softly: SoftAssertions) {
        val root = rootProject()
        writeCodemeta(root, """{"name": "x", "description": "y"}""")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(CodemetaPlugin::class.java)
        val init = root.tasks.getByName("initCodemeta")

        val unordered =
            root.tasks
                .filter { it.name != "initCodemeta" }
                .filterNot { init in it.taskDependencies.getDependencies(it) }
                .map { it.name }

        softly.assertThat(unordered).isEmpty()
    }

    private fun semverOf(project: Project): CodemetaService {
        val registration =
            project.gradle.sharedServices.registrations.getByName("canonCodemeta")
        return registration.service.get() as CodemetaService
    }

    private fun codemetaOf(project: Project): CodemetaExtension {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("codemeta") as CodemetaExtension
    }

    private fun writeCodemeta(root: Project, content: String) {
        File(root.projectDir, "codemeta.json").writeText(content)
    }

    private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(projectDir).build()
}
