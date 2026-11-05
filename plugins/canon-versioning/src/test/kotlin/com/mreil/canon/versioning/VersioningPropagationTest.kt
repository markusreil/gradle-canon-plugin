package com.mreil.canon.versioning

import com.mreil.canon.core.CanonPlugin
import com.mreil.canon.core.CanonPropagation
import com.mreil.canon.core.RecordedCopy
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
class VersioningPropagationTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun rootSchemeIsVisibleInChild(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        val child = childOf(root, "child")
        applyAll(root)
        versioningOf(root).scheme.set("custom")
        val pending = mutableListOf<RecordedCopy>()

        CanonPropagation.copyTree(root, child, pending)

        softly.assertThat(versioningOf(child).scheme.get()).isEqualTo("custom")
        softly.assertThat(pending).hasSize(1)
        softly.assertThat(pending.single().propertyName).isEqualTo("scheme")
        softly.assertThat(pending.single().expected).isEqualTo("custom")
    }

    @Test
    fun driftedChildSchemeIsDetectable(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        val child = childOf(root, "child")
        applyAll(root)
        versioningOf(root).scheme.set("custom")
        val pending = mutableListOf<RecordedCopy>()
        CanonPropagation.copyTree(root, child, pending)
        val record = pending.single()

        versioningOf(child).scheme.set("other")

        softly.assertThat(record.read()).isNotEqualTo(record.expected)
        softly.assertThat(record.projectPath).isEqualTo(child.path)
        softly.assertThat(record.extensionName).isEqualTo("versioning")
    }

    @Test
    fun parsedVersionIgnoresChildVersion(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        val child = childOf(root, "child")
        child.version = "9.9.9"
        applyAll(root)

        softly.assertThat(versioningOf(child).parsedVersion().toString()).isEqualTo("1.2.3")
    }

    private fun applyAll(root: Project) {
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)
    }

    private fun versioningOf(project: Project): VersioningExtension {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("versioning") as VersioningExtension
    }

    private fun rootProject(version: String): Project {
        val root = ProjectBuilder.builder().withProjectDir(projectDir).build()
        root.version = version
        return root
    }

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs() || dir.isDirectory) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }
}
