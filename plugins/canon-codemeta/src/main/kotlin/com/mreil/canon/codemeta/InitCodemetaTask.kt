package com.mreil.canon.codemeta

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * Creates the default `codemeta.json` when absent, then fails so the user fills it in. Auto-wired
 * to run before every other task, so a regular invocation on an existing file is a no-op success.
 */
abstract class InitCodemetaTask : DefaultTask() {

    @get:Internal internal var created: Boolean = false

    init {
        group = "canon"
        description = "Creates the default codemeta.json when absent."
    }

    /** Creates the stub file when absent; no-op otherwise (a regular invocation never fails). */
    @TaskAction
    fun create() {
        val holder =
            (project.extensions.findByName("canon") as ExtensionAware)
                .extensions
                .findByName("codemeta") as CodemetaExtension
        if (!holder.enabled.get()) return
        val file = project.layout.projectDirectory.file("codemeta.json").asFile
        if (file.isFile) return
        file.writeText(
            defaultCodemeta(
                name = project.rootProject.name,
                description = project.rootProject.description,
                version = project.rootProject.version.toString(),
            ),
        )
        created = true
    }
}
