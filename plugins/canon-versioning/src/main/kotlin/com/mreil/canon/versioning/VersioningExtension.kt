package com.mreil.canon.versioning

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.semver4j.Semver

open class VersioningExtension @Inject constructor(objects: ObjectFactory) {

    @Propagate(CopySemantics.IMMUTABLE)
    val scheme: Property<String> =
        objects.property(String::class.java).convention("semver")

    internal lateinit var project: Project

    fun parsedVersion(): Semver = parseVersion(project.rootProject.version.toString(), project.path)

    companion object {
        internal fun parseVersion(value: String, projectPath: String): Semver {
            if (value.isBlank() || value == "unspecified") {
                error(
                    "Project '$projectPath' has no version set (got '$value'): set a semver version (scheme 'semver')",
                )
            }
            if (!Semver.isValid(value)) {
                error(
                    "Project '$projectPath' has version '$value' which is not valid semver (scheme 'semver')",
                )
            }
            return Semver(value)
        }
    }
}
