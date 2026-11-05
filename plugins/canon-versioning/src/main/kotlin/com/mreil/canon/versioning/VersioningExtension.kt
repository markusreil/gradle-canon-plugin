package com.mreil.canon.versioning

import org.gradle.api.Project
import org.semver4j.Semver
import org.semver4j.SemverException

open class VersioningExtension(private val project: Project) {

    var scheme: String = "semver"

    fun parsedVersion(): Semver = parseVersion(project.rootProject.version.toString(), project.path)
}

internal fun parseVersion(value: String, projectPath: String): Semver {
    if (value.isBlank() || value == "unspecified") {
        throw IllegalStateException(
            "Project '$projectPath' has no version set (got '$value'): set a semver version (scheme 'semver')",
        )
    }
    try {
        return Semver(value)
    } catch (e: SemverException) {
        throw IllegalStateException(
            "Project '$projectPath' has version '$value' which is not valid semver (scheme 'semver')",
            e,
        )
    }
}
