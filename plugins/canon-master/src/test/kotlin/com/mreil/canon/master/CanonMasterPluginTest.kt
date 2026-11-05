package com.mreil.canon.master

import com.mreil.canon.core.CanonPropagation
import com.mreil.canon.core.RecordedCopy
import com.mreil.canon.versioning.VersioningExtension
import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(SoftAssertionsExtension::class)
class CanonMasterPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun masterRegistersBundleOnRootAndChild(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")

        root.pluginManager.apply(CanonMasterPlugin::class.java)

        softly.assertThat(root.extensions.findByName("canon")).isNotNull()
        softly.assertThat(child.extensions.findByName("canon")).isNotNull()
        softly.assertThat(versioningOf(root)).isNotNull()
        softly.assertThat(versioningOf(child)).isNotNull()
    }

    @Test
    fun propagationFlowsThroughBundle(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")
        root.pluginManager.apply(CanonMasterPlugin::class.java)
        versioningOf(root).scheme.set("custom")
        val pending = mutableListOf<RecordedCopy>()

        CanonPropagation.copyTree(root, child, pending)

        softly.assertThat(versioningOf(child).scheme.get()).isEqualTo("custom")
        softly.assertThat(pending).hasSize(1)
        softly.assertThat(pending.single().propertyName).isEqualTo("scheme")
    }

    private fun versioningOf(project: Project): VersioningExtension {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("versioning") as VersioningExtension
    }

    private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(projectDir).build()

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs() || dir.isDirectory) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }
}
