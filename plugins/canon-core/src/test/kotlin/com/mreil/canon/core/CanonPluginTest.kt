package com.mreil.canon.core

import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(SoftAssertionsExtension::class)
class CanonPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun registersCanonOnRootAndChild(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")

        root.pluginManager.apply(CanonPlugin::class.java)

        softly
            .assertThat(root.extensions.findByName("canon"))
            .isInstanceOf(CanonExtension::class.java)
        softly
            .assertThat(child.extensions.findByName("canon"))
            .isInstanceOf(CanonExtension::class.java)
    }

    @Test
    fun reapplyIsHarmless(softly: SoftAssertions) {
        val root = rootProject()
        root.pluginManager.apply(CanonPlugin::class.java)
        val first = root.extensions.findByName("canon")

        root.pluginManager.apply(CanonPlugin::class.java)

        softly.assertThat(root.extensions.findByName("canon")).isSameAs(first)
    }

    @Test
    fun directApplicationRegistersLocallyOnly(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")

        child.pluginManager.apply(CanonPlugin::class.java)

        softly
            .assertThat(child.extensions.findByName("canon"))
            .isInstanceOf(CanonExtension::class.java)
        softly.assertThat(root.extensions.findByName("canon")).isNull()
    }

    private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(projectDir).build()

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs()) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }
}
