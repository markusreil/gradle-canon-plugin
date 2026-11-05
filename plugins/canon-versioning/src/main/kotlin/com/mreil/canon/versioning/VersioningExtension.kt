package com.mreil.canon.versioning

import com.mreil.canon.api.CopySemantics
import com.mreil.canon.api.Propagate
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.semver4j.Semver

/** The `versioning` contributor holder: versioning scheme plus version parsing. */
open class VersioningExtension @Inject constructor(objects: ObjectFactory) {

    /** Active versioning scheme, propagated root → children as immutable. */
    @Propagate(CopySemantics.IMMUTABLE)
    val scheme: Property<String> = objects.property(String::class.java).convention("semver")

    internal lateinit var project: Project

    /** Parses the root project's version; never reflects a child-local version. */
    fun parsedVersion(): Semver = parseVersion(project.rootProject.version.toString(), project.path)

    /**
     * Dependency-light version parsing, kept here so Chunk 7 can lift it into a build service
     * untouched.
     */
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
