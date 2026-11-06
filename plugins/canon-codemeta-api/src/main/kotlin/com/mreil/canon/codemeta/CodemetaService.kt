package com.mreil.canon.codemeta

import kotlinx.serialization.json.Json
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Shared codemeta access, consumable without the codemeta contributor.
 */
abstract class CodemetaService : BuildService<CodemetaService.Params>, AutoCloseable {

    /**
     * Service parameters: where the `codemeta.json` file lives.
     */
    interface Params : BuildServiceParameters {

        /**
         * Project directory containing `codemeta.json` (usually the root).
         */
        val rootDir: DirectoryProperty
    }

    /**
     * True when the file exists, whatever its contents.
     */
    fun exists(): Boolean = file().get().asFile.isFile

    /**
     * Parses the file, failing fast naming it.
     */
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

/**
 * Parses codemeta JSON text; unknown fields are ignored so hand-written files survive.
 */
fun parseCodemeta(text: String): Codemeta =
    Json { ignoreUnknownKeys = true }.decodeFromString(text)
