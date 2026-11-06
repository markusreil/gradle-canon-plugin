package com.mreil.canon.versioning

import com.mreil.canon.core.CanonPlugin
import com.mreil.canon.versioning.api.SemverService
import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Contributor contract: registration, fail-fast, idempotency, collision, scheme default. */
@ExtendWith(SoftAssertionsExtension::class)
class VersioningPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun registersCanonAndVersioningOnRootAndChild(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        val child = childOf(root, "child")

        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly.assertThat(root.extensions.findByName("canon")).isNotNull()
        softly.assertThat(child.extensions.findByName("canon")).isNotNull()
        softly.assertThat(versioningOf(root)).isNotNull()
        softly.assertThat(versioningOf(child)).isNotNull()
    }

    @Test
    fun contributorOnlyFailsFastAndRegistersNothing(softly: SoftAssertions) {
        val root = rootProject("1.2.3")

        softly
            .assertThatThrownBy { root.pluginManager.apply(VersioningPlugin::class.java) }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("requires 'canon'")
        softly.assertThat(root.extensions.findByName("canon")).isNull()
    }

    @Test
    fun reapplyKeepsExistingInstance(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)
        val first = versioningOf(root)

        root.pluginManager.apply(VersioningPlugin::class.java)

        softly.assertThat(versioningOf(root)).isSameAs(first)
    }

    @Test
    fun differentContributorWithSameNameFails(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        val canon = root.extensions.findByName("canon") as ExtensionAware
        canon.extensions.create("versioning", OtherVersioningExtension::class.java)

        softly
            .assertThatThrownBy { root.pluginManager.apply(VersioningPlugin::class.java) }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("two contributors claim the same name")
    }

    @Test
    fun schemeDefaultsToSemver(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)
        val extension = versioningOf(root) as VersioningExtension

        softly.assertThat(extension.scheme.get()).isEqualTo("semver")
    }

    @Test
    fun serviceParsesRootVersion(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly.assertThat(semverOf(root).parsed.toString()).isEqualTo("1.2.3")
        softly.assertThat(semverOf(root).isSnapshot()).isFalse()
    }

    @Test
    fun serviceReflectsSnapshotVersion(softly: SoftAssertions) {
        val root = rootProject("1.2.3-SNAPSHOT")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly.assertThat(semverOf(root).parsed.toString()).isEqualTo("1.2.3-SNAPSHOT")
        softly.assertThat(semverOf(root).isSnapshot()).isTrue()
    }

    @Test
    fun serviceIgnoresChildVersion(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        val child = childOf(root, "child")
        child.version = "9.9.9"
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly.assertThat(semverOf(root).parsed.toString()).isEqualTo("1.2.3")
    }

    @Test
    fun garbageVersionFailsAtEvaluation(softly: SoftAssertions) {
        val root = rootProject("banana")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly
            .assertThatThrownBy { (root as ProjectInternal).evaluate() }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("banana")
    }

    @Test
    fun missingVersionFailsAtEvaluation(softly: SoftAssertions) {
        val root = rootProject("unspecified")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(VersioningPlugin::class.java)

        softly
            .assertThatThrownBy { (root as ProjectInternal).evaluate() }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("has no version set")
    }

    private fun semverOf(project: Project): SemverService {
        val registration = project.gradle.sharedServices.registrations.getByName("canonSemver")
        return registration.service.get() as SemverService
    }

    private fun versioningOf(project: Project): Any? {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("versioning")
    }

    private fun rootProject(version: String): Project {
        val root = ProjectBuilder.builder().withProjectDir(projectDir).build()
        root.version = version
        return root
    }

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs()) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }

    open class OtherVersioningExtension
}
