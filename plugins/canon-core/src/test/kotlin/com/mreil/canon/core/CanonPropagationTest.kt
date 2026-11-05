package com.mreil.canon.core

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import java.io.File
import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

/** Engine matrix: every supported type × semantics, plus skip, mismatch, and tree-walk cases. */
@ExtendWith(SoftAssertionsExtension::class)
class CanonPropagationTest {

    @TempDir lateinit var projectDir: File

    open class ShallowProps(objects: ObjectFactory) {
        @field:Propagate(CopySemantics.SHALLOW)
        val shallow: Property<String> = objects.property(String::class.java)
    }

    open class InferredProps(objects: ObjectFactory) {
        val inferred: Property<String> = objects.property(String::class.java)
    }

    open class FrozenProps(objects: ObjectFactory) {
        @field:Propagate(CopySemantics.IMMUTABLE)
        val frozen: Property<String> = objects.property(String::class.java)
    }

    open class DeepProps(objects: ObjectFactory) {
        @field:Propagate(CopySemantics.DEEP)
        val deep: Property<String> = objects.property(String::class.java)
    }

    open class InferredCollectionProps(objects: ObjectFactory) {
        val inferred: ListProperty<String> = objects.listProperty(String::class.java)
    }

    open class ShallowCollectionProps(objects: ObjectFactory) {
        @field:Propagate(CopySemantics.SHALLOW)
        val shallow: ListProperty<String> = objects.listProperty(String::class.java)
    }

    open class PlainProps {
        @field:Propagate(CopySemantics.SHALLOW) var plain: String = ""
    }

    open class UninferredProps {
        var uninferred: String = ""
    }

    open class HiddenProps {
        private var hidden: String = "root-hidden"

        fun peek(): String = hidden

        fun poke(value: String) {
            hidden = value
        }
    }

    @Test
    fun shallowPropertyCopiesPresentValueAsConvention(softly: SoftAssertions) {
        val (source, dest) = pairOf(::ShallowProps)
        source.shallow.set("root-value")

        CanonPropagation.copyExtension("fixture", source, dest, destProject(), mutableListOf())

        softly.assertThat(dest.shallow.get()).isEqualTo("root-value")
    }

    @Test
    fun shallowPropertyLeavesChildUntouchedWhenAbsent(softly: SoftAssertions) {
        val (source, dest) = pairOf(::ShallowProps)
        dest.shallow.set("child-value")

        CanonPropagation.copyExtension("fixture", source, dest, destProject(), mutableListOf())

        softly.assertThat(dest.shallow.get()).isEqualTo("child-value")
    }

    @Test
    fun inferredPropertyDefaultsToShallow(softly: SoftAssertions) {
        val (source, dest) = pairOf(::InferredProps)
        source.inferred.set("root-value")

        CanonPropagation.copyExtension("fixture", source, dest, destProject(), mutableListOf())

        softly.assertThat(dest.inferred.get()).isEqualTo("root-value")
    }

    @Test
    fun immutablePropertyCopiesAndRecords(softly: SoftAssertions) {
        val (source, dest) = pairOf(::FrozenProps)
        val child = destProject()
        source.frozen.set("frozen-value")
        val pending = mutableListOf<RecordedCopy>()

        CanonPropagation.copyExtension("fixture", source, dest, child, pending)

        softly.assertThat(dest.frozen.get()).isEqualTo("frozen-value")
        softly.assertThat(pending).hasSize(1)
        softly.assertThat(pending.single().expected).isEqualTo("frozen-value")
        softly.assertThat(pending.single().extensionName).isEqualTo("fixture")
        softly.assertThat(pending.single().propertyName).isEqualTo("frozen")
    }

    @Test
    fun deepPropertyFailsAsNotImplemented(softly: SoftAssertions) {
        val (source, dest) = pairOf(::DeepProps)
        source.deep.set("root-value")

        val failure =
            assertThrows<IllegalStateException> {
                CanonPropagation.copyExtension(
                    "fixture",
                    source,
                    dest,
                    destProject(),
                    mutableListOf(),
                )
            }

        softly.assertThat(failure.message).contains("Deep copy")
    }

    @Test
    fun inferredCollectionFailsAsDeep(softly: SoftAssertions) {
        val (source, dest) = pairOf(::InferredCollectionProps)
        source.inferred.add("root-value")

        val failure =
            assertThrows<IllegalStateException> {
                CanonPropagation.copyExtension(
                    "fixture",
                    source,
                    dest,
                    destProject(),
                    mutableListOf(),
                )
            }

        softly.assertThat(failure.message).contains("Deep copy")
    }

    @Test
    fun shallowCollectionFails(softly: SoftAssertions) {
        val (source, dest) = pairOf(::ShallowCollectionProps)
        source.shallow.add("root-value")

        val failure =
            assertThrows<IllegalStateException> {
                CanonPropagation.copyExtension(
                    "fixture",
                    source,
                    dest,
                    destProject(),
                    mutableListOf(),
                )
            }

        softly.assertThat(failure.message).contains("Cannot shallow-copy collection")
    }

    @Test
    fun plainVarCopiesByReference(softly: SoftAssertions) {
        val source = PlainProps()
        val dest = PlainProps()
        source.plain = "root-value"

        CanonPropagation.copyExtension("plain", source, dest, destProject(), mutableListOf())

        softly.assertThat(dest.plain).isEqualTo("root-value")
    }

    @Test
    fun uninferredPlainFieldFailsWithoutRule(softly: SoftAssertions) {
        val source = UninferredProps()
        val dest = UninferredProps()
        source.uninferred = "root-value"

        val failure =
            assertThrows<IllegalStateException> {
                CanonPropagation.copyExtension(
                    "fixture",
                    source,
                    dest,
                    destProject(),
                    mutableListOf(),
                )
            }

        softly.assertThat(failure.message).contains("No propagation rule")
    }

    @Test
    fun privateFieldWithoutGetterIsSkipped(softly: SoftAssertions) {
        val (source, dest) = pairOf { HiddenProps() }
        source.poke("changed-after-construction")

        CanonPropagation.copyExtension("fixture", source, dest, destProject(), mutableListOf())

        softly.assertThat(source.peek()).isEqualTo("changed-after-construction")
        softly.assertThat(dest.peek()).isEqualTo("root-hidden")
    }

    @Test
    fun mismatchedPropertyTypesFail(softly: SoftAssertions) {
        val source = ShallowProps(rootProject().objects)
        val dest =
            object {
                @field:Propagate(CopySemantics.SHALLOW) var shallow: String = ""
            }
        source.shallow.set("root-value")

        val failure =
            assertThrows<IllegalStateException> {
                CanonPropagation.copyExtension(
                    "fixture",
                    source,
                    dest,
                    destProject(),
                    mutableListOf(),
                )
            }

        softly.assertThat(failure.message).contains("source and target types differ")
    }

    @Test
    fun copyTreeCopiesGenuineSubExtensionsOnly(softly: SoftAssertions) {
        val root = rootProject()
        val child = childOf(root, "child")
        root.pluginManager.apply(CanonPlugin::class.java)
        val source = ShallowProps(root.objects)
        val dest = ShallowProps(child.objects)
        subExtensionsOf(root).add("fixture", source)
        subExtensionsOf(child).add("fixture", dest)
        source.shallow.set("root-value")
        val pending = mutableListOf<RecordedCopy>()

        CanonPropagation.copyTree(root, child, pending)

        softly.assertThat(dest.shallow.get()).isEqualTo("root-value")
        softly.assertThat(pending).isEmpty()
    }

    private fun <T : Any> pairOf(factory: (ObjectFactory) -> T): Pair<T, T> {
        val objects = rootProject().objects
        return Pair(factory(objects), factory(objects))
    }

    private fun destProject(): Project = childOf(rootProject(), "dest")

    private fun subExtensionsOf(project: Project): ExtensionContainer =
        (project.extensions.findByName("canon") as ExtensionAware).extensions

    private fun rootProject(): Project = ProjectBuilder.builder().withProjectDir(projectDir).build()

    private fun childOf(root: Project, name: String): Project {
        val dir = File(projectDir, name)
        check(dir.mkdirs() || dir.isDirectory) { "Could not create $dir" }
        return ProjectBuilder.builder().withParent(root).withName(name).withProjectDir(dir).build()
    }
}
