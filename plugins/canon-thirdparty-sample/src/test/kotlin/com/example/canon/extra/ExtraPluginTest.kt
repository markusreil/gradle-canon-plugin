package com.example.canon.extra

import com.mreil.canon.core.CanonPropagation
import com.mreil.canon.core.RecordedCopy
import com.mreil.canon.master.CanonMasterPlugin
import java.io.File
import java.nio.charset.StandardCharsets
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Third-party interop: coexistence, foreign propagation, clash, api-only shape. */
@ExtendWith(SoftAssertionsExtension::class)
class ExtraPluginTest {

    @TempDir lateinit var projectDir: File

    @Test
    fun masterAndSampleCoexistOnRootAndChild(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")

        root.pluginManager.apply(CanonMasterPlugin::class.java)
        root.pluginManager.apply(ExtraPlugin::class.java)

        softly.assertThat(versioningOf(root)).isNotNull()
        softly.assertThat(versioningOf(child)).isNotNull()
        softly.assertThat(extraOf(root)).isNotNull()
        softly.assertThat(extraOf(child)).isNotNull()
    }

    @Test
    fun sampleWithoutCanonFailsFast(softly: SoftAssertions) {
        val root = rootProject()

        softly
            .assertThatThrownBy { root.pluginManager.apply(ExtraPlugin::class.java) }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("requires 'canon'")
        softly.assertThat(root.extensions.findByName("canon")).isNull()
    }

    @Test
    fun foreignModePropagatesToChild(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")
        root.pluginManager.apply(CanonMasterPlugin::class.java)
        root.pluginManager.apply(ExtraPlugin::class.java)
        extraOf(root).mode.set("custom")
        val pending = mutableListOf<RecordedCopy>()

        CanonPropagation.copyTree(root, child, pending)

        softly.assertThat(extraOf(child).mode.get()).isEqualTo("custom")
        softly.assertThat(pending.map { it.propertyName }).containsExactly("scheme")
    }

    @Test
    fun sameNameTwinFailsWithContractMessage(softly: SoftAssertions) {
        val root = rootProject()
        root.pluginManager.apply(CanonMasterPlugin::class.java)
        val canon = root.extensions.findByName("canon") as ExtensionAware
        canon.extensions.create("extra", TwinExtraExtension::class.java, root.objects)

        softly
            .assertThatThrownBy { root.pluginManager.apply(ExtraPlugin::class.java) }
            .isInstanceOf(RuntimeException::class.java)
            .hasStackTraceContaining("two contributors claim the same name")
    }

    @Test
    fun shippedClassesReferenceApiOnly(softly: SoftAssertions) {
        val pool =
            readPool("com/example/canon/extra/ExtraPlugin.class") +
                readPool("com/example/canon/extra/ExtraExtension.class")

        softly.assertThat(pool).contains("com/mreil/canon/api/")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/core/")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/versioning/")
        softly.assertThat(pool).doesNotContain("com/mreil/canon/master/")
    }

    private fun readPool(path: String): String {
        val stream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(path)) {
                "$path not on test classpath"
            }
        return stream.use { it.readAllBytes().toString(StandardCharsets.ISO_8859_1) }
    }

    private fun versioningOf(project: Project): Any? {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("versioning")
    }

    private fun extraOf(project: Project): ExtraExtension {
        val canon = project.extensions.findByName("canon") as ExtensionAware
        return canon.extensions.findByName("extra") as ExtraExtension
    }

    private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(projectDir).build()

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs() || dir.isDirectory) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }

    open class TwinExtraExtension(objects: Any)
}
