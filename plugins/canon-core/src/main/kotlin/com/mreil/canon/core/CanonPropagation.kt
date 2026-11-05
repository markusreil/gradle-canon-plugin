package com.mreil.canon.core

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * Copies `easy`/`canon` sub-extension values from the root project down to every child project.
 *
 * Only genuine contributor sub-extensions are visited: the container itself and Gradle-owned
 * extensions (anything under `org.gradle.*`, e.g. `ext`) are skipped. Within each sub-extension,
 * a field participates when it carries [Propagate], or — without the annotation — when it is
 * public or exposes a public `getX`/`isX` getter. Static and synthetic fields are ignored.
 *
 * Supported field types today:
 *
 * - `Property<T>` — fully supported. [CopySemantics.SHALLOW] sets the child's value as a
 *   [convention][org.gradle.api.provider.Property.convention] of the root value, but only when
 *   the root value is present; an absent root value leaves the child untouched. Fields of this
 *   type default to SHALLOW when the annotation is absent.
 * - `ListProperty`, `SetProperty`, `MapProperty` — not copyable yet. They infer
 *   [CopySemantics.DEEP], which currently fails with a descriptive error, and an explicit
 *   SHALLOW on them fails the same way; mark them DEEP (or handle them manually) and they will
 *   start working once the deep-merge chunk lands.
 * - Plain mutable (`var`) fields of any other type — copied by reference assignment. An explicit
 *   [Propagate] mode is required since no default can be inferred; annotating such a field
 *   [CopySemantics.DEEP] fails until deep copy is implemented.
 * - Anything else (notably `val`/`final` fields holding non-provider values) — rejected with an
 *   error telling the contributor to annotate explicitly or switch to a supported type.
 *
 * [CopySemantics.IMMUTABLE] behaves exactly like SHALLOW at copy time and additionally records
 * the copied value; once all projects are evaluated the recorded value is re-read and any drift
 * fails the build, so children cannot override it. Source and target field types must match,
 * otherwise the copy fails with an error.
 *
 * Timing: the copy runs in `afterEvaluate` on the root project; the immutability check runs in
 * `projectsEvaluated`.
 */
object CanonPropagation {
    /**
     * Hooks root-to-children propagation and the immutability check into the build lifecycle.
     *
     * @param root the root project whose `canon` values are the source of truth.
     */
    fun registerPropagation(root: Project) {
        val pending = mutableListOf<RecordedCopy>()
        root.afterEvaluate {
            root.allprojects { target ->
                if (target !== root) {
                    copyTree(root, target, pending)
                }
            }
        }
        root.gradle.projectsEvaluated {
            for (record in pending) {
                val current = record.read()
                check(current == record.expected) {
                    "Project '${record.projectPath}' must not change " +
                        "'${record.extensionName}.${record.propertyName}' (canon value is immutable): " +
                        "expected '${record.expected}', found '$current'"
                }
            }
        }
    }

    /**
     * Copies every genuine sub-extension value from [root] to [target].
     *
     * @param pending collects immutability records; the caller owns verifying them.
     */
    fun copyTree(root: Project, target: Project, pending: MutableList<RecordedCopy>) {
        val rootCanon = root.extensions.findByName("canon") as? ExtensionAware ?: return
        val targetCanon = target.extensions.findByName("canon") as? ExtensionAware ?: return
        for (element in rootCanon.extensions.extensionsSchema.elements) {
            val source = rootCanon.extensions.findByName(element.name)
            val dest = targetCanon.extensions.findByName(element.name)
            if (
                source != null && dest != null && isGenuineSubExtension(element.name, source, root)
            ) {
                copyExtension(element.name, source, dest, target, pending)
            }
        }
    }

    /**
     * Copies all propagatable fields of one sub-extension from [source] to [dest].
     *
     * @param pending collects immutability records for fields copied as IMMUTABLE.
     */
    fun copyExtension(
        extensionName: String,
        source: Any,
        dest: Any,
        target: Project,
        pending: MutableList<RecordedCopy>,
    ) {
        val scope = CopyScope(extensionName, source, dest, target, pending)
        for (field in scope.source.javaClass.declaredFields) {
            if (!Modifier.isStatic(field.modifiers) && !field.isSynthetic) {
                copyField(field, scope)
            }
        }
    }

    private fun isGenuineSubExtension(name: String, source: Any, root: Project): Boolean {
        // The schema can surface project-scope extensions (e.g. `canon` itself); only genuine
        // sub-extensions — instances the project container does not itself resolve — are copied.
        if (source === root.extensions.findByName(name)) return false
        // Gradle-owned machinery (e.g. the auto-registered `ext`) is never ours, whatever its name.
        return !source.javaClass.name.startsWith("org.gradle.")
    }

    private class CopyScope(
        val extensionName: String,
        val source: Any,
        val dest: Any,
        val target: Project,
        val pending: MutableList<RecordedCopy>,
    )

    private fun copyField(field: Field, scope: CopyScope) {
        val mode = resolveMode(field, scope.source) ?: return
        field.isAccessible = true
        val destField =
            scope.dest.javaClass.getDeclaredField(field.name).also { it.isAccessible = true }
        when (mode) {
            CopySemantics.SHALLOW -> applyShallow(field, destField, scope)
            CopySemantics.IMMUTABLE -> {
                applyShallow(field, destField, scope)
                scope.pending.add(recordCopy(field, scope, destField))
            }
            CopySemantics.DEEP ->
                error(
                    "Deep copy for '${field.name}' is not implemented yet: " +
                        "declare an explicit SHALLOW or IMMUTABLE mode, or wait for the deep-merge chunk",
                )
        }
    }

    private fun resolveMode(field: Field, instance: Any): CopySemantics? =
        field.getAnnotation(Propagate::class.java)?.value
            ?: if (isPropagatable(field, instance)) inferFromType(field) else null

    private fun isPropagatable(field: Field, instance: Any): Boolean {
        if (Modifier.isPublic(field.modifiers)) return true
        val capitalized = field.name.replaceFirstChar(Char::uppercaseChar)
        return instance.javaClass.methods
            .filter { Modifier.isPublic(it.modifiers) && it.parameterCount == 0 }
            .map { it.name }
            .any { it == "get$capitalized" || it == "is$capitalized" }
    }

    private fun inferFromType(field: Field): CopySemantics {
        val type = field.type
        if (Property::class.java.isAssignableFrom(type)) return CopySemantics.SHALLOW
        if (
            ListProperty::class.java.isAssignableFrom(type) ||
                SetProperty::class.java.isAssignableFrom(type) ||
                MapProperty::class.java.isAssignableFrom(type)
        ) {
            return CopySemantics.DEEP
        }
        error(
            "No propagation rule for type '${type.name}' of '${field.name}': " +
                "annotate explicitly with a supported mode",
        )
    }

    private fun applyShallow(field: Field, destField: Field, scope: CopyScope) {
        val extensionName = scope.extensionName
        val sourceValue = field.get(scope.source)
        if (sourceValue is Property<*>) {
            val destValue = destField.get(scope.dest)
            if (destValue !is Property<*>) {
                error(
                    "Cannot propagate '${field.name}' in '$extensionName': source and target types differ",
                )
            }
            if (sourceValue.isPresent) {
                @Suppress("UNCHECKED_CAST")
                (destValue as Property<Any>).convention(sourceValue.get())
            }
            return
        }
        if (
            sourceValue is ListProperty<*> ||
                sourceValue is SetProperty<*> ||
                sourceValue is MapProperty<*, *>
        ) {
            error(
                "Cannot shallow-copy collection '${field.name}' in '$extensionName': " +
                    "mark it DEEP (not implemented yet) or handle it manually",
            )
        }
        if (Modifier.isFinal(destField.modifiers)) {
            error(
                "No propagation rule for type '${field.type.name}' of '${field.name}' in '$extensionName': " +
                    "annotate explicitly with a supported mode or use a supported type",
            )
        }
        destField.set(scope.dest, sourceValue)
    }

    private fun recordCopy(field: Field, scope: CopyScope, destField: Field): RecordedCopy {
        val read: () -> Any? = {
            val value = destField.get(scope.dest)
            if (value is Property<*> && value.isPresent) value.get()
            else if (value is Property<*>) null else value
        }
        return RecordedCopy(scope.target.path, scope.extensionName, field.name, read(), read)
    }
}

/**
 * Snapshot of one IMMUTABLE copy, used to detect child-side drift after evaluation.
 *
 * @property expected the value observed at copy time; compared against [read] later.
 * @property read re-reads the child's current value.
 */
class RecordedCopy(
    val projectPath: String,
    val extensionName: String,
    val propertyName: String,
    val expected: Any?,
    val read: () -> Any?,
)
