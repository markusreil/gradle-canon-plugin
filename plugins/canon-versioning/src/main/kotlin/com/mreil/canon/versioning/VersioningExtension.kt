package com.mreil.canon.versioning

import org.gradle.api.Project
import org.semver4j.Semver

open class VersioningExtension(private val project: Project) {

    var scheme: String = "semver"

    fun parsedVersion(): Semver = parseVersion(project.rootProject.version.toString(), project.path)
}

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
