package com.mreil.canon.semver

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.semver4j.Semver

/**
 * Shared semver parsing, consumable without the versioning contributor. The versioning plugin sets
 * the raw version string; parsing fails fast on first use.
 */
abstract class SemverService : BuildService<SemverService.Params>, AutoCloseable {

    /**
     * Service parameters: the raw version string to parse.
     */
    interface Params : BuildServiceParameters {

        /**
         * Raw version string (usually the root project's version).
         */
        val version: Property<String>
    }

    /**
     * Parsed version, failing fast on garbage.
     */
    val parsed: Semver by lazy { parseVersion(parameters.version.get(), "<semver-service>") }

    /**
     * True when the parsed version is a snapshot, i.e. pre-release routing.
     */
    fun isSnapshot(): Boolean = parsed.isSnapshot()

    override fun close() = Unit
}

/**
 * True when this parsed version carries a `SNAPSHOT` pre-release identifier.
 */
fun Semver.isSnapshot(): Boolean = "SNAPSHOT" in preRelease

/**
 * Dependency-light version parsing, lifted untouched for the shared service.
 */
fun parseVersion(value: String, projectPath: String): Semver {
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
