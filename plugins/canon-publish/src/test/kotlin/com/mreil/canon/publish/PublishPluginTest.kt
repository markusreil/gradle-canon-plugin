package com.mreil.canon.publish

import com.mreil.canon.core.CanonPlugin
import com.mreil.canon.master.CanonMasterPlugin
import java.io.File
import java.nio.charset.StandardCharsets
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Publish is configuration surface: `enabled` gates service use, internals stay out of holders. */
@ExtendWith(SoftAssertionsExtension::class)
class PublishPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun enabledDefaultsTrue(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonMasterPlugin::class.java)
        root.pluginManager.apply(PublishPlugin::class.java)

        softly.assertThat(publishOf(root).enabled.get()).isTrue()
    }

    @Test
    fun enabledPathConsultsService(softly: SoftAssertions) {
        val root = rootProject("1.2.3-SNAPSHOT")
        root.pluginManager.apply(CanonMasterPlugin::class.java)
        root.pluginManager.apply(PublishPlugin::class.java)

        softly.assertThatCode { (root as ProjectInternal).evaluate() }.doesNotThrowAnyException()
    }

    @Test
    fun disabledSkipsService(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(PublishPlugin::class.java)
        publishOf(root).enabled.set(false)

        softly.assertThatCode { (root as ProjectInternal).evaluate() }.doesNotThrowAnyException()
    }

    @Test
    fun enabledWithoutServiceFailsFast(softly: SoftAssertions) {
        val root = rootProject("1.2.3")
        root.pluginManager.apply(CanonPlugin::class.java)
        root.pluginManager.apply(PublishPlugin::class.java)

        softly
            .assertThatThrownBy { (root as ProjectInternal).evaluate() }
            .hasStackTraceContaining("canonSemver")
    }

    @Test
    fun shippedClassesReferenceServiceOnly(softly: SoftAssertions) {
        val pool =
            readPool("com/mreil/canon/publish/PublishPlugin.class") +
                readPool("com/mreil/canon/publish/PublishExtension.class")

        softly.assertThat(pool).contains("com/mreil/canon/versioning/api/")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/versioning/Versioning")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/core/")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/master/")
    }

    private fun readPool(path: String): String {
        val stream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "$path not on test classpath"
            }
        return stream.use { it.readAllBytes().toString(StandardCharsets.ISO_8859_1) }
    }

    private fun publishOf(project: Project): PublishExtension {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("publish") as PublishExtension
    }

    private fun rootProject(version: String): Project {
        val root = ProjectBuilder.builder().withProjectDir(projectDir).build()
        root.version = version
        return root
    }
}
