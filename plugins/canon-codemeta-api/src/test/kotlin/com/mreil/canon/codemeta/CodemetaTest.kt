package com.mreil.canon.codemeta

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Codemeta parsing: fields land, unknown keys are ignored, garbage fails.
 */
@ExtendWith(SoftAssertionsExtension::class)
class CodemetaTest {

    @Test
    fun parsesNeededFields(softly: SoftAssertions) {
        val parsed =
            parseCodemeta(
                """
                {
                  "@context": "https://doi.org/10.5063/schema/codemeta-2.0",
                  "@type": "SoftwareSourceCode",
                  "name": "canon",
                  "description": "Convention plugins",
                  "author": [{"name": "M", "email": "m@example.com"}],
                  "license": "Apache-2.0",
                  "codeRepository": "https://example.com/repo",
                  "keywords": ["gradle"],
                  "programmingLanguage": ["Kotlin"],
                  "url": "https://example.com"
                }
                """.trimIndent(),
            )

        softly.assertThat(parsed.name).isEqualTo("canon")
        softly.assertThat(parsed.author.single().email).isEqualTo("m@example.com")
        softly.assertThat(parsed.license).isEqualTo("Apache-2.0")
        softly.assertThat(parsed.codeRepository).isEqualTo("https://example.com/repo")
    }

    @Test
    fun ignoresUnknownKeys(softly: SoftAssertions) {
        val parsed =
            parseCodemeta(
                """{"name": "canon", "description": "x", "futureField": 1}""",
            )

        softly.assertThat(parsed.name).isEqualTo("canon")
    }

    @Test
    fun garbageFails(softly: SoftAssertions) {
        softly
            .assertThatThrownBy { parseCodemeta("not json") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
