package com.mreil.canon.semver

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Shared semver parsing: versions, snapshot distinction, fail-fast garbage.
 *
 * Service registration itself is covered through the versioning plugin tests, which register and
 * read the real service; ProjectBuilder service registration flakes in this module's worker, so
 * this class stays on the dependency-light functions the service delegates to.
 */
@ExtendWith(SoftAssertionsExtension::class)
class SemverServiceTest {

    @Test
    fun parsesReleaseVersion(softly: SoftAssertions) {
        val parsed = parseVersion("1.2.3", "test")

        softly.assertThat(parsed.toString()).isEqualTo("1.2.3")
        softly.assertThat(parsed.isSnapshot()).isFalse()
    }

    @Test
    fun parsesSnapshotVersion(softly: SoftAssertions) {
        val parsed = parseVersion("1.2.3-SNAPSHOT", "test")

        softly.assertThat(parsed.toString()).isEqualTo("1.2.3-SNAPSHOT")
        softly.assertThat(parsed.isSnapshot()).isTrue()
    }

    @Test
    fun garbageVersionFails(softly: SoftAssertions) {
        softly
            .assertThatThrownBy { parseVersion("banana", "test") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("banana")
    }

    @Test
    fun missingVersionFails(softly: SoftAssertions) {
        softly
            .assertThatThrownBy { parseVersion("unspecified", "test") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("has no version set")
    }
}
