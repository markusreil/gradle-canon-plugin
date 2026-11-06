package com.mreil.canon.codemeta

import java.time.Year
import kotlinx.serialization.json.Json
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/** Shared codemeta access, consumable without the codemeta contributor. */
abstract class CodemetaService : BuildService<CodemetaService.Params>, AutoCloseable {

    /** Service parameters: where the `codemeta.json` file lives. */
    interface Params : BuildServiceParameters {

        /** Project directory containing `codemeta.json` (usually the root). */
        val rootDir: DirectoryProperty
    }

    /** True when the file exists, whatever its contents. */
    fun exists(): Boolean = file().get().asFile.isFile

    /** Parses the file, failing fast naming it. */
    fun load(): Codemeta {
        val file = file().get().asFile
        require(file.isFile) { "No codemeta.json at '${file.absolutePath}': run initCodemeta" }
        return try {
            parseCodemeta(file.readText())
        } catch (e: IllegalArgumentException) {
            error("Invalid codemeta.json at '${file.absolutePath}': ${e.message}")
        }
    }

    override fun close() = Unit

    private fun file() = parameters.rootDir.file("codemeta.json")
}

/** Parses codemeta JSON text; unknown fields are ignored so hand-written files survive. */
fun parseCodemeta(text: String): Codemeta = codemetaJson.decodeFromString(text)

/**
 * Canonical output: pretty-printed JSON, nulls omitted. Comments are impossible — neither JSON nor
 * the serializer supports them.
 */
val codemetaJson = Json {
    prettyPrint = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * Minimal default file content, prefilled from the Gradle project model where trivially available.
 * Everything POM (name, description, url, license, developers, scm) and jReleaser (tags,
 * maintainers, links, inception) actually require is present; the rest are placeholders for the
 * user to fill in.
 */
fun defaultCodemeta(name: String, description: String?, version: String?): String =
    codemetaJson.encodeToString(
        Codemeta(
            name = name,
            description = description?.ifBlank { null } ?: "TODO: fill in codemeta.json",
            version = version?.takeUnless { it.isBlank() || it == "unspecified" },
            url = "TODO",
            license = "TODO: SPDX identifier",
            codeRepository = "TODO",
            keywords = emptyList(),
            programmingLanguage = listOf("Kotlin"),
            author = listOf(CodemetaPerson(name = "TODO", email = "TODO")),
            copyrightHolder = listOf(CodemetaPerson(name = "TODO")),
            inceptionYear = Year.now().toString(),
            links = mapOf("homepage" to "TODO", "bugTracker" to "TODO"),
        ),
    )
