package com.example.canon.extra

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/** Sample third-party contributor: registers `extra` under `canon` on all projects. */
class ExtraPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (project === project.rootProject) {
            project.allprojects { register(it) }
        } else {
            register(project)
        }
    }

    private fun register(project: Project) {
        val canon =
            project.extensions.findByName("canon") as? ExtensionAware
                ?: error(
                    "com.example.canon.extra requires 'canon': apply a core bundle " +
                        "before com.example.canon.extra",
                )
        when (val existing = canon.extensions.findByName("extra")) {
            null -> canon.extensions.create("extra", ExtraExtension::class.java)
            is ExtraExtension -> Unit
            else ->
                error(
                    "canon already has an 'extra' contributor — two contributors claim the same name " +
                        "(existing: ${existing.javaClass.name})",
                )
        }
    }
}
